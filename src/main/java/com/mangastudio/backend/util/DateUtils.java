package com.mangastudio.backend.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    
    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DEFAULT_PATTERN);
        return dateTime.format(formatter);
    }
    
    // Returns whether the specified time is overdue.
    public static boolean isOverdue(LocalDateTime deadline) {
        if (deadline == null) return false;
        return LocalDateTime.now().isAfter(deadline);
    }
}
