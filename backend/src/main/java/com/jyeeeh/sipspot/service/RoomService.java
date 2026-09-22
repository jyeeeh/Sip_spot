package com.jyeeeh.sipspot.service;

import com.jyeeeh.sipspot.domain.Account;
import com.jyeeeh.sipspot.domain.Room;
import com.jyeeeh.sipspot.dto.RoomPublicResponse;
import com.jyeeeh.sipspot.repository.AccountRepository;
import com.jyeeeh.sipspot.repository.RoomRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private static final int MAX_CODE_RETRIES = 5;

    private final RoomRepository roomRepo;
    private final AccountRepository accountRepo;
    private final RoomCodeGenerator codeGenerator;
    private final PresenceRegistry presenceRegistry;

    public RoomService(RoomRepository roomRepo, AccountRepository accountRepo,
                       RoomCodeGenerator codeGenerator, PresenceRegistry presenceRegistry) {
        this.roomRepo = roomRepo;
        this.accountRepo = accountRepo;
        this.codeGenerator = codeGenerator;
        this.presenceRegistry = presenceRegistry;
    }

    // 방 코드 재시도는 트랜잭션 밖 루프에서 새 트랜잭션으로 시도
    public CreateRoomResult createRoom(Long accountId) {
        Account account = accountRepo.findById(accountId)
                .orElseThrow(AccountService.UnauthorizedException::new);

        // 이미 방이 있으면 거부
        if (roomRepo.findByHostAccountId(accountId).isPresent()) {
            throw new AlreadyHasRoomException();
        }

        for (int attempt = 0; attempt < MAX_CODE_RETRIES; attempt++) {
            String code = codeGenerator.generate();
            try {
                return doCreateRoom(code, account);
            } catch (DataIntegrityViolationException e) {
                if (isCodeConflict(e)) continue;
                // host_account_id 중복이면 AlreadyHasRoomException
                if (isHostConflict(e)) throw new AlreadyHasRoomException();
                throw e;
            }
        }
        throw new IllegalStateException("방 코드 생성 실패: 재시도 초과");
    }

    @Transactional
    protected CreateRoomResult doCreateRoom(String code, Account account) {
        Room room = new Room(code, account);
        roomRepo.save(room);
        return new CreateRoomResult(room.getCode());
    }

    @Transactional(readOnly = true)
    public RoomPublicResponse getRoom(String code) {
        String normalizedCode = code.toUpperCase();
        Room room = roomRepo.findByCodeWithHost(normalizedCode)
                .orElseThrow(() -> new RoomNotFoundException(normalizedCode));

        return new RoomPublicResponse(
                room.getCode(),
                room.getHost().getNickname(),
                room.getLocation().name(),
                presenceRegistry.isOnline(room.getHost().getId()),
                presenceRegistry.getViewerCount(room.getCode())
        );
    }

    // ── 응답 레코드 ────────────────────────────────────────────────────────────

    public record CreateRoomResult(String code) {}

    // ── 예외 클래스 ────────────────────────────────────────────────────────────

    public static class RoomNotFoundException extends RuntimeException {
        public RoomNotFoundException(String code) { super(code); }
    }

    public static class AlreadyHasRoomException extends RuntimeException {}

    // ── 내부 유틸 ──────────────────────────────────────────────────────────────

    private boolean isCodeConflict(DataIntegrityViolationException e) {
        String msg = e.getMostSpecificCause().getMessage();
        return msg != null && msg.contains("room_code_unique");
    }

    private boolean isHostConflict(DataIntegrityViolationException e) {
        String msg = e.getMostSpecificCause().getMessage();
        return msg != null && msg.contains("room_host_account_unique");
    }
}
