package com.usersy628.coffeeorder.menu.infrastructure;

import com.usersy628.coffeeorder.menu.application.MenuQueryRepository;
import com.usersy628.coffeeorder.menu.domain.Menu;
import org.springframework.data.repository.Repository;

public interface MenuJpaRepository extends Repository<Menu, Long>, MenuQueryRepository {
}
