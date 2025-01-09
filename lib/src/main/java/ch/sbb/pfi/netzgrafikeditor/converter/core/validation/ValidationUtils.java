package ch.sbb.pfi.netzgrafikeditor.converter.core.validation;

import java.util.regex.Pattern;

class ValidationUtils {

    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^a-zA-Z0-9_\\-\\s]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

    private static final String SPECIAL_CHAR_REPLACEMENT = "";
    private static final String WHITESPACE_REPLACEMENT = "_";
    private static final String DOT_REPLACEMENT = "";

    static boolean containsSpecialCharacter(String input) {
        return SPECIAL_CHAR_PATTERN.matcher(input).find();
    }

    static boolean containsWhitespace(String input) {
        return WHITESPACE_PATTERN.matcher(input).find();
    }

    static boolean containsTrailingWhitespace(String input) {
        return !input.equals(input.strip());
    }

    static String removeDotsReplaceWhitespace(String input) {
        return WHITESPACE_PATTERN.matcher(DOT_PATTERN.matcher(input).replaceAll(DOT_REPLACEMENT))
                .replaceAll(WHITESPACE_REPLACEMENT);
    }

    static String removeSpecialCharacters(String input) {
        return SPECIAL_CHAR_PATTERN.matcher(input).replaceAll(SPECIAL_CHAR_REPLACEMENT);
    }

    static String replaceWhitespace(String input) {
        return WHITESPACE_PATTERN.matcher(input.strip()).replaceAll(WHITESPACE_REPLACEMENT);
    }
}
