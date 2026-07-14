package com.usersy628.coffeeorder.popularity.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PopularMenuQueryService {

	private static final long WINDOW_HOURS = 168L;

	private final PopularMenuQueryRepository popularMenuQueryRepository;
	private final Clock clock;

	public PopularMenuQueryService(PopularMenuQueryRepository popularMenuQueryRepository, Clock clock) {
		this.popularMenuQueryRepository = popularMenuQueryRepository;
		this.clock = clock;
	}

	public QueryResult getPopularMenus() {
		Instant to = clock.instant().truncatedTo(ChronoUnit.MICROS);
		Instant from = to.minus(WINDOW_HOURS, ChronoUnit.HOURS);
		return new QueryResult(from, to, popularMenuQueryRepository.findTopThreeByPaidAtBetween(from, to));
	}

	public record QueryResult(Instant from, Instant to, List<PopularMenu> items) {

		public QueryResult {
			items = List.copyOf(items);
		}
	}
}
