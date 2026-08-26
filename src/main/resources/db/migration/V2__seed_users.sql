-- Seed the single demo user with the 100 EUR registration gift.
INSERT INTO users (id, balance_eur, version)
VALUES (1, 100.00, 0);

-- Keep the identity sequence ahead of the explicitly-inserted id.
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));

