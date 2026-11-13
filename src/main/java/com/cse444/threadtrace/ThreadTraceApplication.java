package com.cse444.threadtrace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
public class ThreadTraceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ThreadTraceApplication.class, args);
    }
}
