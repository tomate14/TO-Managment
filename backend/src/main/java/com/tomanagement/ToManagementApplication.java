package com.tomanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class ToManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToManagementApplication.class, args);
    }
}
