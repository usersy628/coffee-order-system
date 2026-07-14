package com.usersy628.coffeeorder.popularity.application;

import java.time.Instant;
import java.util.List;

public interface PopularMenuQueryRepository {

	List<PopularMenu> findTopThreeByPaidAtBetween(Instant from, Instant to);
}
