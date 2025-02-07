/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.flowmodelgenerator.core.converters.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility functions for record conversion.
 *
 * @since 2.0.0
 */
public class Utils {

    private static final String UNICODE_REGEX = "\\\\(\\\\*)u\\{([a-fA-F0-9]+)\\}";
    public static final Pattern UNICODE_PATTERN = Pattern.compile(UNICODE_REGEX);
    private static final Pattern UNESCAPED_SPECIAL_CHAR_SET = Pattern.compile("([$&+,:;=\\?@#\\\\|/'\\ \\[\\}\\]<\\>" +
            ".\"^*{}~`()%!-])");

    /**
     * Replace the unicode patterns in identifiers into respective unicode characters.
     *
     * @param identifier         identifier string
     * @return modified identifier with unicode character
     */
    public static String unescapeUnicodeCodepoints(String identifier) {
        Matcher matcher = UNICODE_PATTERN.matcher(identifier);
        StringBuilder buffer = new StringBuilder(identifier.length());
        while (matcher.find()) {
            String leadingSlashes = matcher.group(1);
            if (isEscapedNumericEscape(leadingSlashes)) {
                // e.g. \\u{61}, \\\\u{61}
                continue;
            }

            int codePoint = Integer.parseInt(matcher.group(2), 16);
            char[] chars = Character.toChars(codePoint);
            String ch = String.valueOf(chars);

            if (ch.equals("\\")) {
                // Ballerina string unescaping is done in two stages.
                // 1. unicode code point unescaping (doing separately as [2] does not support code points > 0xFFFF)
                // 2. java unescaping
                // Replacing unicode code point of backslash at [1] would compromise [2]. Therefore, special case it.
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(leadingSlashes + "\\u005C"));
            } else {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(leadingSlashes + ch));
            }
        }
        matcher.appendTail(buffer);
        return String.valueOf(buffer);
    }

    /**
     * Escape the special characters in an identifier with a preceding `\`.
     *
     * @param identifier encoded identifier string
     * @return decoded identifier
     */
    public static String escapeSpecialCharacters(String identifier) {
        return UNESCAPED_SPECIAL_CHAR_SET.matcher(identifier).replaceAll("\\\\$1");
    }

    /**
     * Returns whether the <a href="https://ballerina.io/ballerina-spec/spec.html#NumericEscape">NumericEscape</a>
     * is escaped, based on no. of leading backslashes.
     *
     * @param leadingSlashes preceding backslashes of the numeric escape.
     *                       e.g. {@code \\u{61}} has 1 leading backslash.
     * @return {@code true} if numeric escape is escaped, {@code false} otherwise.
     */
    public static boolean isEscapedNumericEscape(String leadingSlashes) {
        return !isEven(leadingSlashes.length());
    }

    private static boolean isEven(int n) {
        // (n & 1) is 0 when n is even.
        return (n & 1) == 0;
    }
}
