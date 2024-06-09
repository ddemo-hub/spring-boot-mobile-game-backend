package com.dreamgames.backendengineeringcasestudy.enums;

public enum Country {
    TURKEY("Turkey"),
    THE_UNITED_STATES("the United States"),
    THE_UNITED_KINGDOM("the United Kingdom"),
    FRANCE("France"),
    GERMANY("Germany");

    private final String countryName;

    Country(String countryName) {
        this.countryName = countryName;
    }

    @Override
    public String toString() {
        return countryName;
    }
}