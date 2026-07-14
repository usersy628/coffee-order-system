package com.usersy628.coffeeorder.point.application;

import java.util.Optional;

import com.usersy628.coffeeorder.point.domain.PointWallet;

public interface PointWalletRepository {

	Optional<PointWallet> findByUserIdForUpdate(long userId);
}
