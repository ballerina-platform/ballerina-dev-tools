package io.ballerina.servicemodelgenerator.extension.diagnostics;

/**
 * Parser for identifiers in Ballerina.
 *
 */
public class IdentifierValidator {

    private String errorMessage;

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isValidIdentifier(String input) {
        errorMessage = null;
        if (input == null || input.isEmpty()) {
            return false;
        }

        if (input.startsWith("'")) {
            return parseQuotedIdentifier(input);
        } else {
            return parseUnquotedIdentifier(input);
        }
    }

    private boolean parseQuotedIdentifier(String input) {
        if (input.length() < 2 || input.charAt(input.length() - 1) == '\'') {
            return false;
        }

        int index = 1; // Skip opening quote
        int length = input.length() - 1; // Before closing quote
        boolean hasContent = false;

        while (index < length) {
            char c = input.charAt(index);

            if (c == '\\') {
                int escapeLength = parseEscapeSequence(input, index, length);
                if (escapeLength == -1) return false;
                index += escapeLength;
                hasContent = true;
            } else {
                if (!isValidFollowingChar(c)) return false;
                index++;
                hasContent = true;
            }
        }

        return hasContent && index == length;
    }

    private boolean parseUnquotedIdentifier(String input) {
        int index = 0;
        int length = input.length();

        // Process first character
        char firstChar = input.charAt(index);
        if (firstChar == '\\') {
            int escapeLength = parseEscapeSequence(input, index, length);
            if (escapeLength == -1) {
                errorMessage = "invalid escape sequence";
                return false;
            }
            index += escapeLength;
        } else {
            if (!isValidInitialChar(firstChar)) {
                errorMessage = "invalid character: '" + firstChar + "'";
                return false;
            }
            index++;
        }

        // Process remaining characters
        while (index < length) {
            char c = input.charAt(index);

            if (c == '\\') {
                int escapeLength = parseEscapeSequence(input, index, length);
                if (escapeLength == -1) {
                    errorMessage = "invalid escape sequence";
                    return false;
                }
                index += escapeLength;
            } else {
                if (!isValidFollowingChar(c)) {
                    errorMessage = "invalid character: '" + c + "'";
                    return false;
                }
                index++;
            }
        }

        return true;
    }

    private static int parseEscapeSequence(String input, int index, int maxPos) {
        if (index + 1 >= maxPos) return -1;

        char nextChar = input.charAt(index + 1);
        if (nextChar == 'u') {
            return parseUnicodeEscape(input, index, maxPos);
        } else {
            return parseSingleEscape(nextChar) ? 2 : -1;
        }
    }

    private static int parseUnicodeEscape(String input, int index, int maxPos) {
        if (index + 3 >= maxPos || input.charAt(index + 2) != '{') {
            return -1;
        }

        int endIndex = input.indexOf('}', index + 3);
        if (endIndex == -1 || endIndex >= maxPos) {
            return -1;
        }

        String hex = input.substring(index + 3, endIndex);
        if (!isValidHex(hex)) return -1;

        try {
            int codePoint = Integer.parseInt(hex, 16);
            if (codePoint < 0 || codePoint > 0x10FFFF) return -1;
        } catch (NumberFormatException e) {
            return -1;
        }

        return endIndex - index + 1;
    }

    private static boolean parseSingleEscape(char c) {
        return !isAsciiLetter(c) &&
                c != 0x9 &&    // TAB
                c != 0xA &&    // LF
                c != 0xD &&    // CR
                !isUnicodePatternWhitespace(c);
    }

    private static boolean isValidHex(String hex) {
        return hex.chars().allMatch(c ->
                (c >= '0' && c <= '9') ||
                        (c >= 'A' && c <= 'F') ||
                        (c >= 'a' && c <= 'f')
        );
    }

    private static boolean isValidInitialChar(char c) {
        return isAsciiLetter(c) ||
                c == '_' ||
                (c > 0x7F && isValidUnicodeChar(c));
    }

    private static boolean isValidFollowingChar(char c) {
        return isValidInitialChar(c) ||
                (c >= '0' && c <= '9');
    }

    private static boolean isAsciiLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private static boolean isValidUnicodeChar(char c) {
        // Check private use areas
        if ((c >= 0xE000 && c <= 0xF8FF) ||
                (c >= 0xF0000 && c <= 0xFFFFD) ||
                (c >= 0x100000 && c <= 0x10FFFD)) {
            return false;
        }

        // Check whitespace and syntax characters
        return !isUnicodePatternWhitespace(c) &&
                !isUnicodePatternSyntax(c);
    }

    private static boolean isUnicodePatternWhitespace(char c) {
        return c == 0x200E || // LEFT-TO-RIGHT MARK
                c == 0x200F || // RIGHT-TO-LEFT MARK
                c == 0x2028 || // LINE SEPARATOR
                c == 0x2029;   // PARAGRAPH SEPARATOR
    }

    private static boolean isUnicodePatternSyntax(char c) {
        // This should check Unicode's Pattern_Syntax property
        // Implementation requires full Unicode data - this is simplified
        return false;
    }
}
