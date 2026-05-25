package com.cocoshowroom.server;

import com.cocoshowroom.server.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class CocoshowroomApplication {

    public static void main(String[] args) {
        SpringApplication.run(CocoshowroomApplication.class, args);
    }
}
