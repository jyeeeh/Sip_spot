package com.jyeeeh.sipspot.service;

import com.jyeeeh.sipspot.domain.Member;
import com.jyeeeh.sipspot.domain.Room;
import com.jyeeeh.sipspot.dto.CreateRoomResponse;
import com.jyeeeh.sipspot.dto.JoinRoomResponse;
import com.jyeeeh.sipspot.dto.RoomDetailResponse;
import com.jyeeeh.sipspot.repository.MemberRepository;
import com.jyeeeh.sipspot.repository.RoomRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    private static final int MAX_CODE_RETRIES = 5;

    private final RoomRepository roomRepo;
    private final MemberRepository memberRepo;
    private final RoomCodeGenerator codeGenerator;
    private final TokenService tokenService;

    public RoomService(RoomRepository roomRepo, MemberRepository memberRepo,
                       RoomCodeGenerator codeGenerator, TokenService tokenService) {
        this.roomRepo = roomRepo;
        this.memberRepo = memberRepo;
        this.codeGenerator = codeGenerator;
        this.tokenService = tokenService;
    }

    // 방 코드 재시도는 트랜잭션 밖 루프에서 새 트랜잭션으로 시도
    public CreateRoomResponse createRoom(String nickname) {
        for (int attempt = 0; attempt < MAX_CODE_RETRIES; attempt++) {
            String code = codeGenerator.generate();
            try {
                return doCreateRoom(code, nickname.trim());
            } catch (DataIntegrityViolationException e) {
                if (isCodeConflict(e)) continue;
                throw e;
            }
        }
        throw new IllegalStateException("방 코드 생성 실패: 재시도 초과");
    }

    @Transactional
    protected CreateRoomResponse doCreateRoom(String code, String nickname) {
        Room room = new Room(code);
        roomRepo.save(room);

        String token = tokenService.generateToken();
        Member member = new Member(room, nickname, tokenService.hash(token), true);
        try {
            memberRepo.save(member);
        } catch (DataIntegrityViolationException e) {
            throw e; // 닉네임/코드 충돌 — 상위로 전달
        }
        return new CreateRoomResponse(room.getCode(), member.getId(), token);
    }

    @Transactional
    public JoinRoomResponse joinRoom(String code, String nickname) {
        String normalizedCode = code.toUpperCase();
        String trimmedNickname = nickname.trim();

        Room room = roomRepo.findByCodeForUpdate(normalizedCode)
                .orElseThrow(() -> new RoomNotFoundException(normalizedCode));

        int currentCount = memberRepo.countByRoomId(room.getId());
        if (currentCount >= room.getMaxMembers()) {
            throw new RoomFullException();
        }

        String token = tokenService.generateToken();
        Member member = new Member(room, trimmedNickname, tokenService.hash(token), false);
        try {
            memberRepo.save(member);
            memberRepo.flush();
        } catch (DataIntegrityViolationException e) {
            throw new NicknameConflictException();
        }
        return new JoinRoomResponse(member.getId(), token);
    }

    // 토큰 검증 후 방 조회
    @Transactional(readOnly = true)
    public RoomDetailResponse getRoom(String code, String token) {
        String tokenHash = tokenService.hash(token);
        Member caller = memberRepo.findByTokenHash(tokenHash)
                .orElseThrow(UnauthorizedException::new);

        String normalizedCode = code.toUpperCase();
        if (!caller.getRoom().getCode().equals(normalizedCode)) {
            throw new UnauthorizedException();
        }

        Room room = roomRepo.findByCode(normalizedCode)
                .orElseThrow(() -> new RoomNotFoundException(normalizedCode));

        List<Member> members = room.getMembers();
        return RoomDetailResponse.from(room, members);
    }

    // ── 예외 클래스 ────────────────────────────────────────────────────────────

    public static class RoomNotFoundException extends RuntimeException {
        public RoomNotFoundException(String code) { super(code); }
    }

    public static class RoomFullException extends RuntimeException {}

    public static class NicknameConflictException extends RuntimeException {}

    public static class UnauthorizedException extends RuntimeException {}

    // ── 내부 유틸 ──────────────────────────────────────────────────────────────

    private boolean isCodeConflict(DataIntegrityViolationException e) {
        String msg = e.getMostSpecificCause().getMessage();
        return msg != null && msg.contains("room_code_unique");
    }
}
