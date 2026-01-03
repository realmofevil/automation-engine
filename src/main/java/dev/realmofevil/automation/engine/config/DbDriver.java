package dev.realmofevil.automation.engine.config;

public enum DbDriver {
    ORACLE("oracle.jdbc.OracleDriver"),
    POSTGRES("org.postgresql.Driver");

    private final String driverClass;

    DbDriver(String driverClass) {
        this.driverClass = driverClass;
    }

    public String driverClass() {
        return driverClass;
    }
}
