package com.jyeeeh.sipspot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank @Size(min = 1, max = 12) String nickname
) {}
