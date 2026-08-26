package com.sporty.f1bets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class F1BetsApplication {

    public static void main(String[] args) {
        SpringApplication.run(F1BetsApplication.class, args);
    }

}
