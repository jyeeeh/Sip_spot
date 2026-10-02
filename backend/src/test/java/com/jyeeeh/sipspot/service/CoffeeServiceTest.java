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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

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
        Coffee c = mockCoffee(1L, "홍길동", "안녕", ROOM_CODE, HOST_ID);
        when(coffeeRepo.findByRoomCodeOrderByCreatedAtDesc(ROOM_CODE)).thenReturn(List.of(c));

        List<CoffeeResponse> result = coffeeService.getCoffees(ROOM_CODE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).message()).isEqualTo("안녕");
        assertThat(result.get(0).name()).isEqualTo("홍길동");
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

        CoffeeResponse result = coffeeService.sendCoffee(ROOM_CODE, "홍길동", "응원해요", "pass1234");

        assertThat(result.message()).isEqualTo("응원해요");
        assertThat(result.name()).isEqualTo("홍길동");

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

        CoffeeResponse result = coffeeService.sendCoffee(ROOM_CODE, "홍길동", "   ", "pass1234");

        assertThat(result.message()).isNull();
    }

    @Test
    void 이름_누락이면_CoffeeValidationException() {
        assertThatThrownBy(() -> coffeeService.sendCoffee(ROOM_CODE, "  ", "msg", "pass1234"))
                .isInstanceOf(CoffeeService.CoffeeValidationException.class);
        verifyNoInteractions(roomRepo, coffeeRepo, messagingTemplate);
    }

    @Test
    void 이름_10자_초과이면_CoffeeValidationException() {
        assertThatThrownBy(() -> coffeeService.sendCoffee(ROOM_CODE, "12345678901", "msg", "pass1234"))
                .isInstanceOf(CoffeeService.CoffeeValidationException.class);
        verifyNoInteractions(roomRepo, coffeeRepo, messagingTemplate);
    }

    @Test
    void 비밀번호_4자_미만이면_CoffeeValidationException() {
        assertThatThrownBy(() -> coffeeService.sendCoffee(ROOM_CODE, "홍길동", "msg", "123"))
                .isInstanceOf(CoffeeService.CoffeeValidationException.class);
        verifyNoInteractions(roomRepo, coffeeRepo, messagingTemplate);
    }

    @Test
    void 존재하지_않는_방이면_RoomNotFoundException() {
        when(roomRepo.findByCodeWithHost(ROOM_CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> coffeeService.sendCoffee(ROOM_CODE, "홍길동", "hi", "pass1234"))
                .isInstanceOf(RoomService.RoomNotFoundException.class);
    }

    // ── deleteCoffeeByHost ────────────────────────────────────────────────────

    @Test
    void 호스트가_커피_삭제_성공() {
        Coffee coffee = mockCoffee(5L, "홍길동", "msg", ROOM_CODE, HOST_ID);
        when(coffeeRepo.findByIdAndRoomCode(5L, ROOM_CODE)).thenReturn(Optional.of(coffee));

        coffeeService.deleteCoffeeByHost(5L, ROOM_CODE, HOST_ID);

        verify(coffeeRepo).delete(coffee);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms/" + ROOM_CODE), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(CoffeeDeletedEvent.class);
        CoffeeDeletedEvent event = (CoffeeDeletedEvent) captor.getValue();
        assertThat(event.coffeeId()).isEqualTo(5L);
    }

    @Test
    void 호스트가_아니면_ForbiddenException() {
        Coffee coffee = mockCoffee(5L, "홍길동", "msg", ROOM_CODE, HOST_ID);
        when(coffeeRepo.findByIdAndRoomCode(5L, ROOM_CODE)).thenReturn(Optional.of(coffee));

        assertThatThrownBy(() -> coffeeService.deleteCoffeeByHost(5L, ROOM_CODE, OTHER_ID))
                .isInstanceOf(ForbiddenException.class);
        verify(coffeeRepo, never()).delete(any());
    }

    @Test
    void 호스트_삭제시_존재하지_않는_커피이면_CoffeeNotFoundException() {
        when(coffeeRepo.findByIdAndRoomCode(99L, ROOM_CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> coffeeService.deleteCoffeeByHost(99L, ROOM_CODE, HOST_ID))
                .isInstanceOf(CoffeeService.CoffeeNotFoundException.class);
    }

    // ── deleteCoffeeByPassword ────────────────────────────────────────────────

    @Test
    void 올바른_비밀번호로_커피_삭제_성공() {
        String rawPassword = "pass1234";
        String hash = encoder.encode(rawPassword);
        Coffee coffee = mockCoffeeWithHash(5L, "홍길동", "msg", ROOM_CODE, HOST_ID, hash);
        when(coffeeRepo.findByIdAndRoomCode(5L, ROOM_CODE)).thenReturn(Optional.of(coffee));

        coffeeService.deleteCoffeeByPassword(5L, ROOM_CODE, rawPassword);

        verify(coffeeRepo).delete(coffee);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms/" + ROOM_CODE), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(CoffeeDeletedEvent.class);
    }

    @Test
    void 틀린_비밀번호이면_ForbiddenException() {
        String hash = encoder.encode("pass1234");
        Coffee coffee = mockCoffeeWithHash(5L, "홍길동", "msg", ROOM_CODE, HOST_ID, hash);
        when(coffeeRepo.findByIdAndRoomCode(5L, ROOM_CODE)).thenReturn(Optional.of(coffee));

        assertThatThrownBy(() -> coffeeService.deleteCoffeeByPassword(5L, ROOM_CODE, "wrong000"))
                .isInstanceOf(ForbiddenException.class);
        verify(coffeeRepo, never()).delete(any());
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

    private Coffee mockCoffee(Long id, String writerName, String message, String roomCode, Long hostId) {
        return mockCoffeeWithHash(id, writerName, message, roomCode, hostId, "$2a$10$dummy");
    }

    private Coffee mockCoffeeWithHash(Long id, String writerName, String message,
                                      String roomCode, Long hostId, String hash) {
        Room room = mockRoom(roomCode, hostId);
        Coffee coffee = mock(Coffee.class);
        when(coffee.getId()).thenReturn(id);
        when(coffee.getWriterName()).thenReturn(writerName);
        when(coffee.getMessage()).thenReturn(message);
        when(coffee.getPasswordHash()).thenReturn(hash);
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
