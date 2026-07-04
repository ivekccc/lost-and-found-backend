-- Seed verification question templates per category.
-- Runs after R__seed_report_categories (Flyway orders repeatable migrations by description).
-- Deletes only the seeded prompts so admin-created templates survive a reseed.

DELETE FROM question_templates WHERE (prompt, kind) IN (
    SELECT v.prompt, v.kind FROM (VALUES
        ('What is on the lock screen or wallpaper?', 'TEXT'),
        ('Describe any scratches, cracks or other visible damage.', 'TEXT'),
        ('What are the last 4 digits of the serial number or IMEI?', 'TEXT'),
        ('What color is the device?', 'CHOICE'),
        ('Does the device have a case or cover?', 'CHOICE'),
        ('What is the full name printed on the document?', 'TEXT'),
        ('What is the document number, or part of it?', 'TEXT'),
        ('What is the date of birth printed on the document?', 'TEXT'),
        ('What type of document is it?', 'CHOICE'),
        ('Describe the keychain or anything attached to the keys.', 'TEXT'),
        ('Describe the most distinctive key (color, shape, brand).', 'TEXT'),
        ('How many keys are on the ring?', 'CHOICE'),
        ('Is there a car key on the ring?', 'CHOICE'),
        ('What brand and color is the wallet?', 'TEXT'),
        ('Name one card or personal item inside the wallet.', 'TEXT'),
        ('Approximately how much cash was inside?', 'TEXT'),
        ('What material is the wallet made of?', 'CHOICE'),
        ('How does the wallet close?', 'CHOICE'),
        ('Describe any engraving or inscription.', 'TEXT'),
        ('Describe the stones or pendant, if any.', 'TEXT'),
        ('Describe any damage or signs of wear.', 'TEXT'),
        ('What metal or material is it made of?', 'CHOICE'),
        ('What brand and size is the item?', 'TEXT'),
        ('Describe anything left in the pockets.', 'TEXT'),
        ('Describe any stains, repairs or modifications.', 'TEXT'),
        ('What is the main color?', 'CHOICE'),
        ('Name one specific item inside the bag.', 'TEXT'),
        ('What brand is the bag?', 'TEXT'),
        ('Describe any keychains, patches or other marks on the bag.', 'TEXT'),
        ('How does the bag close?', 'CHOICE'),
        ('What size is the bag?', 'CHOICE'),
        ('What is the pet''s name and does it respond to it?', 'TEXT'),
        ('Describe any distinctive markings (spots, scars, patches).', 'TEXT'),
        ('What collar or tag was the pet wearing?', 'TEXT'),
        ('Is the pet microchipped?', 'CHOICE'),
        ('Describe a detail only the owner would know.', 'TEXT'),
        ('Describe any damage, wear or repairs.', 'TEXT'),
        ('What is written, printed or engraved on the item, if anything?', 'TEXT'),
        ('What is the approximate size?', 'CHOICE')
    ) AS v(prompt, kind)
);

INSERT INTO question_templates (category_id, prompt, kind, default_choices)
SELECT c.id, v.prompt, v.kind, v.choices::jsonb
FROM (VALUES
    ('Electronics', 'What is on the lock screen or wallpaper?', 'TEXT', NULL),
    ('Electronics', 'Describe any scratches, cracks or other visible damage.', 'TEXT', NULL),
    ('Electronics', 'What are the last 4 digits of the serial number or IMEI?', 'TEXT', NULL),
    ('Electronics', 'What color is the device?', 'CHOICE', '["Black","White","Silver","Gold","Blue","Purple","Red","Other"]'),
    ('Electronics', 'Does the device have a case or cover?', 'CHOICE', '["No case","Transparent case","Colored case","Wallet-style case","Other"]'),

    ('Documents', 'What is the full name printed on the document?', 'TEXT', NULL),
    ('Documents', 'What is the document number, or part of it?', 'TEXT', NULL),
    ('Documents', 'What is the date of birth printed on the document?', 'TEXT', NULL),
    ('Documents', 'What type of document is it?', 'CHOICE', '["ID card","Passport","Driver''s license","Student card","Other"]'),

    ('Keys', 'Describe the keychain or anything attached to the keys.', 'TEXT', NULL),
    ('Keys', 'Describe the most distinctive key (color, shape, brand).', 'TEXT', NULL),
    ('Keys', 'How many keys are on the ring?', 'CHOICE', '["1","2","3","4","5 or more"]'),
    ('Keys', 'Is there a car key on the ring?', 'CHOICE', '["Yes","No"]'),

    ('Wallet', 'What brand and color is the wallet?', 'TEXT', NULL),
    ('Wallet', 'Name one card or personal item inside the wallet.', 'TEXT', NULL),
    ('Wallet', 'Approximately how much cash was inside?', 'TEXT', NULL),
    ('Wallet', 'What material is the wallet made of?', 'CHOICE', '["Leather","Fabric","Synthetic","Other"]'),
    ('Wallet', 'How does the wallet close?', 'CHOICE', '["Fold-over","Zipper","Clasp","Open"]'),

    ('Jewelry', 'Describe any engraving or inscription.', 'TEXT', NULL),
    ('Jewelry', 'Describe the stones or pendant, if any.', 'TEXT', NULL),
    ('Jewelry', 'Describe any damage or signs of wear.', 'TEXT', NULL),
    ('Jewelry', 'What metal or material is it made of?', 'CHOICE', '["Gold","Silver","Stainless steel","Platinum","Other"]'),

    ('Clothing', 'What brand and size is the item?', 'TEXT', NULL),
    ('Clothing', 'Describe anything left in the pockets.', 'TEXT', NULL),
    ('Clothing', 'Describe any stains, repairs or modifications.', 'TEXT', NULL),
    ('Clothing', 'What is the main color?', 'CHOICE', '["Black","White","Gray","Blue","Red","Green","Brown","Other"]'),

    ('Bags', 'Name one specific item inside the bag.', 'TEXT', NULL),
    ('Bags', 'What brand is the bag?', 'TEXT', NULL),
    ('Bags', 'Describe any keychains, patches or other marks on the bag.', 'TEXT', NULL),
    ('Bags', 'How does the bag close?', 'CHOICE', '["Zipper","Buttons","Clasp","Drawstring","Open top"]'),
    ('Bags', 'What size is the bag?', 'CHOICE', '["Small","Medium","Large"]'),

    ('Pets', 'What is the pet''s name and does it respond to it?', 'TEXT', NULL),
    ('Pets', 'Describe any distinctive markings (spots, scars, patches).', 'TEXT', NULL),
    ('Pets', 'What collar or tag was the pet wearing?', 'TEXT', NULL),
    ('Pets', 'Is the pet microchipped?', 'CHOICE', '["Yes","No","Not sure"]'),

    ('Other', 'Describe a detail only the owner would know.', 'TEXT', NULL),
    ('Other', 'Describe any damage, wear or repairs.', 'TEXT', NULL),
    ('Other', 'What is written, printed or engraved on the item, if anything?', 'TEXT', NULL),
    ('Other', 'What is the approximate size?', 'CHOICE', '["Fits in a pocket","Fits in a bag","Larger"]')
) AS v(category, prompt, kind, choices)
JOIN report_categories c ON c.name = v.category;
