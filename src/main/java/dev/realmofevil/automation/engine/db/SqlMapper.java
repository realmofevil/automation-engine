package dev.realmofevil.automation.engine.db;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Functional interface to handle ResultSet mapping that might throw SQLException.
 */
@FunctionalInterface
public interface SqlMapper<T> {
    T map(ResultSet rs) throws SQLException;
}