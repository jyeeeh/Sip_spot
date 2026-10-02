package com.jyeeeh.cupdrop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 체크리스트 3: 서버 레벨 입력 검증 — 컨트롤러 @Valid 1차 방어
public record SendCoffeeRequest(
        @NotBlank @Size(max = 10) String name,
        @Size(max = 200) String message,
        @NotBlank @Size(min = 4) String password
) {}
