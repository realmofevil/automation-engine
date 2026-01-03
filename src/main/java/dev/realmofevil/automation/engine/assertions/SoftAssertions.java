package dev.realmofevil.automation.engine.assertions;

import io.qameta.allure.Allure;

import java.util.ArrayList;
import java.util.List;

public class SoftAssertions {

    private final List<String> failures = new ArrayList<>();

    public void assertTrue(String description, boolean condition) {
        Allure.step(description);

        if (!condition) {
            failures.add(description);
            Allure.addAttachment("Soft assertion failed", description);
        }
    }

    public void verify() {
        if (!failures.isEmpty()) {
            throw new AssertionError(
                "Soft assertion failures:\n" + String.join("\n", failures));
        }
    }
}
