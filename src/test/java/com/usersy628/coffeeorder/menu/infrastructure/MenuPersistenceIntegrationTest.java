package com.usersy628.coffeeorder.menu.infrastructure;

import java.time.Instant;
import java.util.List;

import com.usersy628.coffeeorder.menu.application.MenuQueryRepository;
import com.usersy628.coffeeorder.menu.domain.Menu;
import com.usersy628.coffeeorder.support.testcontainers.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@MySqlIntegrationTest
@Transactional
class MenuPersistenceIntegrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MenuQueryRepository menuQueryRepository;

	@Test
	void mapsMysqlMicrosecondTimestampsToUtcInstants() throws NoSuchFieldException {
		jdbcTemplate.update("DELETE FROM menu");
		jdbcTemplate.update("""
			INSERT INTO menu (id, name, price, status, created_at, updated_at)
			VALUES (
				40,
				'시간 매핑 테스트 메뉴',
				5500,
				'ON_SALE',
				'2026-07-14 12:34:56.123456',
				'2026-07-14 12:34:57.654321'
			)
			""");

		List<Menu> menus = menuQueryRepository.findAllByOrderByIdAsc();
		assertThat(menus).hasSize(1);

		Menu menu = menus.get(0);
		Object createdAt = ReflectionTestUtils.getField(menu, "createdAt");
		Object updatedAt = ReflectionTestUtils.getField(menu, "updatedAt");

		assertThat(Menu.class.getDeclaredField("createdAt").getType()).isEqualTo(Instant.class);
		assertThat(Menu.class.getDeclaredField("updatedAt").getType()).isEqualTo(Instant.class);
		assertThat(createdAt).isEqualTo(Instant.parse("2026-07-14T12:34:56.123456Z"));
		assertThat(updatedAt).isEqualTo(Instant.parse("2026-07-14T12:34:57.654321Z"));
		assertThat(((Instant) createdAt).getNano()).isEqualTo(123_456_000);
		assertThat(((Instant) updatedAt).getNano()).isEqualTo(654_321_000);
	}
}
