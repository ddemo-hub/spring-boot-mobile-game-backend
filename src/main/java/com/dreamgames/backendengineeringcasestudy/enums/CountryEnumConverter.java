package com.dreamgames.backendengineeringcasestudy.enums;

import java.util.Locale;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CountryEnumConverter implements AttributeConverter<Country, String> {
    @Override
    public String convertToDatabaseColumn(Country countryEnum) {
       return countryEnum.toString();
    }

    @Override
    public Country convertToEntityAttribute(String dbValue) {
        Country countryEnum = Country.valueOf(dbValue.replaceAll(" ", "_").toUpperCase(Locale.forLanguageTag("en-US")));
        return countryEnum;
    }
}