package ch.sbb.pfi.netzgrafikeditor.converter.validation;

import java.util.regex.Pattern;

class ValidationUtils {

    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^a-zA-Z0-9_\\-\\s]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private static final String SPECIAL_CHAR_REPLACEMENT = "";
    private static final String WHITESPACE_REPLACEMENT = "_";

    static boolean containsSpecialCharacter(String input) {
        return SPECIAL_CHAR_PATTERN.matcher(input).find();
    }

    static boolean containsWhitespace(String input) {
        return WHITESPACE_PATTERN.matcher(input).find();
    }

    static boolean containsTrailingWhitespace(String input) {
        return !input.equals(input.strip());
    }

    static String removeSpecialCharacters(String input) {
        return SPECIAL_CHAR_PATTERN.matcher(input).replaceAll(SPECIAL_CHAR_REPLACEMENT);
    }

    static String removeWhitespace(String input) {
        return WHITESPACE_PATTERN.matcher(input).replaceAll(WHITESPACE_REPLACEMENT).strip();
    }
}
