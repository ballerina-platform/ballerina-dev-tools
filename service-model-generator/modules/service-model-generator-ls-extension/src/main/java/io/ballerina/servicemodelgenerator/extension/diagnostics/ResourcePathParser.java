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

/**
 * Parser for resource paths in Ballerina.
 *
 * @since 2.3.0
 */
public class ResourcePathParser {

    /**
     * Splits the input string into segments based on the '/' character.
     *
     * @param input
     * @return
     */
    public static ParseResult parseResourcePath(String input) {
        ParseResult result = new ParseResult();

        if (input == null || input.isEmpty()) {
            result.addError(new ParseError(0, "path cannot be empty"));
            result.setValid(false);
            return result;
        }

        if (input.equals(".")) {
            result.addSegment(new DotSegment(0, 0));
            result.setValid(true);
            return result;
        }

        if (input.startsWith("/") || input.startsWith("\\/")) {
            result.addError(new ParseError(0, "path cannot start with slash"));
            result.setValid(false);
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

        boolean escaped = false;
        while (current < input.length()) {
            if (input.charAt(current) == '/') {
                if (start != current) {
                    String value = !escaped ? input.substring(start, current) : input.substring(start, current - 1);
                    segments.add(new SegmentPart(value, start, current - 1));
                } else {
                    segments.add(new SegmentPart("", start, current));
                }
                start = current + 1;
            }

            escaped = input.charAt(current) == '\\';
            current++;
        }

        if (start < current) {
            String value = input.substring(start, current);
            segments.add(new SegmentPart(value, start, current - 1));
        }

        return segments;
    }

    private static void processSegment(SegmentPart segment, ParseResult result) {
        if (segment.value().isEmpty())  {
            result.addSegment(new ValueSegment("", segment.start(), segment.end()));
        } else {
            result.addSegment(new ValueSegment(segment.value(), segment.start(), segment.end()));
        }
    }

    private static void processParam(SegmentPart segment, ParseResult result) {
        String content = segment.value().substring(1, segment.value().length() - 1);
        List<String> segments = splitSegmentParamNameParts(content.trim());
        if (segments.isEmpty()) {
            result.addError(new ParseError(segment.start(), "Empty parameter"));
            return;
        }

        int count = segments.size();
        String firstSegment = segments.getFirst();
        boolean isRest = firstSegment.endsWith("...");
        if (count == 1) {
            if (isRest) { // [T...]
                result.addSegment(new RestParamSegment(Collections.emptyList(),
                        firstSegment.substring(0, firstSegment.length() - 3), null,
                        segment.start(), segment.end()));
                return;
            }
            result.addSegment(new RestParamSegment(Collections.emptyList(),
                    firstSegment, null, segment.start(), segment.end()));
            return;
        }

        if (count == 2) {
            String secondSegment = segments.get(1);
            if (isRest) {
                result.addSegment(new RestParamSegment(Collections.emptyList(),
                        firstSegment.substring(0, firstSegment.length() - 3), segments.get(1),
                        segment.start(), segment.end()));
                return;
            }
            if (!secondSegment.equals("...") && secondSegment.startsWith("...")) { // [T ...t]
                result.addSegment(new RestParamSegment(Collections.emptyList(),
                        firstSegment, secondSegment.substring(3), segment.start(), segment.end()));
                return;
            }
            result.addSegment(new ParamSegment(Segment.Type.PARAM, Collections.emptyList(),
                    firstSegment, segments.get(1), segment.start(), segment.end()));
            return;
        }

        if (count == 3) {
            String secondSegment = segments.get(1);
            if (secondSegment.equals("...")) { // [T ... t]
                result.addSegment(new RestParamSegment(Collections.emptyList(),
                        segments.get(0), segments.get(2), segment.start(), segment.end()));
                return;
            }
            return;
        }

        result.addError(new ParseError(segment.start(), "invalid path parameter: " + segment.value()));
    }


    private static List<String> splitSegmentParamNameParts(String input) {
        List<String> segments = new ArrayList<>();
        int start = 0;
        int current = 0;

        boolean escaped = false;
        while (current < input.length()) {
            if (!escaped && input.charAt(current) == ' ') {
                if (start != current) {
                    String value = input.substring(start, current);
                    segments.add(value);
                } else {
                    segments.add("");
                }
                start = current + 1;
            }

            escaped = input.charAt(current) == '\\';
            current++;
        }

        if (start < current) {
            String value = input.substring(start, current);
            segments.add(value);
        }

        return segments;
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

    public abstract static class Segment {
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

        public ParamSegment(Type type, List<String> annots, String typeDescriptor,
                            String paramName, int start, int end) {
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

    public static class RestParamSegment extends ParamSegment {

        public RestParamSegment(List<String> annots, String typeDescriptor,
                                String paramName, int start, int end) {
            super(Type.REST_PARAM, annots, typeDescriptor, paramName, start, end);
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
