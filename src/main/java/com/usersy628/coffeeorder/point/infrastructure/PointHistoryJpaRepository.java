package com.usersy628.coffeeorder.point.infrastructure;

import java.util.Optional;

import com.usersy628.coffeeorder.point.application.PointHistoryRepository;
import com.usersy628.coffeeorder.point.domain.PointHistory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointHistoryJpaRepository extends JpaRepository<PointHistory, Long>, PointHistoryRepository {

	@Override
	@Query("""
		select history from PointHistory history
		where history.userId = :userId
		  and history.type = :#{T(com.usersy628.coffeeorder.point.domain.PointHistoryType).CHARGE}
		  and history.idempotencyKey = :idempotencyKey
		""")
	Optional<PointHistory> findChargeByUserIdAndIdempotencyKey(
		@Param("userId") long userId,
		@Param("idempotencyKey") String idempotencyKey
	);

	@Override
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select history from PointHistory history
		where history.userId = :userId
		  and history.type = :#{T(com.usersy628.coffeeorder.point.domain.PointHistoryType).CHARGE}
		  and history.idempotencyKey = :idempotencyKey
		""")
	Optional<PointHistory> findChargeByUserIdAndIdempotencyKeyForUpdate(
		@Param("userId") long userId,
		@Param("idempotencyKey") String idempotencyKey
	);
}
