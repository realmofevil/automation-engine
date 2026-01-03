package dev.realmofevil.automation.engine.execution;

import dev.realmofevil.automation.engine.config.OperatorConfig;
import dev.realmofevil.automation.engine.config.SuiteDefinition;

import java.util.List;

/**
 * Represents a finalized plan for a specific operator.
 * Contains the operator configuration and the specific list of tests to run.
 */
public record OperatorExecutionPlan(
    OperatorConfig operator,
    List<SuiteDefinition.TestEntry> tests
) {}