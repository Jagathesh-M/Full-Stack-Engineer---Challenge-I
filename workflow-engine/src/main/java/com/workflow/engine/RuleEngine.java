package com.workflow.engine;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates rule conditions against execution data.
 * Supports: ==, !=, <, >, <=, >=, &&, ||, contains(field, "value"), startsWith, endsWith, DEFAULT.
 */
@Component
public class RuleEngine {

    private static final Pattern COMPARISON_PATTERN = Pattern.compile(
            "\\s*(\\w+)\\s*(==|!=|<=|>=|<|>)\\s*('([^']*)'|\"([^\"]*)\"|([\\d.]+)|(true|false))\\s*",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CONTAINS_PATTERN = Pattern.compile(
            "contains\\s*\\(\\s*([\\w.]+)\\s*,\\s*[\"']([^\"']*)[\"']\\s*\\)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern STARTS_WITH_PATTERN = Pattern.compile(
            "startsWith\\s*\\(\\s*([\\w.]+)\\s*,\\s*[\"']([^\"']*)[\"']\\s*\\)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ENDS_WITH_PATTERN = Pattern.compile(
            "endsWith\\s*\\(\\s*([\\w.]+)\\s*,\\s*[\"']([^\"']*)[\"']\\s*\\)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Evaluate a condition string against data. "DEFAULT" (case-insensitive) always returns true.
     */
    public boolean evaluate(String condition, Map<String, Object> data) {
        if (condition == null || condition.isBlank()) {
            return false;
        }
        String trimmed = condition.trim();
        if ("DEFAULT".equalsIgnoreCase(trimmed)) {
            return true;
        }
        return evaluateExpression(trimmed, data);
    }

    private boolean evaluateExpression(String expr, Map<String, Object> data) {
        expr = expr.trim();
        if (expr.contains("||")) {
            String[] parts = splitByOperator(expr, "||");
            for (String part : parts) {
                if (evaluateExpression(part.trim(), data)) return true;
            }
            return false;
        }
        if (expr.contains("&&")) {
            String[] parts = splitByOperator(expr, "&&");
            for (String part : parts) {
                if (!evaluateExpression(part.trim(), data)) return false;
            }
            return true;
        }
        // Single comparison or function
        return evaluateSingle(expr, data);
    }

    private String[] splitByOperator(String expr, String op) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i <= expr.length() - op.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(' || c == '\'' || c == '"') {
                if (c == '(') depth++;
                else while (i + 1 < expr.length() && expr.charAt(i) != (c == '\'' ? '\'' : '"')) i++;
            } else if (c == ')') depth--;
            else if (depth == 0 && expr.substring(i, i + op.length()).equals(op)) {
                result.add(expr.substring(start, i));
                start = i + op.length();
                i += op.length() - 1;
            }
        }
        result.add(expr.substring(start));
        return result.toArray(new String[0]);
    }

    private boolean evaluateSingle(String expr, Map<String, Object> data) {
        expr = expr.trim();

        Matcher containsMatcher = CONTAINS_PATTERN.matcher(expr);
        if (containsMatcher.matches()) {
            String field = containsMatcher.group(1);
            String value = containsMatcher.group(2);
            Object fieldVal = getValue(data, field);
            return fieldVal != null && String.valueOf(fieldVal).contains(value);
        }

        Matcher startsMatcher = STARTS_WITH_PATTERN.matcher(expr);
        if (startsMatcher.matches()) {
            String field = startsMatcher.group(1);
            String prefix = startsMatcher.group(2);
            Object fieldVal = getValue(data, field);
            return fieldVal != null && String.valueOf(fieldVal).startsWith(prefix);
        }

        Matcher endsMatcher = ENDS_WITH_PATTERN.matcher(expr);
        if (endsMatcher.matches()) {
            String field = endsMatcher.group(1);
            String suffix = endsMatcher.group(2);
            Object fieldVal = getValue(data, field);
            return fieldVal != null && String.valueOf(fieldVal).endsWith(suffix);
        }

        Matcher compMatcher = COMPARISON_PATTERN.matcher(expr);
        if (compMatcher.matches()) {
            String field = compMatcher.group(1);
            String op = compMatcher.group(2);
            String strVal = compMatcher.group(4);
            if (strVal == null) strVal = compMatcher.group(5);
            if (strVal == null) strVal = compMatcher.group(6);
            if (strVal == null) strVal = compMatcher.group(7);
            Object fieldVal = getValue(data, field);
            return compare(fieldVal, op, strVal);
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private Object getValue(Map<String, Object> data, String key) {
        if (data == null || key == null) return null;
        if (key.contains(".")) {
            String[] parts = key.split("\\.", 2);
            Object nested = data.get(parts[0]);
            if (nested instanceof Map) {
                return getValue((Map<String, Object>) nested, parts[1]);
            }
            return null;
        }
        return data.get(key);
    }

    private boolean compare(Object fieldVal, String op, String strVal) {
        if (fieldVal == null) {
            return "==".equals(op) && (strVal == null || "null".equalsIgnoreCase(strVal))
                    || "!=".equals(op) && strVal != null && !"null".equalsIgnoreCase(strVal);
        }
        if ("true".equalsIgnoreCase(strVal) || "false".equalsIgnoreCase(strVal)) {
            boolean expected = Boolean.parseBoolean(strVal);
            boolean actual = fieldVal instanceof Boolean ? (Boolean) fieldVal : Boolean.parseBoolean(String.valueOf(fieldVal));
            return "==".equals(op) ? actual == expected : actual != expected;
        }
        if (fieldVal instanceof Number || (strVal != null && strVal.matches("-?[\\d.]+"))) {
            double a = fieldVal instanceof Number ? ((Number) fieldVal).doubleValue() : Double.parseDouble(String.valueOf(fieldVal));
            double b = strVal == null ? 0 : Double.parseDouble(strVal);
            return switch (op) {
                case "==" -> Math.abs(a - b) < 1e-9;
                case "!=" -> Math.abs(a - b) >= 1e-9;
                case "<" -> a < b;
                case ">" -> a > b;
                case "<=" -> a <= b;
                case ">=" -> a >= b;
                default -> false;
            };
        }
        String a = String.valueOf(fieldVal);
        String b = strVal == null ? "" : strVal;
        return switch (op) {
            case "==" -> a.equals(b);
            case "!=" -> !a.equals(b);
            default -> false;
        };
    }
}
