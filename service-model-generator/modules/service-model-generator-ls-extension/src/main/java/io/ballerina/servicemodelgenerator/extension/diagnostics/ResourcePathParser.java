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

package io.ballerina.servicemodelgenerator.extension.diagnostics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parser for resource paths in Ballerina.
 *
 * @since 2.3.0
 */
public class ResourcePathParser {

    public static ParseResult parseResourcePath(String input) {
        ParseResult result = new ParseResult();

        if (input == null || input.isEmpty()) {
            result.addError(new ParseError(0, "path cannot be empty"));
            result.setValid(false);
            return result;
        }

        if (input.equals(".")) {
            result.addSegment(new DotSegment(0, 0));
            boolean valid = result.getErrors().isEmpty();
            result.setValid(valid);
            if (!valid) {
                result.addError(new ParseError(0, "cannot have characters after dot (.)"));
            }
            return result;
        }

        List<SegmentPart> segments = splitSegments(input);
        for (SegmentPart segment : segments) {
            String value = segment.value();
            if (value.startsWith("[") && value.endsWith("]")) {
                processParam(segment, result);
            } else {
                processSegment(segment, result);
            }
        }

        result.setValid(result.getErrors().isEmpty());
        return result;
    }

    private static List<SegmentPart> splitSegments(String input) {
        List<SegmentPart> segments = new ArrayList<>();
        int start = 0;
        int current = 0;

        while (current < input.length()) {
            if (input.charAt(current) == '/') {
                if (start != current) {
                    String value = input.substring(start, current);
                    segments.add(new SegmentPart(value, start, current - 1));
                } else {
                    segments.add(new SegmentPart("", start, current));
                }
                start = current + 1;
            }
            current++;
        }

        if (start < current) {
            String value = input.substring(start, current);
            segments.add(new SegmentPart(value, start, current - 1));
        }

        return segments;
    }

    private static void processSegment(SegmentPart segment, ParseResult result) {
        TokenizeResult tokenResult = tokenize(segment.value(), segment.start());
        result.addErrors(tokenResult.errors());

        if (tokenResult.tokens().isEmpty() && segment.value().isEmpty())  {
            result.addSegment(new ValueSegment("", segment.start(), segment.end()));
            return;
        } else if (tokenResult.errors().isEmpty() && tokenResult.tokens().size() != 1) {
            result.addError(new ParseError(segment.start(), "Invalid segment: " + segment.value()));
            return;
        }

        result.addSegment(new ValueSegment(segment.value(), segment.start(), segment.end()));
    }

    private static void processParam(SegmentPart segment, ParseResult result) {
        String content = segment.value().substring(1, segment.value().length() - 1);

        if (isConstantLiteral(content)) {
            result.addSegment(new ParamSegment(Segment.Type.CONST_PARAM, Collections.emptyList(), content, content, segment.start(), segment.end()));
            return;
        }

        TokenizeResult tokenResult = tokenize(content, segment.start() + 1);
        result.addErrors(tokenResult.errors());

        boolean hasRest = tokenResult.tokens().stream().anyMatch(t -> t.value().equals("..."));
        if (hasRest) {
            handleRestParam(tokenResult.tokens(), segment, result);
        } else {
            handleRegularParam(tokenResult.tokens(), segment, result);
        }
    }

    private static void handleRegularParam(List<Token> tokens, SegmentPart segment, ParseResult result) {
        if (tokens.isEmpty()) {
            result.addError(new ParseError(segment.start() + 2, "Empty parameter"));
            return;
        }

        Token lastToken = tokens.get(tokens.size() - 1);
        String paramName = validateParamName(lastToken, result);
        List<Token> remaining = tokens.subList(0, tokens.size() - 1);

        if (remaining.isEmpty()) {
            result.addError(new ParseError(segment.start() + 2, "Missing type descriptor"));
            return;
        }

        String typeDescriptor = remaining.get(remaining.size() - 1).value();
        List<String> annots = remaining.subList(0, remaining.size() - 1).stream()
                .map(Token::value)
                .collect(Collectors.toList());

        result.addSegment(new ParamSegment(Segment.Type.PARAM, annots, typeDescriptor, paramName, segment.start(), segment.end()));
    }

    private static void handleRestParam(List<Token> tokens, SegmentPart segment, ParseResult result) {
        int dotIndex = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).value().equals("...")) {
                dotIndex = i;
                break;
            }
        }
        if (dotIndex == -1) return;

        List<Token> beforeDot = tokens.subList(0, dotIndex);
        List<Token> afterDot = tokens.subList(dotIndex + 1, tokens.size());

        if (beforeDot.isEmpty()) {
            result.addError(new ParseError(segment.start() + 2, "Missing type descriptor in rest parameter"));
        }

        if (afterDot.size() > 1) {
            result.addError(new ParseError(afterDot.get(1).start(), "Extra tokens after rest parameter"));
        }

        String typeDescriptor = beforeDot.isEmpty() ? "" : beforeDot.get(beforeDot.size() - 1).value();
        List<String> annots = beforeDot.subList(0, beforeDot.size() - 1).stream()
                .map(Token::value)
                .collect(Collectors.toList());

        String paramName = afterDot.isEmpty() ? null : validateParamName(afterDot.get(0), result);

        result.addSegment(new ParamSegment(Segment.Type.REST_PARAM, annots, typeDescriptor, paramName, segment.start(), segment.end()));
    }

    private static String validateParamName(Token token, ParseResult result) {
        if (token == null) {
            return null;
        }
        String value = token.value();
        if (!isValidIdentifier(value)) {
            result.addError(new ParseError(token.start(), "Invalid parameter name: " + value));
            return null;
        }
        return value;
    }

    private static boolean isConstantLiteral(String value) {
        return value.startsWith("\"") && value.endsWith("\"");
    }

    private static TokenizeResult tokenize(String content, int offset) {
        List<Token> tokens = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();
        int pos = 0;

        while (pos < content.length()) {
            char c = content.charAt(pos);
            int start = pos + offset;

            if (Character.isWhitespace(c)) {
                pos++;
                continue;
            }

            if (c == '\'' || c == '"') {
                QuotedReadResult quotedResult = readQuoted(content, pos, offset);
                pos = quotedResult.newPos();
                errors.addAll(quotedResult.errors());
                if (quotedResult.value() != null) {
                    tokens.add(new Token(quotedResult.value(), start, pos + offset - 1));
                }
            } else if (c == '.' && pos + 2 < content.length() && content.substring(pos, pos + 3).equals("...")) {
                tokens.add(new Token("...", start, start + 2));
                pos += 3;
            } else {
                UnquotedReadResult unquotedResult = readUnquoted(content, pos, offset);
                pos = unquotedResult.newPos();
                errors.addAll(unquotedResult.errors());
                if (unquotedResult.value() != null) {
                    tokens.add(new Token(unquotedResult.value(), start, pos + offset - 1));
                }
            }
        }

        return new TokenizeResult(tokens, errors);
    }

    private static QuotedReadResult readQuoted(String content, int pos, int offset) {
        StringBuilder value = new StringBuilder();
        boolean escape = false;
        char quoteChar = content.charAt(pos);
        value.append(quoteChar);
        pos++;

        List<ParseError> errors = new ArrayList<>();

        while (pos < content.length()) {
            char c = content.charAt(pos);
            if (escape) {
                value.append(c);
                escape = false;
                pos++;
            } else if (c == '\\') {
                escape = true;
                pos++;
            } else if (c == quoteChar) {
                value.append(c);
                pos++;
                return new QuotedReadResult(value.toString(), pos, errors);
            } else {
                value.append(c);
                pos++;
            }
        }

        errors.add(new ParseError(pos + offset, "Unterminated quoted identifier"));
        return new QuotedReadResult(null, pos, errors);
    }

    private static UnquotedReadResult readUnquoted(String content, int pos, int offset) {
        StringBuilder value = new StringBuilder();
        char initialChar = content.charAt(pos);
        List<ParseError> errors = new ArrayList<>();

        if (!isValidInitial(initialChar)) {
            errors.add(new ParseError(pos + offset, "Invalid initial character: " + initialChar));
            return new UnquotedReadResult(null, pos + 1, errors);
        }

        value.append(initialChar);
        pos++;

        while (pos < content.length()) {
            char c = content.charAt(pos);
            if (Character.isWhitespace(c) || c == ']' || c == '[') {
                break;
            }
            if (isValidFollowing(c)) {
                value.append(c);
                pos++;
                continue;
            }
            if (c == '\\') {
                if (pos + 1 < content.length()) {
                    char next = content.charAt(pos + 1);
                    if (next == '-' || next == '\\' || next == '.') {
                        value.append(c).append(next);
                        pos += 2;
                        continue;
                    }
                }
                errors.add(new ParseError(pos + offset, "Backslash is not allowed"));
                return new UnquotedReadResult(null, pos + 1, errors);
            } else if (c == '.') {
                if (pos + 2 < content.length() && content.substring(pos, pos + 3).equals("...")) {
                    value.append("...");
                    pos += 3;
                }
            } else {
                errors.add(new ParseError(pos + offset, "Invalid character: " + c));
                return new UnquotedReadResult(null, pos + 1, errors);
            }
        }

        return new UnquotedReadResult(value.toString(), pos, errors);
    }

    private static boolean isValidInitial(char c) {
        return Character.isLetter(c) || c == '_' || isUnicodeIdentifierChar(c);
    }

    private static boolean isValidFollowing(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || isUnicodeIdentifierChar(c);
    }

    private static boolean isUnicodeIdentifierChar(char c) {
        // Implement actual Unicode check if needed
        return false;
    }

    private static boolean isValidIdentifier(String value) {
        if (value.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            return true;
        }
        if (value.matches("^'[^']*'$")) {
            return true;
        }
        if (value.contains("-") && !value.contains("\\-")) {
            return false;
        }
        return false;
    }

    // Helper Classes
    public static class ParseError {
        private final int position;
        private final String message;

        public ParseError(int position, String message) {
            this.position = position;
            this.message = message;
        }

        public int getPosition() {
            return position;
        }

        public String getMessage() {
            return message;
        }
    }

    public static abstract class Segment {
        public enum Type {
            DOT, SEGMENT, PARAM, REST_PARAM, CONST_PARAM
        }

        private final Type type;
        private final int start;
        private final int end;

        protected Segment(Type type, int start, int end) {
            this.type = type;
            this.start = start;
            this.end = end;
        }

        public Type getType() {
            return type;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }
    }

    public static class DotSegment extends Segment {
        public DotSegment(int start, int end) {
            super(Type.DOT, start, end);
        }
    }

    public static class ValueSegment extends Segment {
        private final String value;

        public ValueSegment(String value, int start, int end) {
            super(Type.SEGMENT, start, end);
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static class ParamSegment extends Segment {
        private final List<String> annots;
        private final String typeDescriptor;
        private final String paramName;

        public ParamSegment(Type type, List<String> annots, String typeDescriptor, String paramName, int start, int end) {
            super(type, start, end);
            this.annots = annots;
            this.typeDescriptor = typeDescriptor;
            this.paramName = paramName;
        }

        public List<String> getAnnots() {
            return annots;
        }

        public String getTypeDescriptor() {
            return typeDescriptor;
        }

        public String getParamName() {
            return paramName;
        }
    }

    public static class ParseResult {
        private boolean valid;
        private final List<ParseError> errors = new ArrayList<>();
        private final List<Segment> segments = new ArrayList<>();

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public List<ParseError> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        public List<Segment> getSegments() {
            return Collections.unmodifiableList(segments);
        }

        public void addError(ParseError error) {
            errors.add(error);
        }

        public void addErrors(List<ParseError> errors) {
            this.errors.addAll(errors);
        }

        public void addSegment(Segment segment) {
            segments.add(segment);
        }
    }

    private record SegmentPart(String value, int start, int end) {
    }

    private record Token(String value, int start, int end) {
    }

    private record TokenizeResult(List<Token> tokens, List<ParseError> errors) {
    }

    private record QuotedReadResult(String value, int newPos, List<ParseError> errors) {
    }

    private record UnquotedReadResult(String value, int newPos, List<ParseError> errors) {
    }
}