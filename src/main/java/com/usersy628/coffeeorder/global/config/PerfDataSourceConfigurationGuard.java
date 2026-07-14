package com.usersy628.coffeeorder.global.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Validates the resolved perf JDBC URL before singleton creation, including Flyway initialization.
 */
@Component
@Profile("perf")
class PerfDataSourceConfigurationGuard implements BeanFactoryPostProcessor, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Environment configuredEnvironment = requireEnvironment();
        int expectedPort = PerfBaselineInfoContributor.parsePort(
            configuredEnvironment.getProperty("PERF_MYSQL_PORT", "3308")
        );
        assertExpectedUrl(configuredEnvironment.getRequiredProperty("spring.datasource.url"), expectedPort);

        String hikariJdbcUrl = configuredEnvironment.getProperty("spring.datasource.hikari.jdbc-url");
        if (hikariJdbcUrl != null && !hikariJdbcUrl.isBlank()) {
            assertExpectedUrl(hikariJdbcUrl, expectedPort);
        }
    }

    private static void assertExpectedUrl(String jdbcUrl, int expectedPort) {
        PerfBaselineInfoContributor.DataSourceIdentity identity =
            PerfBaselineInfoContributor.parseJdbcUrlIdentity(jdbcUrl);
        if (!PerfBaselineInfoContributor.isExpected(identity, expectedPort)) {
            throw new IllegalStateException("The perf profile must configure 127.0.0.1:" + expectedPort
                + "/coffee_order_perf before datasource or Flyway initialization.");
        }
    }

    private Environment requireEnvironment() {
        if (environment == null) {
            throw new IllegalStateException("The perf datasource guard did not receive an Environment.");
        }
        return environment;
    }
}
