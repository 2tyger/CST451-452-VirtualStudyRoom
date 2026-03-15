package com.tygilbert.virtualstudyroom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VirtualstudyroomApplication {

    public static void main(String[] args) {
        SpringApplication.run(VirtualstudyroomApplication.class, args);
    }
}