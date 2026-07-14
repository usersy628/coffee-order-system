package com.usersy628.coffeeorder.menu.api;

import com.usersy628.coffeeorder.support.testcontainers.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MySqlIntegrationTest
@Transactional
class MenuApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void returnsAllMenusIncludingStoppedInIdOrder() throws Exception {
        jdbcTemplate.update("DELETE FROM menu");
        insertMenu(30L, "콜드브루", 5500L, "ON_SALE");
        insertMenu(10L, "판매 중지 메뉴", 6000L, "STOPPED");
        insertMenu(20L, "카푸치노", 5200L, "ON_SALE");

        mockMvc.perform(get("/api/menus"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("X-Trace-Id", matchesPattern("[0-9a-f]{32}")))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].length()").value(4))
                .andExpect(jsonPath("$[0].menuId").value(10))
                .andExpect(jsonPath("$[0].name").value("판매 중지 메뉴"))
                .andExpect(jsonPath("$[0].price").value(6000))
                .andExpect(jsonPath("$[0].status").value("STOPPED"))
                .andExpect(jsonPath("$[1].menuId").value(20))
                .andExpect(jsonPath("$[2].menuId").value(30));
    }

    @Test
    void returnsAnEmptyArrayWhenNoMenusExist() throws Exception {
        jdbcTemplate.update("DELETE FROM menu");

        mockMvc.perform(get("/api/menus"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("X-Trace-Id", matchesPattern("[0-9a-f]{32}")))
                .andExpect(content().json("[]"));
    }

    private void insertMenu(long id, String name, long price, String status) {
        jdbcTemplate.update("""
                INSERT INTO menu (id, name, price, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """, id, name, price, status);
    }
}
