package com.usersy628.coffeeorder.support.testcontainers;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class MySqlContainerConfiguration {

    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4.10");

    @Bean
    @ServiceConnection
    public MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(MYSQL_IMAGE)
                .withDatabaseName("coffee_order")
                .withUsername("coffee_test")
                .withPassword("coffee_test")
                .withCommand("--default-time-zone=+00:00");
    }
}
