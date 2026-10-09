package com.proactiveos.events.entity;

import com.proactiveos.integrations.entity.ExternalProviderId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ExternalProviderIdConverter implements AttributeConverter<ExternalProviderId, String> {

    @Override
    public String convertToDatabaseColumn(ExternalProviderId attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public ExternalProviderId convertToEntityAttribute(String value) {
        return value == null ? null : ExternalProviderId.valueOf(value);
    }
}