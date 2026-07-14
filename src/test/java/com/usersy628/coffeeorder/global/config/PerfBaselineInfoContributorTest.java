package com.usersy628.coffeeorder.global.config;

import java.util.Map;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PerfBaselineInfoContributorTest {

    @Test
    void parsesTheResolvedMysqlConnectionIdentityWithoutQueryParameters() {
        PerfBaselineInfoContributor.DataSourceIdentity identity = PerfBaselineInfoContributor.parseJdbcIdentity(
            "jdbc:mysql://127.0.0.1:3308/coffee_order_perf?connectionTimeZone=UTC",
            "coffee_order_perf"
        );

        assertThat(identity.host()).isEqualTo("127.0.0.1");
        assertThat(identity.port()).isEqualTo(3308);
        assertThat(identity.database()).isEqualTo("coffee_order_perf");
        assertThat(PerfBaselineInfoContributor.isExpected(identity, 3308)).isTrue();
    }

    @Test
    void rejectsAConnectionWhoseCatalogDoesNotMatchItsJdbcUrl() {
        assertThatThrownBy(() -> PerfBaselineInfoContributor.parseJdbcIdentity(
            "jdbc:mysql://127.0.0.1:3308/coffee_order_perf",
            "coffee_order"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnOverriddenPerfDatasourceUrlBeforeBeanInitialization() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("PERF_MYSQL_PORT", "3308")
            .withProperty("spring.datasource.url", "jdbc:mysql://127.0.0.1:3307/coffee_order");

        PerfDataSourceConfigurationGuard guard = new PerfDataSourceConfigurationGuard();
        guard.setEnvironment(environment);

        assertThatThrownBy(() -> guard.postProcessBeanFactory(new DefaultListableBeanFactory()))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsAnOverriddenHikariJdbcUrlBeforeBeanInitialization() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("PERF_MYSQL_PORT", "3308")
            .withProperty("spring.datasource.url", "jdbc:mysql://127.0.0.1:3308/coffee_order_perf")
            .withProperty("spring.datasource.hikari.jdbc-url", "jdbc:mysql://127.0.0.1:3307/coffee_order");
        PerfDataSourceConfigurationGuard guard = new PerfDataSourceConfigurationGuard();
        guard.setEnvironment(environment);

        assertThatThrownBy(() -> guard.postProcessBeanFactory(new DefaultListableBeanFactory()))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void appliesThePerfConfigurationGuardDuringContextRefresh() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("perf");
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "PERF_MYSQL_PORT", "3308",
                "spring.datasource.url", "jdbc:mysql://127.0.0.1:${PERF_MYSQL_PORT:3308}/coffee_order_perf"
            )));
            context.register(PerfDataSourceConfigurationGuard.class);

            assertThatCode(context::refresh).doesNotThrowAnyException();
        }
    }
}
