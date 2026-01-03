package dev.realmofevil.automation.engine.bootstrap;

public final class CliOverridesFactory {

    private CliOverridesFactory() {}

    public static CliOverrides from(CliArguments args) {
        return new CliOverrides(args.asMap());
    }
}