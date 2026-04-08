DELETE FROM users WHERE email IN ('user1@lostandfound.com', 'user2@lostandfound.com');

INSERT INTO users (email, username, password, status, created_at, first_name, last_name, phone_number) VALUES
('user1@lostandfound.com', 'user1', '$2a$12$zR4sesgo5t2e4w6Yn6T2rObRamOZJFddlNdgYIf01CoFU9.gLH/YO', 'ACTIVE', NOW(), 'Marko', 'Petrovic', '+381601234567'),
('user2@lostandfound.com', 'user2', '$2a$12$zR4sesgo5t2e4w6Yn6T2rObRamOZJFddlNdgYIf01CoFU9.gLH/YO', 'ACTIVE', NOW(), 'Ana', 'Jovanovic', '+381609876543');
