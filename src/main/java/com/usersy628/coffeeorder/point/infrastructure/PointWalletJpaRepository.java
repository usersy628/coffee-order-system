package com.usersy628.coffeeorder.point.infrastructure;

import java.util.Optional;

import com.usersy628.coffeeorder.point.application.PointWalletRepository;
import com.usersy628.coffeeorder.point.domain.PointWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointWalletJpaRepository extends JpaRepository<PointWallet, Long>, PointWalletRepository {

	@Override
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select wallet from PointWallet wallet where wallet.userId = :userId")
	Optional<PointWallet> findByUserIdForUpdate(@Param("userId") long userId);
}
