package dev.realmofevil.automation.engine.reporting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SmartRedactor {
    private static final Pattern SENSITIVE_JSON = Pattern.compile("(?i)\"(username|password|token|access_token|authorization|auth)\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SENSITIVE_HEADER = Pattern.compile("(?i)(Authorization|Cookie|Session)\\s*:\\s*(.*)");

    private SmartRedactor() {}

    public static String mask(String input) {
        if (input == null) return null;

        String masked = input;

        Matcher jsonMatcher = SENSITIVE_JSON.matcher(masked);
        masked = jsonMatcher.replaceAll(m -> "\"" + m.group(1) + "\": \"" + applyMask(m.group(2)) + "\"");

        Matcher headerMatcher = SENSITIVE_HEADER.matcher(masked);
        masked = headerMatcher.replaceAll(m -> m.group(1) + ": " + applyMask(m.group(2)));

        return masked;
    }

    public static String maskValue(String value) {
        return applyMask(value);
    }

    private static String applyMask(String value) {
        if (value == null) return "null";
        int len = value.length();
        if (len <= 4) return "*******";

        return value.substring(0, 2) + "*******" + value.substring(len - 3);
    }
}