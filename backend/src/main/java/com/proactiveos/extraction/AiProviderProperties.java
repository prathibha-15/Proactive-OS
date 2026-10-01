package com.proactiveos.extraction;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "proactiveos.ai")
public record AiProviderProperties(String baseUrl, String model, String apiKey, String timeZone) {
}
