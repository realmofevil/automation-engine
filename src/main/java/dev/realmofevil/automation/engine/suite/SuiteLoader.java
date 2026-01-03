package dev.realmofevil.automation.engine.suite;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public final class SuiteLoader {

    private SuiteLoader() {}

    public static void loadAndExecute(String[] args) {
        String suite = args.length > 0 ? args[0] : "suites/dev.yaml";
        Yaml yaml = new Yaml();

        try (InputStream in = SuiteLoader.class
                .getClassLoader()
                .getResourceAsStream(suite)) {

            if (in == null) {
                throw new IllegalStateException("Suite not found: " + suite);
            }

            Map<String, Object> data = yaml.load(in);
            System.out.println("Loaded suite: " + data.get("suite"));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}