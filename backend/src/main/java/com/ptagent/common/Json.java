package com.ptagent.common;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private Json() {
    }

    public static Object parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new Parser(value).parse();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String value) {
        Object parsed = parse(value);
        if (parsed instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return new LinkedHashMap<>();
    }

    public static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out);
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }

    public static String str(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    public static long longValue(Map<String, Object> map, String key, long fallback) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null && !String.valueOf(value).isBlank()) {
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public static double doubleValue(Map<String, Object> map, String key, double fallback) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null && !String.valueOf(value).isBlank()) {
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public static boolean bool(Map<String, Object> map, String key, boolean fallback) {
        Object value = map.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value != null && !String.valueOf(value).isBlank()) {
            return Boolean.parseBoolean(String.valueOf(value));
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    public static List<String> stringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        List<String> result = new ArrayList<>();
        if (value instanceof Iterable<?> items) {
            for (Object item : items) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    result.add(String.valueOf(item));
                }
            }
        } else if (value instanceof String text && !text.isBlank()) {
            for (String item : text.split(",")) {
                if (!item.isBlank()) {
                    result.add(item.trim());
                }
            }
        } else if (value instanceof List<?>) {
            for (Object item : (List<Object>) value) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
        }
        return result;
    }

    private static void write(Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String text) {
            out.append('"').append(escape(text)).append('"');
        } else if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                out.append('"').append(escape(String.valueOf(entry.getKey()))).append('"').append(':');
                write(entry.getValue(), out);
            }
            out.append('}');
        } else if (value instanceof Iterable<?> items) {
            out.append('[');
            boolean first = true;
            for (Object item : items) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                write(item, out);
            }
            out.append(']');
        } else if (value.getClass().isArray()) {
            out.append('[');
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    out.append(',');
                }
                write(Array.get(value, i), out);
            }
            out.append(']');
        } else {
            out.append('"').append(escape(String.valueOf(value))).append('"');
        }
    }

    private static String escape(String text) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static final class Parser {
        private final String input;
        private int index;

        private Parser(String input) {
            this.input = input;
        }

        private Object parse() {
            Object value = parseValue();
            skipWhitespace();
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= input.length()) {
                return null;
            }
            char c = input.charAt(index);
            if (c == '"') {
                return parseString();
            }
            if (c == '{') {
                return parseObject();
            }
            if (c == '[') {
                return parseArray();
            }
            if (c == 't') {
                index += 4;
                return true;
            }
            if (c == 'f') {
                index += 5;
                return false;
            }
            if (c == 'n') {
                index += 4;
                return null;
            }
            return parseNumber();
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            index++;
            skipWhitespace();
            if (peek('}')) {
                index++;
                return map;
            }
            while (index < input.length()) {
                String key = parseString();
                skipWhitespace();
                if (peek(':')) {
                    index++;
                }
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (peek(',')) {
                    index++;
                    skipWhitespace();
                    continue;
                }
                if (peek('}')) {
                    index++;
                    break;
                }
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            index++;
            skipWhitespace();
            if (peek(']')) {
                index++;
                return list;
            }
            while (index < input.length()) {
                list.add(parseValue());
                skipWhitespace();
                if (peek(',')) {
                    index++;
                    continue;
                }
                if (peek(']')) {
                    index++;
                    break;
                }
            }
            return list;
        }

        private String parseString() {
            StringBuilder value = new StringBuilder();
            if (peek('"')) {
                index++;
            }
            while (index < input.length()) {
                char c = input.charAt(index++);
                if (c == '"') {
                    break;
                }
                if (c == '\\' && index < input.length()) {
                    char escaped = input.charAt(index++);
                    switch (escaped) {
                        case '"' -> value.append('"');
                        case '\\' -> value.append('\\');
                        case '/' -> value.append('/');
                        case 'b' -> value.append('\b');
                        case 'f' -> value.append('\f');
                        case 'n' -> value.append('\n');
                        case 'r' -> value.append('\r');
                        case 't' -> value.append('\t');
                        case 'u' -> {
                            String hex = input.substring(index, Math.min(index + 4, input.length()));
                            value.append((char) Integer.parseInt(hex, 16));
                            index += 4;
                        }
                        default -> value.append(escaped);
                    }
                } else {
                    value.append(c);
                }
            }
            return value.toString();
        }

        private Number parseNumber() {
            int start = index;
            while (index < input.length()) {
                char c = input.charAt(index);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                    index++;
                } else {
                    break;
                }
            }
            String token = input.substring(start, index);
            if (token.contains(".") || token.contains("e") || token.contains("E")) {
                return Double.parseDouble(token);
            }
            return Long.parseLong(token);
        }

        private boolean peek(char c) {
            return index < input.length() && input.charAt(index) == c;
        }

        private void skipWhitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }
    }
}
