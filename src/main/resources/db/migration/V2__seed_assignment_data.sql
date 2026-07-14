-- Assignment-only fixed seed data. Production reference data should be managed
-- separately from schema migrations. This migration targets a new empty schema,
-- so it intentionally uses plain INSERT statements without upsert behavior.
INSERT INTO users (id, created_at)
VALUES (1, UTC_TIMESTAMP(6)),
       (2, UTC_TIMESTAMP(6)),
       (3, UTC_TIMESTAMP(6));

INSERT INTO point_wallet (user_id, balance, updated_at)
VALUES (1, 0, UTC_TIMESTAMP(6)),
       (2, 0, UTC_TIMESTAMP(6)),
       (3, 0, UTC_TIMESTAMP(6));

INSERT INTO menu (id, name, price, status, created_at, updated_at)
VALUES (1, '아메리카노', 4500, 'ON_SALE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
       (2, '카페라테', 5000, 'ON_SALE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
       (3, '디카페인 아메리카노', 5000, 'STOPPED', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
