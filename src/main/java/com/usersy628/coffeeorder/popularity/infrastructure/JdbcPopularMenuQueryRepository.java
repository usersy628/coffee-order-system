package com.usersy628.coffeeorder.popularity.infrastructure;

import com.usersy628.coffeeorder.popularity.application.PopularMenu;
import com.usersy628.coffeeorder.popularity.application.PopularMenuQueryRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class JdbcPopularMenuQueryRepository implements PopularMenuQueryRepository {

	private final JdbcTemplate jdbcTemplate;

	public JdbcPopularMenuQueryRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public List<PopularMenu> findTopThreeByPaidAtBetween(Instant from, Instant to) {
		return jdbcTemplate.query("""
			SELECT oi.menu_id,
			       m.name AS menu_name,
			       SUM(oi.quantity) AS total_quantity
			FROM orders o
			JOIN order_item oi ON oi.order_id = o.id
			JOIN menu m ON m.id = oi.menu_id
			WHERE o.paid_at >= ?
			  AND o.paid_at < ?
			GROUP BY oi.menu_id, m.name
			ORDER BY total_quantity DESC, oi.menu_id ASC
			LIMIT 3
			""", (resultSet, rowNum) -> new PopularMenu(
			resultSet.getLong("menu_id"),
			resultSet.getString("menu_name"),
			toLongExactly(resultSet.getBigDecimal("total_quantity"))
		), Timestamp.from(from), Timestamp.from(to));
	}

	private long toLongExactly(BigDecimal value) {
		return value.longValueExact();
	}
}
