DELETE FROM users WHERE email IN ('user1@lostandfound.com', 'user2@lostandfound.com');

INSERT INTO users (email, username, password, status, created_at) VALUES
('user1@lostandfound.com', 'user1', '$2a$12$zR4sesgo5t2e4w6Yn6T2rObRamOZJFddlNdgYIf01CoFU9.gLH/YO', 'ACTIVE', NOW()),
('user2@lostandfound.com', 'user2', '$2a$12$zR4sesgo5t2e4w6Yn6T2rObRamOZJFddlNdgYIf01CoFU9.gLH/YO', 'ACTIVE', NOW());
