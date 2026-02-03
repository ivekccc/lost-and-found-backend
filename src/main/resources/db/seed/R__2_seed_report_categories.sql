-- Seed report categories
DELETE FROM report_categories WHERE name IN (
    'Electronics',
    'Documents',
    'Keys',
    'Wallet',
    'Jewelry',
    'Clothing',
    'Bags',
    'Pets',
    'Other'
);

INSERT INTO report_categories (name, is_active) VALUES
    ('Electronics', TRUE),
    ('Documents', TRUE),
    ('Keys', TRUE),
    ('Wallet', TRUE),
    ('Jewelry', TRUE),
    ('Clothing', TRUE),
    ('Bags', TRUE),
    ('Pets', TRUE),
    ('Other', TRUE);
