package com.webapp.backend.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for IP Address storage.
 * Converts String to database format without modification.
 * MySQL will handle IPv4 and IPv6 addresses as VARCHAR.
 */
@Converter(autoApply = false)
public class InetTypeConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        // Return the string as-is; MySQL will handle the IP address storage
        return attribute;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        // Return the string as-is from the database
        return dbData;
    }
}
