package com.proactiveos.integrations.provider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.proactiveos.integrations.entity.ExternalProviderId;
import org.springframework.stereotype.Component;

@Component
public class ExternalProviderRegistry {

    private final Map<ExternalProviderId, ExternalDataProvider> providers;

    public ExternalProviderRegistry(List<ExternalDataProvider> providers) {
        this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(
                ExternalDataProvider::providerId, Function.identity()));
    }

    public List<ExternalDataProvider> all() {
        return providers.values().stream()
                .sorted((left, right) -> left.providerId().compareTo(right.providerId()))
                .toList();
    }

    public ExternalDataProvider get(ExternalProviderId providerId) {
        ExternalDataProvider provider = providers.get(providerId);
        if (provider == null) {
            throw new UnsupportedExternalProviderException(providerId);
        }
        return provider;
    }
}