package com.jyeeeh.sipspot.exception;

import com.jyeeeh.sipspot.service.AccountService;
import com.jyeeeh.sipspot.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountService.AuthException.class)
    public ProblemDetail handleAuth(AccountService.AuthException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    @ExceptionHandler(AccountService.UnauthorizedException.class)
    public ProblemDetail handleUnauthorized(AccountService.UnauthorizedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "인증 정보가 없거나 유효하지 않습니다.");
    }

    @ExceptionHandler(AccountService.DuplicateUsernameException.class)
    public ProblemDetail handleDuplicateUsername(AccountService.DuplicateUsernameException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "이미 사용 중인 아이디입니다.");
    }

    @ExceptionHandler(AccountService.DuplicateNicknameException.class)
    public ProblemDetail handleDuplicateNickname(AccountService.DuplicateNicknameException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "이미 사용 중인 닉네임입니다.");
    }

    @ExceptionHandler(RoomService.RoomNotFoundException.class)
    public ProblemDetail handleNotFound(RoomService.RoomNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "존재하지 않는 방입니다.");
    }

    @ExceptionHandler(RoomService.AlreadyHasRoomException.class)
    public ProblemDetail handleAlreadyHasRoom(RoomService.AlreadyHasRoomException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "이미 방이 존재합니다.");
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
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
    }
}
