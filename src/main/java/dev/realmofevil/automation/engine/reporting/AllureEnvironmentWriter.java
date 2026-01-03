package dev.realmofevil.automation.engine.reporting;

import dev.realmofevil.automation.engine.config.EnvironmentConfig;
import dev.realmofevil.automation.engine.context.ExecutionContext;

import java.io.FileWriter;
import java.util.Properties;

public final class AllureEnvironmentWriter {

    /**
    private static final String RESULTS_DIR = "target/allure-results";

    private AllureEnvironmentWriter() {}

    public static void write() {
        try {
            Properties props = new Properties();
            var config = ExecutionContext.get();

            props.setProperty("Environment", config.environment().name());
            props.setProperty("Operator",
                    config.operator() == null ? "generic" : config.operator().name());
            props.setProperty("Base URL",
                    config.environment().protocol() + "://" +
                            config.environment().baseDomain());
            props.setProperty("Parallelism",
                    String.valueOf(config.suite().parallelism()));

            File dir = new File(RESULTS_DIR);
            dir.mkdirs();

            File file = new File(dir, "environment.properties");
            try (FileWriter writer = new FileWriter(file)) {
                props.store(writer, "Allure Environment");
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to write Allure environment", e);
        }
    }
    **/

    private static final String OUTPUT = "allure-results/environment.properties";

    private AllureEnvironmentWriter() {}

    public static void write() {
        EnvironmentConfig environment = ExecutionContext.get().environment();
        Properties props = new Properties();

        props.setProperty("environment", environment.name());
        props.setProperty("scheme", environment.scheme()); // fix use of prefixed https protocol
        props.setProperty("timeoutMs", String.valueOf(environment.timeoutMs()));

        environment.operators().forEach((operator, endpoint) -> {
            props.setProperty(
                    "operator." + operator + ".domain",
                    endpoint.domain());

            props.setProperty(
                    "operator." + operator + ".basePath",
                    endpoint.basePath());

            props.setProperty(
                    "operator." + operator + ".parallelism",
                    String.valueOf(endpoint.parallelism()));
        });

        writeFile(props);
    }

    private static void writeFile(Properties props) {
        try (FileWriter writer = new FileWriter(OUTPUT)) {
            props.store(writer, "Allure execution environment");
        } catch (Exception e) {
            throw new RuntimeException("Failed to write Allure environment", e);
        }
    }
}
