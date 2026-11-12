CREATE OR REPLACE FUNCTION transliterate_serbian(input TEXT) RETURNS TEXT
    LANGUAGE sql
    IMMUTABLE
    PARALLEL SAFE
    STRICT
AS $$
SELECT translate(
    replace(replace(replace(replace(replace(replace(input,
        'Љ', 'Lj'), 'љ', 'lj'), 'Њ', 'Nj'), 'њ', 'nj'), 'Џ', 'Dž'), 'џ', 'dž'),
    'АБВГДЂЕЖЗИЈКЛМНОПРСТЋУФХЦЧШабвгдђежзијклмнопрстћуфхцчш',
    'ABVGDĐEŽZIJKLMNOPRSTĆUFHCČŠabvgdđežzijklmnoprstćufhcčš')
$$;
