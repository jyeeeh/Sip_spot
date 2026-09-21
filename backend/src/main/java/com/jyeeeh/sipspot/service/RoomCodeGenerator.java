package com.jyeeeh.sipspot.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class RoomCodeGenerator {

    static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 7;
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        char[] code = new char[CODE_LENGTH];
        for (int i = 0; i < CODE_LENGTH; i++) {
            code[i] = CHARSET.charAt(random.nextInt(CHARSET.length()));
        }
        return new String(code);
    }
}
