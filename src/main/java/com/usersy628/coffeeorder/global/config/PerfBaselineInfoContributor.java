package com.usersy628.coffeeorder.global.config;

import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Keeps the local S11 runner from treating a healthy application connected to a normal database as a perf run.
 * This bean is deliberately limited to the opt-in {@code perf} profile.
 */
@Component
@Profile("perf")
class PerfBaselineInfoContributor implements ApplicationRunner, InfoContributor {

    private static final String EXPECTED_HOST = "127.0.0.1";
    private static final String EXPECTED_DATABASE = "coffee_order_perf";

    private final DataSource dataSource;
    private final int expectedPort;
    private volatile DataSourceIdentity identity;

    PerfBaselineInfoContributor(DataSource dataSource, Environment environment) {
        this.dataSource = dataSource;
        this.expectedPort = parsePort(environment.getProperty("PERF_MYSQL_PORT", "3308"));
    }

    @Override
    public void run(ApplicationArguments args) {
        DataSourceIdentity resolvedIdentity = resolveIdentity();
        if (!isExpected(resolvedIdentity, expectedPort)) {
            throw new IllegalStateException("The perf profile must connect to 127.0.0.1:" + expectedPort
                + "/coffee_order_perf, but the resolved datasource identity is different.");
        }
        identity = resolvedIdentity;
    }

    @Override
    public void contribute(Info.Builder builder) {
        DataSourceIdentity resolvedIdentity = identity;
        if (resolvedIdentity == null) {
            builder.withDetail("s11", Map.of(
                "baseline-profile", "perf",
                "connection", "not-verified"
            ));
            return;
        }
        builder.withDetail("s11", Map.of(
            "baseline-profile", "perf",
            "database", resolvedIdentity.database(),
            "host", resolvedIdentity.host(),
            "port", resolvedIdentity.port()
        ));
    }

    private DataSourceIdentity resolveIdentity() {
        try (Connection connection = dataSource.getConnection()) {
            return parseJdbcIdentity(connection.getMetaData().getURL(), connection.getCatalog());
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to verify the perf datasource identity.", exception);
        }
    }

    static DataSourceIdentity parseJdbcIdentity(String jdbcUrl, String catalog) {
        DataSourceIdentity jdbcIdentity = parseJdbcUrlIdentity(jdbcUrl);
        if (!jdbcIdentity.database().equals(catalog)) {
            throw new IllegalArgumentException("The perf datasource identity is incomplete or inconsistent.");
        }

        return jdbcIdentity;
    }

    static DataSourceIdentity parseJdbcUrlIdentity(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("The perf datasource must use a MySQL JDBC URL.");
        }

        URI uri;
        try {
            uri = URI.create(jdbcUrl.substring("jdbc:".length()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("The perf datasource identity is incomplete or inconsistent.");
        }
        String databaseFromUrl = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
        if (uri.getHost() == null || uri.getPort() < 0 || databaseFromUrl.isBlank()) {
            throw new IllegalArgumentException("The perf datasource identity is incomplete or inconsistent.");
        }

        return new DataSourceIdentity(uri.getHost(), uri.getPort(), databaseFromUrl);
    }

    static boolean isExpected(DataSourceIdentity identity, int expectedPort) {
        return EXPECTED_HOST.equals(identity.host())
            && expectedPort == identity.port()
            && EXPECTED_DATABASE.equals(identity.database());
    }

    static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("PERF_MYSQL_PORT must be a TCP port between 1 and 65535.");
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("PERF_MYSQL_PORT must be a number.", exception);
        }
    }

    record DataSourceIdentity(String host, int port, String database) {
    }
}
