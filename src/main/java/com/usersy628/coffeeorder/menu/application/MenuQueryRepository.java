package com.usersy628.coffeeorder.menu.application;

import java.util.List;

import com.usersy628.coffeeorder.menu.domain.Menu;

public interface MenuQueryRepository {

	List<Menu> findAllByOrderByIdAsc();
}
