package dev.realmofevil.automation.engine.bootstrap;

import dev.realmofevil.automation.engine.suite.SuiteLoader;

public final class EngineBootstrap {
    private EngineBootstrap() {}
    public static void run(String[] args) {
        SuiteLoader.loadAndExecute(args);
    }
}