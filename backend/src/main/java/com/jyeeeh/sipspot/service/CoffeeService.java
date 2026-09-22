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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CoffeeService {

    private final CoffeeRepository coffeeRepo;
    private final RoomRepository roomRepo;
    private final SimpMessagingTemplate messagingTemplate;

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
    public CoffeeResponse sendCoffee(String roomCode, String message) {
        String code = roomCode.toUpperCase();
        Room room = roomRepo.findByCodeWithHost(code)
                .orElseThrow(() -> new RoomService.RoomNotFoundException(code));

        String trimmed = (message != null) ? message.strip() : null;
        if (trimmed != null && trimmed.isEmpty()) trimmed = null;

        Coffee coffee = new Coffee(room, trimmed);
        coffeeRepo.save(coffee);

        CoffeeResponse response = CoffeeResponse.from(coffee);
        messagingTemplate.convertAndSend("/topic/rooms/" + code, new CoffeeSentEvent(response));
        return response;
    }

    @Transactional
    public void deleteCoffee(Long coffeeId, String roomCode, Long accountId) {
        String code = roomCode.toUpperCase();
        Coffee coffee = coffeeRepo.findByIdAndRoomCode(coffeeId, code)
                .orElseThrow(CoffeeNotFoundException::new);

        if (!coffee.getRoom().getHost().getId().equals(accountId)) {
            throw new ForbiddenException();
        }

        coffeeRepo.delete(coffee);
        messagingTemplate.convertAndSend("/topic/rooms/" + code, new CoffeeDeletedEvent(coffeeId));
    }

    // ── 예외 클래스 ────────────────────────────────────────────────────────────

    public static class CoffeeNotFoundException extends RuntimeException {}
}
