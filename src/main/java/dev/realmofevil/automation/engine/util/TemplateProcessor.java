package dev.realmofevil.automation.engine.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TemplateProcessor {

    /**
     * Recursive replacement of placeholders {{key}} with values from the context map.
     */
    @SuppressWarnings("unchecked")
    public static Object process(Object template, Map<String, Object> context) {
        if (template instanceof Map) {
            Map<String, Object> result = new HashMap<>();
            ((Map<String, Object>) template).forEach((k, v) -> result.put(k, process(v, context)));
            return result;
        } else if (template instanceof List) {
            List<Object> result = new ArrayList<>();
            ((List<Object>) template).forEach(item -> result.add(process(item, context)));
            return result;
        } else if (template instanceof String) {
            return replacePlaceholder((String) template, context);
        }
        return template;
    }

    private static Object replacePlaceholder(String value, Map<String, Object> context) {
        if (value.startsWith("{{") && value.endsWith("}}")) {
            String key = value.substring(2, value.length() - 2).trim();

            if (key.equals("generated.uuid")) {
                return UUID.randomUUID().toString();
            }
            if (key.equals("generated.timestamp")) {
                return System.currentTimeMillis();
            }

            return lookup(key, context);
        }
        return value;
    }

    private static Object lookup(String key, Map<String, Object> context) {
        // Support dot notation (e.g., "account.metadata.type")
        String[] parts = key.split("\\.");
        Object current = context;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }
}