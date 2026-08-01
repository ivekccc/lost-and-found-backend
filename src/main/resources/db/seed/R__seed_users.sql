DELETE
FROM users
WHERE email IN ('user1@lostandfound.com', 'user2@lostandfound.com', 'admin@lostandfound.com');

INSERT INTO users (email, username, password, status, role, created_at, first_name, last_name, phone_number,
                   active_city_id)
VALUES ('user1@lostandfound.com', 'user1', '$2a$12$zR4sesgo5t2e4w6Yn6T2rObRamOZJFddlNdgYIf01CoFU9.gLH/YO', 'ACTIVE',
        'USER', NOW(), 'Marko',
        'Petrovic', '+381601234567', (SELECT id FROM cities WHERE code = 'BG')),
       ('user2@lostandfound.com', 'user2', '$2a$12$zR4sesgo5t2e4w6Yn6T2rObRamOZJFddlNdgYIf01CoFU9.gLH/YO', 'ACTIVE',
        'USER', NOW(), 'Ana',
        'Jovanovic', '+381609876543', (SELECT id FROM cities WHERE code = 'BG')),
       ('admin@lostandfound.com', 'admin', '$2a$12$zR4sesgo5t2e4w6Yn6T2rObRamOZJFddlNdgYIf01CoFU9.gLH/YO', 'ACTIVE',
        'ADMIN', NOW(), 'Admin',
        'Adminovic', '+381601111111', (SELECT id FROM cities WHERE code = 'BG'));