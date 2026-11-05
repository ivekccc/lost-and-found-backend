WITH v(category, prompt, prompt_sr, kind, choices, choices_sr) AS (VALUES
    ('Electronics', 'What is on the lock screen or wallpaper?', 'Šta se nalazi na zaključanom ekranu ili pozadini?', 'TEXT', NULL, NULL),
    ('Electronics', 'Describe any scratches, cracks or other visible damage.', 'Opiši ogrebotine, pukotine ili druga vidljiva oštećenja.', 'TEXT', NULL, NULL),
    ('Electronics', 'What are the last 4 digits of the serial number or IMEI?', 'Koje su poslednje 4 cifre serijskog broja ili IMEI broja?', 'TEXT', NULL, NULL),
    ('Electronics', 'What color is the device?', 'Koje je boje uređaj?', 'CHOICE',
        '["Black","White","Silver","Gold","Blue","Purple","Red","Other"]',
        '["Crna","Bela","Srebrna","Zlatna","Plava","Ljubičasta","Crvena","Drugo"]'),
    ('Electronics', 'Does the device have a case or cover?', 'Da li uređaj ima masku ili futrolu?', 'CHOICE',
        '["No case","Transparent case","Colored case","Wallet-style case","Other"]',
        '["Bez maske","Providna maska","Maska u boji","Futrola na preklop","Drugo"]'),

    ('Documents', 'What is the full name printed on the document?', 'Koje je ime i prezime na dokumentu?', 'TEXT', NULL, NULL),
    ('Documents', 'What is the document number, or part of it?', 'Koji je broj dokumenta, ili bar deo broja?', 'TEXT', NULL, NULL),
    ('Documents', 'What is the date of birth printed on the document?', 'Koji je datum rođenja na dokumentu?', 'TEXT', NULL, NULL),
    ('Documents', 'What type of document is it?', 'Koja je vrsta dokumenta?', 'CHOICE',
        '["ID card","Passport","Driver''s license","Student card","Other"]',
        '["Lična karta","Pasoš","Vozačka dozvola","Studentska kartica","Drugo"]'),

    ('Keys', 'Describe the keychain or anything attached to the keys.', 'Opiši privezak ili šta je sve zakačeno za ključeve.', 'TEXT', NULL, NULL),
    ('Keys', 'Describe the most distinctive key (color, shape, brand).', 'Opiši najupadljiviji ključ (boja, oblik, proizvođač).', 'TEXT', NULL, NULL),
    ('Keys', 'How many keys are on the ring?', 'Koliko ključeva je na alki?', 'CHOICE',
        '["1","2","3","4","5 or more"]',
        '["1","2","3","4","5 ili više"]'),
    ('Keys', 'Is there a car key on the ring?', 'Da li je na alki i ključ od automobila?', 'CHOICE',
        '["Yes","No"]',
        '["Da","Ne"]'),

    ('Wallet', 'What brand and color is the wallet?', 'Koje je marke i boje novčanik?', 'TEXT', NULL, NULL),
    ('Wallet', 'Name one card or personal item inside the wallet.', 'Navedi jednu karticu ili ličnu stvar iz novčanika.', 'TEXT', NULL, NULL),
    ('Wallet', 'Approximately how much cash was inside?', 'Otprilike koliko je gotovine bilo unutra?', 'TEXT', NULL, NULL),
    ('Wallet', 'What material is the wallet made of?', 'Od kog materijala je novčanik?', 'CHOICE',
        '["Leather","Fabric","Synthetic","Other"]',
        '["Koža","Tkanina","Sintetika","Drugo"]'),
    ('Wallet', 'How does the wallet close?', 'Kako se novčanik zatvara?', 'CHOICE',
        '["Fold-over","Zipper","Clasp","Open"]',
        '["Na preklop","Rajsferšlus","Kopča","Otvoren"]'),

    ('Jewelry', 'Describe any engraving or inscription.', 'Opiši gravuru ili natpis, ako postoji.', 'TEXT', NULL, NULL),
    ('Jewelry', 'Describe the stones or pendant, if any.', 'Opiši kamenje ili privezak, ako postoji.', 'TEXT', NULL, NULL),
    ('Jewelry', 'Describe any damage or signs of wear.', 'Opiši oštećenja ili tragove nošenja.', 'TEXT', NULL, NULL),
    ('Jewelry', 'What metal or material is it made of?', 'Od kog je metala ili materijala?', 'CHOICE',
        '["Gold","Silver","Stainless steel","Platinum","Other"]',
        '["Zlato","Srebro","Nerđajući čelik","Platina","Drugo"]'),

    ('Clothing', 'What brand and size is the item?', 'Koje je marke i veličine odevni predmet?', 'TEXT', NULL, NULL),
    ('Clothing', 'Describe anything left in the pockets.', 'Opiši šta je ostalo u džepovima.', 'TEXT', NULL, NULL),
    ('Clothing', 'Describe any stains, repairs or modifications.', 'Opiši fleke, popravke ili prepravke.', 'TEXT', NULL, NULL),
    ('Clothing', 'What is the main color?', 'Koja je glavna boja?', 'CHOICE',
        '["Black","White","Gray","Blue","Red","Green","Brown","Other"]',
        '["Crna","Bela","Siva","Plava","Crvena","Zelena","Braon","Drugo"]'),

    ('Bags', 'Name one specific item inside the bag.', 'Navedi jednu određenu stvar iz torbe.', 'TEXT', NULL, NULL),
    ('Bags', 'What brand is the bag?', 'Koje je marke torba?', 'TEXT', NULL, NULL),
    ('Bags', 'Describe any keychains, patches or other marks on the bag.', 'Opiši priveske, prišivke ili druge oznake na torbi.', 'TEXT', NULL, NULL),
    ('Bags', 'How does the bag close?', 'Kako se torba zatvara?', 'CHOICE',
        '["Zipper","Buttons","Clasp","Drawstring","Open top"]',
        '["Rajsferšlus","Dugmad","Kopča","Vezica","Otvorena"]'),
    ('Bags', 'What size is the bag?', 'Koje je veličine torba?', 'CHOICE',
        '["Small","Medium","Large"]',
        '["Mala","Srednja","Velika"]'),

    ('Pets', 'What is the pet''s name and does it respond to it?', 'Kako se ljubimac zove i da li se odaziva na ime?', 'TEXT', NULL, NULL),
    ('Pets', 'Describe any distinctive markings (spots, scars, patches).', 'Opiši posebne oznake (pege, ožiljke, šare).', 'TEXT', NULL, NULL),
    ('Pets', 'What collar or tag was the pet wearing?', 'Kakvu ogrlicu ili pločicu je ljubimac nosio?', 'TEXT', NULL, NULL),
    ('Pets', 'Is the pet microchipped?', 'Da li ljubimac ima čip?', 'CHOICE',
        '["Yes","No","Not sure"]',
        '["Da","Ne","Ne znam"]'),

    ('Other', 'Describe a detail only the owner would know.', 'Opiši detalj koji je poznat samo vlasniku.', 'TEXT', NULL, NULL),
    ('Other', 'Describe any damage, wear or repairs.', 'Opiši oštećenja, tragove korišćenja ili popravke.', 'TEXT', NULL, NULL),
    ('Other', 'What is written, printed or engraved on the item, if anything?', 'Da li na predmetu nešto piše, ili je nešto odštampano ili ugravirano?', 'TEXT', NULL, NULL),
    ('Other', 'What is the approximate size?', 'Koje je otprilike veličine?', 'CHOICE',
        '["Fits in a pocket","Fits in a bag","Larger"]',
        '["Staje u džep","Staje u torbu","Veće"]')
),
updated AS (
    UPDATE question_templates t
    SET prompt_sr = v.prompt_sr, default_choices_sr = v.choices_sr::jsonb
    FROM v
    WHERE t.prompt = v.prompt AND t.kind = v.kind
    RETURNING t.id
)
INSERT INTO question_templates (category_id, prompt, prompt_sr, kind, default_choices, default_choices_sr)
SELECT c.id, v.prompt, v.prompt_sr, v.kind, v.choices::jsonb, v.choices_sr::jsonb
FROM v
JOIN report_categories c ON c.name = v.category
WHERE NOT EXISTS (
    SELECT 1 FROM question_templates t WHERE t.prompt = v.prompt AND t.kind = v.kind
);
