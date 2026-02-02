package dev.realmofevil.automation.engine.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility for generating dynamic test data.
 * Replaces the legacy Utils random generation methods.
 */
public final class DataGenerator {

    private DataGenerator() {}

    /**
     * Generates a random integer within a range.
     */
    public static int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * Generates a random alphanumeric string.
     * Useful for unique usernames or transaction references.
     */
    public static String randomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Generates a timestamp string formatted for specific API requirements.
     */
    public static String timestamp(String pattern) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern, Locale.US));
    }

    /**
     * Generates a standard Username based on legacy patterns.
     */
    public static String randomUsername() {
        return "Auto" + randomString(8);
    }
}