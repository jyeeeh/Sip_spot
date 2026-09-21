package com.jyeeeh.sipspot.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RoomCodeGeneratorTest {

    private final RoomCodeGenerator generator = new RoomCodeGenerator();
    private static final Set<Character> ALLOWED = Set.copyOf(
            RoomCodeGenerator.CHARSET.chars()
                    .mapToObj(c -> (char) c)
                    .toList()
    );

    @RepeatedTest(100)
    void 길이는_7이어야_한다() {
        assertThat(generator.generate()).hasSize(7);
    }

    @RepeatedTest(100)
    void 허용된_문자만_사용한다() {
        String code = generator.generate();
        for (char c : code.toCharArray()) {
            assertThat(ALLOWED).contains(c);
        }
    }

    @Test
    void 혼동_문자_0_1_I_O를_포함하지_않는다() {
        Set<Character> forbidden = Set.of('0', '1', 'I', 'O');
        for (char c : RoomCodeGenerator.CHARSET.toCharArray()) {
            assertThat(forbidden).doesNotContain(c);
        }
    }

    @Test
    void charset_길이는_32이다() {
        assertThat(RoomCodeGenerator.CHARSET).hasSize(32);
    }
}
