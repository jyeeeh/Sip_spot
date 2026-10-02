package com.jyeeeh.cupdrop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CupdropApplication {

    public static void main(String[] args) {
        SpringApplication.run(CupdropApplication.class, args);
    }
}
