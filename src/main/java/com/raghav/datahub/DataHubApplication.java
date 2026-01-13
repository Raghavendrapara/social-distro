package com.raghav.datahub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class DataHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataHubApplication.class, args);
    }
}
