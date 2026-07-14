package com.usersy628.coffeeorder;

import com.usersy628.coffeeorder.support.testcontainers.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@MySqlIntegrationTest
class CoffeeOrderApplicationTests {

    @Autowired
    private Clock clock;

    @Test
    void loadsApplicationContextWithUtcClock() {
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    }
}
