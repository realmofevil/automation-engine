package dev.realmofevil.automation.engine.db;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DbClient {

    public List<String> querySingleColumn(String sql) {
        try (Statement stmt = DbContext.get().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<String> results = new ArrayList<>();
            while (rs.next()) {
                results.add(rs.getString(1));
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("DB query failed", e);
        }
    }

    public int executeUpdate(String sql) {
        try (Statement stmt = DbContext.get().createStatement()) {
            return stmt.executeUpdate(sql);
        } catch (Exception e) {
            throw new RuntimeException("DB update failed", e);
        }
    }
}
