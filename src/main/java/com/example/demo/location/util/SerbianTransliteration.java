package com.example.demo.location.util;

import java.util.Map;

public final class SerbianTransliteration {

    private static final Map<Character, String> LOWER = Map.ofEntries(
            Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"), Map.entry('г', "g"),
            Map.entry('д', "d"), Map.entry('ђ', "đ"), Map.entry('е', "e"), Map.entry('ж', "ž"),
            Map.entry('з', "z"), Map.entry('и', "i"), Map.entry('ј', "j"), Map.entry('к', "k"),
            Map.entry('л', "l"), Map.entry('љ', "lj"), Map.entry('м', "m"), Map.entry('н', "n"),
            Map.entry('њ', "nj"), Map.entry('о', "o"), Map.entry('п', "p"), Map.entry('р', "r"),
            Map.entry('с', "s"), Map.entry('т', "t"), Map.entry('ћ', "ć"), Map.entry('у', "u"),
            Map.entry('ф', "f"), Map.entry('х', "h"), Map.entry('ц', "c"), Map.entry('ч', "č"),
            Map.entry('џ', "dž"), Map.entry('ш', "š"));

    private SerbianTransliteration() {
    }

    public static String toLatin(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            char lower = Character.toLowerCase(current);
            String latin = LOWER.get(lower);
            if (latin == null) {
                result.append(current);
                continue;
            }
            if (current == lower) {
                result.append(latin);
            } else if (latin.length() == 1 || nextIsUpper(text, i)) {
                result.append(latin.toUpperCase());
            } else {
                result.append(Character.toUpperCase(latin.charAt(0))).append(latin.substring(1));
            }
        }
        return result.toString();
    }

    private static boolean nextIsUpper(String text, int index) {
        return index + 1 < text.length()
                && Character.isLetter(text.charAt(index + 1))
                && Character.isUpperCase(text.charAt(index + 1));
    }
}
