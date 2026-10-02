package com.jyeeeh.sipspot.service;

import com.jyeeeh.sipspot.domain.Coffee;
import com.jyeeeh.sipspot.domain.Room;
import com.jyeeeh.sipspot.dto.CoffeeResponse;
import com.jyeeeh.sipspot.dto.ws.CoffeeDeletedEvent;
import com.jyeeeh.sipspot.dto.ws.CoffeeSentEvent;
import com.jyeeeh.sipspot.exception.ForbiddenException;
import com.jyeeeh.sipspot.repository.CoffeeRepository;
import com.jyeeeh.sipspot.repository.RoomRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CoffeeService {

    private final CoffeeRepository coffeeRepo;
    private final RoomRepository roomRepo;
    private final SimpMessagingTemplate messagingTemplate;
    // 체크리스트 1: 비밀번호 BCrypt 해시 저장
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public CoffeeService(CoffeeRepository coffeeRepo, RoomRepository roomRepo,
                         SimpMessagingTemplate messagingTemplate) {
        this.coffeeRepo = coffeeRepo;
        this.roomRepo = roomRepo;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public List<CoffeeResponse> getCoffees(String roomCode) {
        return coffeeRepo.findByRoomCodeOrderByCreatedAtDesc(roomCode.toUpperCase())
                .stream()
                .map(CoffeeResponse::from)
                .toList();
    }

    @Transactional
    public CoffeeResponse sendCoffee(String roomCode, String name, String message, String rawPassword) {
        // 체크리스트 3: 서비스 레벨 2차 검증 (단위 테스트 가능하도록 서비스에도 검증)
        String trimmedName = (name != null) ? name.strip() : "";
        if (trimmedName.isEmpty() || trimmedName.length() > 10) {
            throw new CoffeeValidationException("이름은 1~10자여야 합니다.");
        }
        if (rawPassword == null || rawPassword.length() < 4) {
            throw new CoffeeValidationException("비밀번호는 4자 이상이어야 합니다.");
        }

        String code = roomCode.toUpperCase();
        Room room = roomRepo.findByCodeWithHost(code)
                .orElseThrow(() -> new RoomService.RoomNotFoundException(code));

        String trimmedMessage = (message != null) ? message.strip() : null;
        if (trimmedMessage != null && trimmedMessage.isEmpty()) trimmedMessage = null;

        // 체크리스트 1: rawPassword는 여기서 해시로 변환 후 버림 — 체크리스트 2: 절대 로그에 남기지 않음
        String hash = passwordEncoder.encode(rawPassword);
        Coffee coffee = new Coffee(room, trimmedName, trimmedMessage, hash);
        coffeeRepo.save(coffee);

        CoffeeResponse response = CoffeeResponse.from(coffee);
        // 체크리스트 7: CoffeeResponse에 passwordHash 없음 → WS 브로드캐스트에도 비밀번호 정보 전송 안 됨
        messagingTemplate.convertAndSend("/topic/rooms/" + code, new CoffeeSentEvent(response));
        return response;
    }

    // 체크리스트 4·8: 호스트 경로 — 서버가 인증한 accountId로만 판단, 해당 방 커피인지 재검증
    @Transactional
    public void deleteCoffeeByHost(Long coffeeId, String roomCode, Long accountId) {
        String code = roomCode.toUpperCase();
        // findByIdAndRoomCode: coffeeId + roomCode 동시 검증 → 다른 방 커피 ID 시도 시 404
        Coffee coffee = coffeeRepo.findByIdAndRoomCode(coffeeId, code)
                .orElseThrow(CoffeeNotFoundException::new);

        if (!coffee.getRoom().getHost().getId().equals(accountId)) {
            throw new ForbiddenException();
        }

        coffeeRepo.delete(coffee);
        messagingTemplate.convertAndSend("/topic/rooms/" + code, new CoffeeDeletedEvent(coffeeId));
    }

    // 체크리스트 8: 비밀번호 경로 — 저장된 hash와 BCrypt 비교로 소유권 재검증
    @Transactional
    public void deleteCoffeeByPassword(Long coffeeId, String roomCode, String rawPassword) {
        String code = roomCode.toUpperCase();
        Coffee coffee = coffeeRepo.findByIdAndRoomCode(coffeeId, code)
                .orElseThrow(CoffeeNotFoundException::new);

        // 체크리스트 2: rawPassword는 matches() 비교에만 사용하고 로그에 절대 남기지 않음
        if (!passwordEncoder.matches(rawPassword, coffee.getPasswordHash())) {
            throw new ForbiddenException();
        }

        coffeeRepo.delete(coffee);
        messagingTemplate.convertAndSend("/topic/rooms/" + code, new CoffeeDeletedEvent(coffeeId));
    }

    // ── 예외 클래스 ────────────────────────────────────────────────────────────

    public static class CoffeeNotFoundException extends RuntimeException {}

    public static class CoffeeValidationException extends RuntimeException {
        public CoffeeValidationException(String message) { super(message); }
    }
}
