package com.jyeeeh.sipspot.service;

import com.jyeeeh.sipspot.domain.Account;
import com.jyeeeh.sipspot.domain.Coffee;
import com.jyeeeh.sipspot.domain.Room;
import com.jyeeeh.sipspot.dto.CoffeeResponse;
import com.jyeeeh.sipspot.dto.ws.CoffeeDeletedEvent;
import com.jyeeeh.sipspot.dto.ws.CoffeeSentEvent;
import com.jyeeeh.sipspot.exception.ForbiddenException;
import com.jyeeeh.sipspot.repository.CoffeeRepository;
import com.jyeeeh.sipspot.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CoffeeServiceTest {

    private CoffeeRepository coffeeRepo;
    private RoomRepository roomRepo;
    private SimpMessagingTemplate messagingTemplate;
    private CoffeeService coffeeService;

    private static final String ROOM_CODE = "ABC1234";
    private static final Long HOST_ID = 1L;
    private static final Long OTHER_ID = 2L;

    @BeforeEach
    void setUp() {
        coffeeRepo = mock(CoffeeRepository.class);
        roomRepo = mock(RoomRepository.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        coffeeService = new CoffeeService(coffeeRepo, roomRepo, messagingTemplate);
    }

    // ── getCoffees ────────────────────────────────────────────────────────────

    @Test
    void 커피_목록_반환() {
        Coffee c = mockCoffee(1L, "안녕", ROOM_CODE, HOST_ID);
        when(coffeeRepo.findByRoomCodeOrderByCreatedAtDesc(ROOM_CODE)).thenReturn(List.of(c));

        List<CoffeeResponse> result = coffeeService.getCoffees(ROOM_CODE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).message()).isEqualTo("안녕");
    }

    // ── sendCoffee ────────────────────────────────────────────────────────────

    @Test
    void 커피_전송_성공_브로드캐스트() {
        Room room = mockRoom(ROOM_CODE, HOST_ID);
        when(roomRepo.findByCodeWithHost(ROOM_CODE)).thenReturn(Optional.of(room));
        when(coffeeRepo.save(any(Coffee.class))).thenAnswer(inv -> {
            Coffee c = inv.getArgument(0);
            setId(c, 10L);
            return c;
        });

        CoffeeResponse result = coffeeService.sendCoffee(ROOM_CODE, "응원해요");

        assertThat(result.message()).isEqualTo("응원해요");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms/" + ROOM_CODE), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(CoffeeSentEvent.class);
        CoffeeSentEvent event = (CoffeeSentEvent) captor.getValue();
        assertThat(event.type()).isEqualTo("COFFEE_SENT");
    }

    @Test
    void 메시지_빈_문자열이면_null로_저장() {
        Room room = mockRoom(ROOM_CODE, HOST_ID);
        when(roomRepo.findByCodeWithHost(ROOM_CODE)).thenReturn(Optional.of(room));
        when(coffeeRepo.save(any(Coffee.class))).thenAnswer(inv -> {
            Coffee c = inv.getArgument(0);
            setId(c, 11L);
            return c;
        });

        CoffeeResponse result = coffeeService.sendCoffee(ROOM_CODE, "   ");

        assertThat(result.message()).isNull();
    }

    @Test
    void 존재하지_않는_방이면_RoomNotFoundException() {
        when(roomRepo.findByCodeWithHost(ROOM_CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> coffeeService.sendCoffee(ROOM_CODE, "hi"))
                .isInstanceOf(RoomService.RoomNotFoundException.class);
    }

    // ── deleteCoffee ──────────────────────────────────────────────────────────

    @Test
    void 호스트가_커피_삭제_성공() {
        Coffee coffee = mockCoffee(5L, "msg", ROOM_CODE, HOST_ID);
        when(coffeeRepo.findByIdAndRoomCode(5L, ROOM_CODE)).thenReturn(Optional.of(coffee));

        coffeeService.deleteCoffee(5L, ROOM_CODE, HOST_ID);

        verify(coffeeRepo).delete(coffee);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms/" + ROOM_CODE), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(CoffeeDeletedEvent.class);
        CoffeeDeletedEvent event = (CoffeeDeletedEvent) captor.getValue();
        assertThat(event.coffeeId()).isEqualTo(5L);
    }

    @Test
    void 호스트가_아니면_ForbiddenException() {
        Coffee coffee = mockCoffee(5L, "msg", ROOM_CODE, HOST_ID);
        when(coffeeRepo.findByIdAndRoomCode(5L, ROOM_CODE)).thenReturn(Optional.of(coffee));

        assertThatThrownBy(() -> coffeeService.deleteCoffee(5L, ROOM_CODE, OTHER_ID))
                .isInstanceOf(ForbiddenException.class);
        verify(coffeeRepo, never()).delete(any());
    }

    @Test
    void 존재하지_않는_커피이면_CoffeeNotFoundException() {
        when(coffeeRepo.findByIdAndRoomCode(99L, ROOM_CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> coffeeService.deleteCoffee(99L, ROOM_CODE, HOST_ID))
                .isInstanceOf(CoffeeService.CoffeeNotFoundException.class);
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Room mockRoom(String code, Long hostId) {
        Account host = mock(Account.class);
        when(host.getId()).thenReturn(hostId);

        Room room = mock(Room.class);
        when(room.getCode()).thenReturn(code);
        when(room.getHost()).thenReturn(host);
        return room;
    }

    private Coffee mockCoffee(Long id, String message, String roomCode, Long hostId) {
        Room room = mockRoom(roomCode, hostId);
        Coffee coffee = mock(Coffee.class);
        when(coffee.getId()).thenReturn(id);
        when(coffee.getMessage()).thenReturn(message);
        when(coffee.getStatus()).thenReturn("FREE_SENT");
        when(coffee.getCreatedAt()).thenReturn(OffsetDateTime.now());
        when(coffee.getRoom()).thenReturn(room);
        return coffee;
    }

    // Coffee.id는 private이고 setter가 없으므로 리플렉션으로 주입
    private void setId(Coffee coffee, Long id) {
        try {
            var field = Coffee.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(coffee, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
