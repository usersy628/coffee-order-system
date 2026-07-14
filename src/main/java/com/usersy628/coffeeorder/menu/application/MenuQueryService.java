package com.usersy628.coffeeorder.menu.application;

import java.util.List;

import com.usersy628.coffeeorder.menu.domain.Menu;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MenuQueryService {

	private final MenuQueryRepository menuQueryRepository;

	public MenuQueryService(MenuQueryRepository menuQueryRepository) {
		this.menuQueryRepository = menuQueryRepository;
	}

	public List<Menu> getMenus() {
		return menuQueryRepository.findAllByOrderByIdAsc();
	}
}
