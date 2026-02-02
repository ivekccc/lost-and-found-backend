-- Seed reports (requires users and categories to exist)
DELETE FROM reports WHERE title IN (
    'Lost iPhone 15 Pro',
    'Found car keys near park',
    'Lost brown leather wallet',
    'Found golden ring',
    'Lost black backpack'
);

INSERT INTO reports (title, description, type, category_id, status, location, created_at, expires_at, user_id, contact_email, contact_phone) VALUES
(
    'Lost iPhone 15 Pro',
    'Lost my iPhone 15 Pro (blue) near the central bus station. Has a black case with card holder.',
    'LOST',
    (SELECT id FROM report_categories WHERE name = 'Electronics'),
    'ACTIVE',
    'Central Bus Station, Main Street 1',
    NOW() - INTERVAL '2 days',
    NOW() + INTERVAL '28 days',
    (SELECT id FROM users WHERE email = 'user1@lostandfound.com'),
    'user1@lostandfound.com',
    '+381601234567'
),
(
    'Found car keys near park',
    'Found a set of car keys (Volkswagen) on the bench near the fountain in City Park.',
    'FOUND',
    (SELECT id FROM report_categories WHERE name = 'Keys'),
    'ACTIVE',
    'City Park, near main fountain',
    NOW() - INTERVAL '1 day',
    NOW() + INTERVAL '29 days',
    (SELECT id FROM users WHERE email = 'user2@lostandfound.com'),
    'user2@lostandfound.com',
    NULL
),
(
    'Lost brown leather wallet',
    'Lost my wallet in the shopping mall. Contains ID card and some cash. Brown leather, brand: Tommy Hilfiger.',
    'LOST',
    (SELECT id FROM report_categories WHERE name = 'Wallet'),
    'ACTIVE',
    'City Mall, 2nd floor',
    NOW() - INTERVAL '5 hours',
    NOW() + INTERVAL '30 days',
    (SELECT id FROM users WHERE email = 'user1@lostandfound.com'),
    'contact@email.com',
    '+381609876543'
),
(
    'Found golden ring',
    'Found a golden ring with small diamond on the beach. Looks like engagement ring.',
    'FOUND',
    (SELECT id FROM report_categories WHERE name = 'Jewelry'),
    'ACTIVE',
    'Sunset Beach, near lifeguard tower',
    NOW() - INTERVAL '3 days',
    NOW() + INTERVAL '27 days',
    (SELECT id FROM users WHERE email = 'user2@lostandfound.com'),
    'user2@lostandfound.com',
    '+381607654321'
),
(
    'Lost black backpack',
    'Left my black North Face backpack on the train. Contains laptop and notebooks.',
    'LOST',
    (SELECT id FROM report_categories WHERE name = 'Bags'),
    'RESOLVED',
    'Train Station, Platform 3',
    NOW() - INTERVAL '7 days',
    NOW() + INTERVAL '23 days',
    (SELECT id FROM users WHERE email = 'user1@lostandfound.com'),
    'user1@lostandfound.com',
    NULL
);
