package dev.realmofevil.automation.engine.reporting;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public final class EnvironmentFileWriter {

    public static void write(ReportingContext ctx) {
        Properties props = new Properties();
        props.put("Environment", ctx.environment());
        props.put("Operator", ctx.operator());
        props.put("Suite", ctx.suite());
        props.put("ExecutionId", ctx.executionId().toString());

        Path path = Paths.get("allure-results/environment.properties");

        try (OutputStream os = Files.newOutputStream(path)) {
            props.store(os, "Execution Environment");
        } catch (IOException e) {
            throw new RuntimeException("Failed to write environment.properties", e);
        }
    }
}