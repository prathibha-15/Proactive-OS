package com.proactiveos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.proactiveos.extraction.AiProviderProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiProviderProperties.class)
public class ProactiveOsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProactiveOsApplication.class, args);
    }
}