package com.jyeeeh.sipspot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SipspotApplication {

    public static void main(String[] args) {
        SpringApplication.run(SipspotApplication.class, args);
    }
}
