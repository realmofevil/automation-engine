package dev.realmofevil.automation.engine.context;

import dev.realmofevil.automation.engine.env.Environment;
import dev.realmofevil.automation.engine.operator.Operator;

public final class ExecutionContext {

    private final Environment environment;
    private final Operator operator;
    private final String suite;

    public ExecutionContext(Environment environment, Operator operator, String suite) {
        this.environment = environment;
        this.operator = operator;
        this.suite = suite;
    }

    public Environment environment() {
        return environment;
    }

    public Operator operator() {
        return operator;
    }

    public String suite() {
        return suite;
    }
}