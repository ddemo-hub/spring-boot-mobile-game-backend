package com.dreamgames.backendengineeringcasestudy.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateUtil {
    /**
     * Get the current time in UTC
     * @return Current UTC time 
     */
    public static LocalDateTime getCurrentTimeUTC() {
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneId.of("UTC"));
        return nowUtc.toLocalDateTime();
    }

    /**
     * Parse a date string argument into a LocalDateTime object
     * @param date A date string in the 'yyyy-MM-dd' format
     * @return LocalDateTime that corresponds to that date
     */
    public static LocalDateTime parseDateArg(String date) {
        LocalDateTime localDate;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            localDate = LocalDate.parse(date, formatter).atStartOfDay();    
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date format. The supported date format is 'yyyy-MM-dd'");
        }

        return localDate;
    }
}
