package com.jyeeeh.sipspot.service;

import com.jyeeeh.sipspot.domain.Account;
import com.jyeeeh.sipspot.domain.AccountSession;
import com.jyeeeh.sipspot.domain.Room;
import com.jyeeeh.sipspot.dto.auth.LoginResponse;
import com.jyeeeh.sipspot.dto.auth.SignupResponse;
import com.jyeeeh.sipspot.repository.AccountRepository;
import com.jyeeeh.sipspot.repository.AccountSessionRepository;
import com.jyeeeh.sipspot.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    private AccountRepository accountRepo;
    private AccountSessionRepository sessionRepo;
    private RoomRepository roomRepo;
    private TokenService tokenService;
    private PresenceRegistry presenceRegistry;
    private AccountService accountService;

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        accountRepo = mock(AccountRepository.class);
        sessionRepo = mock(AccountSessionRepository.class);
        roomRepo = mock(RoomRepository.class);
        tokenService = mock(TokenService.class);
        presenceRegistry = mock(PresenceRegistry.class);
        accountService = new AccountService(accountRepo, sessionRepo, roomRepo, tokenService, presenceRegistry);

        when(tokenService.generateToken()).thenReturn("raw-token");
        when(tokenService.hash("raw-token")).thenReturn("hashed-token");
    }

    // ── 회원가입 ───────────────────────────────────────────────────────────────

    @Test
    void 회원가입_성공() {
        when(accountRepo.existsByUsernameLower("user1")).thenReturn(false);
        when(accountRepo.existsByNicknameLower("nick1")).thenReturn(false);

        SignupResponse response = accountService.signup("user1", "password123", "nick1");

        assertThat(response.token()).isEqualTo("raw-token");
        assertThat(response.nickname()).isEqualTo("nick1");
        verify(sessionRepo).save(any(AccountSession.class));
    }

    @Test
    void 아이디_중복이면_DuplicateUsernameException() {
        when(accountRepo.existsByUsernameLower("user1")).thenReturn(true);

        assertThatThrownBy(() -> accountService.signup("user1", "password123", "nick1"))
                .isInstanceOf(AccountService.DuplicateUsernameException.class);
        verify(accountRepo, never()).saveAndFlush(any());
    }

    @Test
    void 닉네임_중복이면_DuplicateNicknameException() {
        when(accountRepo.existsByUsernameLower("user1")).thenReturn(false);
        when(accountRepo.existsByNicknameLower("nick1")).thenReturn(true);

        assertThatThrownBy(() -> accountService.signup("user1", "password123", "nick1"))
                .isInstanceOf(AccountService.DuplicateNicknameException.class);
        verify(accountRepo, never()).saveAndFlush(any());
    }

    // ── 로그인 ────────────────────────────────────────────────────────────────

    @Test
    void 로그인_성공_방_없음() {
        String rawPassword = "password123";
        Account account = mockAccount(1L, "user1", "nick1", encoder.encode(rawPassword));

        when(accountRepo.findByUsernameLower("user1")).thenReturn(Optional.of(account));
        when(roomRepo.findByHostAccountId(1L)).thenReturn(Optional.empty());

        LoginResponse response = accountService.login("user1", rawPassword);

        assertThat(response.token()).isEqualTo("raw-token");
        assertThat(response.nickname()).isEqualTo("nick1");
        assertThat(response.room()).isNull();
        verify(sessionRepo).save(any(AccountSession.class));
    }

    @Test
    void 로그인_성공_방_있음() {
        String rawPassword = "password123";
        Account account = mockAccount(1L, "user1", "nick1", encoder.encode(rawPassword));

        Room room = mock(Room.class);
        when(room.getCode()).thenReturn("ABC1234");
        when(room.getLocation()).thenReturn(com.jyeeeh.sipspot.domain.Location.LIVING_ROOM);

        when(accountRepo.findByUsernameLower("user1")).thenReturn(Optional.of(account));
        when(roomRepo.findByHostAccountId(1L)).thenReturn(Optional.of(room));
        when(presenceRegistry.isOnline(1L)).thenReturn(false);

        LoginResponse response = accountService.login("user1", rawPassword);

        assertThat(response.room()).isNotNull();
        assertThat(response.room().code()).isEqualTo("ABC1234");
    }

    @Test
    void 존재하지_않는_아이디이면_AuthException() {
        when(accountRepo.findByUsernameLower("nouser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.login("nouser", "password123"))
                .isInstanceOf(AccountService.AuthException.class);
    }

    @Test
    void 비밀번호_틀리면_AuthException() {
        Account account = mockAccount(1L, "user1", "nick1", encoder.encode("correct-password"));
        when(accountRepo.findByUsernameLower("user1")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.login("user1", "wrong-password"))
                .isInstanceOf(AccountService.AuthException.class);
        verify(sessionRepo, never()).save(any());
    }

    // ── 로그아웃 ──────────────────────────────────────────────────────────────

    @Test
    void 로그아웃_시_세션_삭제() {
        accountService.logout("hashed-token");
        verify(sessionRepo).deleteByTokenHash("hashed-token");
    }

    // ── 내 정보 ───────────────────────────────────────────────────────────────

    @Test
    void 내_정보_조회_성공() {
        Account account = mockAccount(1L, "user1", "nick1", "hash");
        when(accountRepo.findById(1L)).thenReturn(Optional.of(account));
        when(roomRepo.findByHostAccountId(1L)).thenReturn(Optional.empty());

        var response = accountService.me(1L);

        assertThat(response.accountId()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("user1");
        assertThat(response.nickname()).isEqualTo("nick1");
        assertThat(response.room()).isNull();
    }

    @Test
    void 존재하지_않는_accountId이면_UnauthorizedException() {
        when(accountRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.me(99L))
                .isInstanceOf(AccountService.UnauthorizedException.class);
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Account mockAccount(Long id, String username, String nickname, String passwordHash) {
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(id);
        when(account.getUsername()).thenReturn(username);
        when(account.getNickname()).thenReturn(nickname);
        when(account.getPasswordHash()).thenReturn(passwordHash);
        return account;
    }
}
