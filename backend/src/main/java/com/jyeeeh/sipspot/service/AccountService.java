package com.jyeeeh.sipspot.service;

import com.jyeeeh.sipspot.domain.Account;
import com.jyeeeh.sipspot.domain.AccountSession;
import com.jyeeeh.sipspot.domain.Room;
import com.jyeeeh.sipspot.dto.auth.*;
import com.jyeeeh.sipspot.repository.AccountRepository;
import com.jyeeeh.sipspot.repository.AccountSessionRepository;
import com.jyeeeh.sipspot.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepo;
    private final AccountSessionRepository sessionRepo;
    private final RoomRepository roomRepo;
    private final TokenService tokenService;
    private final PresenceRegistry presenceRegistry;
    private final BCryptPasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accountRepo,
                          AccountSessionRepository sessionRepo,
                          RoomRepository roomRepo,
                          TokenService tokenService,
                          PresenceRegistry presenceRegistry) {
        this.accountRepo = accountRepo;
        this.sessionRepo = sessionRepo;
        this.roomRepo = roomRepo;
        this.tokenService = tokenService;
        this.presenceRegistry = presenceRegistry;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public SignupResponse signup(String username, String rawPassword, String nickname) {
        if (accountRepo.existsByUsernameLower(username)) {
            throw new DuplicateUsernameException();
        }
        if (accountRepo.existsByNicknameLower(nickname)) {
            throw new DuplicateNicknameException();
        }
        // 평문 비밀번호는 어떤 로그에도 남기지 않음
        String passwordHash = passwordEncoder.encode(rawPassword);
        Account account = new Account(username.trim(), passwordHash, nickname.trim());
        try {
            accountRepo.saveAndFlush(account);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청에 의한 race condition — username/nickname 중복으로 처리
            throw new DuplicateUsernameException();
        }

        String token = tokenService.generateToken();
        sessionRepo.save(new AccountSession(account, tokenService.hash(token)));

        return new SignupResponse(account.getId(), account.getNickname(), token);
    }

    @Transactional
    public LoginResponse login(String username, String rawPassword) {
        Account account = accountRepo.findByUsernameLower(username)
                .orElseThrow(AuthException::new);

        // BCrypt 검증. 실패 사유를 구분하지 않음 (계정 존재 여부 추측 방지)
        if (!passwordEncoder.matches(rawPassword, account.getPasswordHash())) {
            throw new AuthException();
        }

        String token = tokenService.generateToken();
        sessionRepo.save(new AccountSession(account, tokenService.hash(token)));

        Optional<Room> room = roomRepo.findByHostAccountId(account.getId());
        RoomSummary roomSummary = room.map(r -> new RoomSummary(
                r.getCode(),
                r.getLocation().name(),
                presenceRegistry.isOnline(account.getId())
        )).orElse(null);

        return new LoginResponse(account.getId(), account.getNickname(), token, roomSummary);
    }

    @Transactional
    public void logout(String tokenHash) {
        sessionRepo.deleteByTokenHash(tokenHash);
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long accountId) {
        Account account = accountRepo.findById(accountId)
                .orElseThrow(UnauthorizedException::new);

        Optional<Room> room = roomRepo.findByHostAccountId(account.getId());
        RoomSummary roomSummary = room.map(r -> new RoomSummary(
                r.getCode(),
                r.getLocation().name(),
                presenceRegistry.isOnline(account.getId())
        )).orElse(null);

        return new MeResponse(account.getId(), account.getUsername(), account.getNickname(), roomSummary);
    }

    // ── 예외 클래스 ────────────────────────────────────────────────────────────

    /** 로그인 실패 — username 없음/비번 틀림 모두 동일 (계정 존재 여부 노출 방지) */
    public static class AuthException extends RuntimeException {}

    public static class UnauthorizedException extends RuntimeException {}

    public static class DuplicateUsernameException extends RuntimeException {}

    public static class DuplicateNicknameException extends RuntimeException {}
}
