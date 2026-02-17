package dev.realmofevil.automation.engine.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class DateUtil {

    private DateUtil() {}

    /**
     * Returns current timestamp in milliseconds.
     */
    public static long nowMillis() {
        return Instant.now().toEpochMilli();
    }

    /**
     * Returns timestamp for N hours ago (useful for history queries).
     */
    public static long hoursAgo(int hours) {
        return Instant.now().minus(hours, ChronoUnit.HOURS).toEpochMilli();
    }

    /**
     * Returns timestamp for N minutes in future (useful for query windows).
     */
    public static long minutesFromNow(int minutes) {
        return Instant.now().plus(minutes, ChronoUnit.MINUTES).toEpochMilli();
    }

    /**
     * Formats current date-time with specific pattern.
     * 
     * @param pattern e.g., "yyyy-MM-dd HH:mm:ss"
     */
    public static String formatCurrentDate(String pattern) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Converts Epoch Millis to formatted String (UTC).
     */
    public static String formatMillis(long millis, String pattern) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.of("UTC"))
                .format(DateTimeFormatter.ofPattern(pattern));
    }
}