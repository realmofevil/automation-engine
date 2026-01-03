package dev.realmofevil.automation.engine.bootstrap;

import dev.realmofevil.automation.engine.db.DbConfig;
import dev.realmofevil.automation.engine.db.DbCredentials;
import dev.realmofevil.automation.engine.operator.ApiAccount;
import dev.realmofevil.automation.engine.operator.OperatorConfig;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OperatorConfigLoader {

    private OperatorConfigLoader() {}

    @SuppressWarnings("unchecked")
    public static List<OperatorConfig> load(
            String envYaml,
            CliOverrides cli
    ) {
        Map<String, Object> root = YamlLoader.load(envYaml);
        Map<String, Object> env = (Map<String, Object>) root.get("environment");
        List<Map<String, Object>> operators =
                (List<Map<String, Object>>) env.get("operators");

        return operators.stream()
                .map(op -> resolveOperator(op, cli))
                .toList();
    }

    private static OperatorConfig resolveOperator(
            Map<String, Object> op,
            CliOverrides cli
    ) {
        String id = (String) op.get("id");

        Map<String, Object> domains = (Map<String, Object>) op.get("domains");

        URI desktop = URI.create(
                cli.getOrDefault("operator." + id + ".desktop",
                        (String) domains.get("desktop"))
        );

        URI mobile = URI.create(
                cli.getOrDefault("operator." + id + ".mobile",
                        (String) domains.get("mobile"))
        );

        int threads = ((Map<String, Object>) op.get("execution"))
                .get("threads") instanceof Integer i ? i : 1;

        String routes = (String) op.get("routes");

        Map<String, ApiAccount> apiAccounts = new HashMap<>();
        Map<String, Object> accounts =
                (Map<String, Object>) op.get("apiAccounts");

        accounts.forEach((name, value) -> {
            Map<String, String> v = (Map<String, String>) value;
            apiAccounts.put(name,
                    new ApiAccount(v.get("username"), v.get("password")));
        });

        Map<String, Object> db = (Map<String, Object>) op.get("db");
        Map<String, String> creds =
                (Map<String, String>) db.get("credentials");

        DbConfig dbConfig = new DbConfig(
                (String) db.get("type"),
                (String) db.get("host"),
                (int) db.get("port"),
                (String) db.get("database"),
                new DbCredentials(
                        creds.get("username"),
                        creds.get("password")
                )
        );

        return new OperatorConfig(
                id,
                desktop,
                mobile,
                threads,
                routes,
                apiAccounts,
                dbConfig
        );
    }
}