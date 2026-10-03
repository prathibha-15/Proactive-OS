package com.proactiveos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.proactiveos.extraction.AiProviderProperties;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties(AiProviderProperties.class)
public class ProactiveOsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProactiveOsApplication.class, args);
    }
}