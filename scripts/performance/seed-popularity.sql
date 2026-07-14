-- This script is intentionally limited to the disposable S11 performance database.
-- Run it only through docker compose -p coffee-order-system-perf -f docker-compose.performance.yml exec.

DROP PROCEDURE IF EXISTS s11_seed_popularity;

DELIMITER //

CREATE PROCEDURE s11_seed_popularity()
BEGIN
    IF DATABASE() <> 'coffee_order_perf' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'S11 seed is allowed only in coffee_order_perf';
    END IF;

    SET @menu_id_base = 10000;
    SET @order_id_base = 1000000;
    SET @order_item_id_base = 2000000;
    SET @window_to = UTC_TIMESTAMP(6);
    SET @window_from = DATE_SUB(@window_to, INTERVAL 168 HOUR);

    DROP TEMPORARY TABLE IF EXISTS s11_perf_numbers;
    CREATE TEMPORARY TABLE s11_perf_numbers (
        n INT NOT NULL PRIMARY KEY
    );

    INSERT INTO s11_perf_numbers (n)
    SELECT d0.n + 10 * d1.n + 100 * d2.n + 1000 * d3.n + 10000 * d4.n
    FROM (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) AS d0
    CROSS JOIN (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) AS d1
    CROSS JOIN (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) AS d2
    CROSS JOIN (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) AS d3
    CROSS JOIN (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) AS d4;

    START TRANSACTION;

    DELETE FROM order_event_outbox
    WHERE order_id >= @order_id_base
      AND order_id < @order_id_base + 100000;
    DELETE FROM point_history
    WHERE order_id >= @order_id_base
      AND order_id < @order_id_base + 100000;
    DELETE FROM order_item
    WHERE order_id >= @order_id_base
      AND order_id < @order_id_base + 100000;
    DELETE FROM orders
    WHERE id >= @order_id_base
      AND id < @order_id_base + 100000;
    DELETE FROM menu
    WHERE id >= @menu_id_base
      AND id < @menu_id_base + 100;

    INSERT INTO menu (id, name, price, status, created_at, updated_at)
    SELECT @menu_id_base + n,
           CONCAT('S11 perf menu-', LPAD(n, 3, '0')),
           1000,
           'ON_SALE',
           @window_to,
           @window_to
    FROM s11_perf_numbers
    WHERE n < 100;

    INSERT INTO orders (id, user_id, idempotency_key, request_hash, total_amount, status, created_at, paid_at)
    SELECT @order_id_base + n,
           1,
           CONCAT('s11-performance-order-', n),
           REPEAT('p', 64),
           6000,
           'PAID',
           paid_at,
           paid_at
    FROM (
        SELECT n,
               CASE
                   WHEN n < 70000 THEN DATE_SUB(@window_to, INTERVAL (1 + FLOOR(n * 604799 / 69999)) SECOND)
                   ELSE DATE_SUB(@window_from, INTERVAL (1 + FLOOR((n - 70000) * 1987199 / 29999)) SECOND)
               END AS paid_at
        FROM s11_perf_numbers
    ) AS order_timing;

    INSERT INTO order_item (id, order_id, menu_id, menu_name, unit_price, quantity, line_amount)
    SELECT @order_item_id_base + n * 3 + item_slot.slot,
           @order_id_base + n,
           @menu_id_base + MOD(n + item_slot.menu_offset, 100),
           CONCAT('S11 perf menu-', LPAD(MOD(n + item_slot.menu_offset, 100), 3, '0')),
           1000,
           item_slot.quantity,
           item_slot.quantity * 1000
    FROM s11_perf_numbers
    CROSS JOIN (
        SELECT 0 AS slot, 0 AS menu_offset, 1 AS quantity
        UNION ALL SELECT 1, 37, 2
        UNION ALL SELECT 2, 74, 3
    ) AS item_slot;

    IF (SELECT COUNT(*) FROM orders WHERE id >= @order_id_base AND id < @order_id_base + 100000) <> 100000
       OR (SELECT COUNT(*) FROM order_item WHERE id >= @order_item_id_base AND id < @order_item_id_base + 300000) <> 300000
       OR (SELECT COUNT(*) FROM menu WHERE id >= @menu_id_base AND id < @menu_id_base + 100) <> 100 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'S11 seed did not produce the expected fixture counts';
    END IF;

    COMMIT;

    SELECT COUNT(*) AS orders_seeded
    FROM orders
    WHERE id >= @order_id_base
      AND id < @order_id_base + 100000;

    SELECT COUNT(*) AS order_items_seeded
    FROM order_item
    WHERE id >= @order_item_id_base
      AND id < @order_item_id_base + 300000;
END//

DELIMITER ;

CALL s11_seed_popularity();
DROP PROCEDURE IF EXISTS s11_seed_popularity;
