package com.jyeeeh.sipspot.exception;

import com.jyeeeh.sipspot.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RoomService.RoomNotFoundException.class)
    public ProblemDetail handleNotFound(RoomService.RoomNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "존재하지 않는 방입니다.");
    }

    @ExceptionHandler(RoomService.UnauthorizedException.class)
    public ProblemDetail handleUnauthorized(RoomService.UnauthorizedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "인증 정보가 없거나 유효하지 않습니다.");
    }

    @ExceptionHandler(RoomService.RoomFullException.class)
    public ProblemDetail handleRoomFull(RoomService.RoomFullException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "정원이 초과됐습니다.");
    }

    @ExceptionHandler(RoomService.NicknameConflictException.class)
    public ProblemDetail handleNicknameConflict(RoomService.NicknameConflictException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("입력값이 올바르지 않습니다.");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ProblemDetail handleRateLimit(RateLimitExceededException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
    }
}
