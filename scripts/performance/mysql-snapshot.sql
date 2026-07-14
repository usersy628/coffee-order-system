-- Read-only S11 observation snapshot. Run against coffee_order_perf after the application is healthy.

SELECT UTC_TIMESTAMP(6) AS captured_at_utc,
       DATABASE() AS current_database,
       @@version AS mysql_version;

SELECT COUNT(*) AS s11_orders,
       (SELECT COUNT(*) FROM order_item WHERE id >= 2000000 AND id < 2300000) AS s11_order_items,
       (SELECT COUNT(*) FROM menu WHERE id >= 10000 AND id < 10100) AS s11_menus
FROM orders
WHERE id >= 1000000
  AND id < 1100000;

SHOW GLOBAL STATUS
WHERE Variable_name IN (
    'Threads_connected',
    'Threads_running',
    'Innodb_row_lock_current_waits',
    'Innodb_row_lock_time',
    'Innodb_row_lock_waits',
    'Innodb_deadlocks',
    'Connection_errors_max_connections'
);

SELECT status,
       COUNT(*) AS event_count,
       MIN(created_at) AS oldest_created_at,
       MAX(created_at) AS newest_created_at
FROM order_event_outbox
GROUP BY status
ORDER BY status;

SELECT COUNT(*) AS pending_count,
       MIN(created_at) AS oldest_pending_created_at,
       CASE
           WHEN COUNT(*) = 0 THEN NULL
           ELSE TIMESTAMPDIFF(MICROSECOND, MIN(created_at), UTC_TIMESTAMP(6)) / 1000000.0
       END AS oldest_pending_age_seconds
FROM order_event_outbox
WHERE status = 'PENDING';
