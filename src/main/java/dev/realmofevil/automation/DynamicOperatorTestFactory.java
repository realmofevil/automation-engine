package dev.realmofevil.automation;

import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.context.ExecutionContextHolder;
import dev.realmofevil.automation.engine.env.Environment;
import dev.realmofevil.automation.engine.operator.Operator;
import dev.realmofevil.automation.engine.routing.RouteRegistry;
import dev.realmofevil.automation.engine.auth.AuthChain;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class DynamicOperatorTestFactory {

    @TestFactory
    Stream<DynamicTest> operatorSuite() {

        Environment env = new Environment("DEV");

        List<Operator> operators = List.of(
            new Operator("opA", "https://dev-a", new RouteRegistry("/api"),
                    new AuthChain(List.of()), 2),
            new Operator("opB", "https://dev-b", new RouteRegistry("/api"),
                    new AuthChain(List.of()), 1)
        );

        return operators.stream().map(op ->
            dynamicTest("Suite for " + op.name(), () -> {
                ExecutionContextHolder.set(
                    new ExecutionContext(env, op, "default-suite")
                );
                try {
                    // invoke real test classes here
                    System.out.println("Executing tests for " + op.name());
                } finally {
                    ExecutionContextHolder.clear();
                }
            })
        );
    }
}