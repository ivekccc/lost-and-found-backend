INSERT INTO report_categories (name, name_sr, is_active) VALUES
    ('Electronics', 'Elektronika', TRUE),
    ('Documents', 'Dokumenta', TRUE),
    ('Keys', 'Ključevi', TRUE),
    ('Wallet', 'Novčanik', TRUE),
    ('Jewelry', 'Nakit', TRUE),
    ('Clothing', 'Odeća', TRUE),
    ('Bags', 'Torbe', TRUE),
    ('Pets', 'Ljubimci', TRUE),
    ('Other', 'Ostalo', TRUE)
ON CONFLICT (name) DO UPDATE SET name_sr = EXCLUDED.name_sr;
