package dev.realmofevil.automation.engine.suite;

import dev.realmofevil.automation.engine.bootstrap.YamlLoader;

import java.util.List;
import java.util.Map;

public final class SuiteLoader {

    private SuiteLoader() {}

    @SuppressWarnings("unchecked")
    public static SuiteDefinition load(String suiteYaml) {
        Map<String, Object> root = YamlLoader.load("suites/" + suiteYaml);
        Map<String, Object> suite = (Map<String, Object>) root.get("suite");

        List<Map<String, Object>> tests =
                (List<Map<String, Object>>) suite.get("tests");

        return new SuiteDefinition(
                (String) suite.get("name"),
                ((Map<String, List<String>>) suite.get("operators"))
                        .get("include"),
                tests.stream()
                        .map(t -> new TestDefinition(
                                (String) t.get("class"),
                                (List<String>) t.get("tags")
                        ))
                        .toList()
        );
    }
}