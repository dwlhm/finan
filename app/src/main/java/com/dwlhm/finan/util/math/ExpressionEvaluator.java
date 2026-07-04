package com.dwlhm.finan.util.math;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

public final class ExpressionEvaluator {

    private static final Map<String, Integer> PRECEDENCE = Map.of(
            "+", 1, "-", 1, "*", 2, "/", 2
    );

    private ExpressionEvaluator() {
    }

    public static long evaluate(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Expression is empty");
        }

        List<String> tokens = tokenize(expression);
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("No valid tokens in expression: " + expression);
        }

        List<String> rpn = toRpn(tokens);
        return evaluateRpn(rpn);
    }

    public static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '×' || c == '÷' || c == '−';
    }

    static List<String> tokenize(String expr) {
        String s = expr
                .replace('×', '*')
                .replace('÷', '/')
                .replace('−', '-')
                .replace(" ", "");

        List<String> tokens = new ArrayList<>();
        StringBuilder numBuf = new StringBuilder();
        boolean lastWasOperator = true;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                numBuf.append(c);
                lastWasOperator = false;
            } else if (c == '+' || c == '-' || c == '*' || c == '/') {
                if (c == '-' && lastWasOperator) {
                    numBuf.append('-');
                    continue;
                }
                if (numBuf.length() > 0) {
                    tokens.add(numBuf.toString());
                    numBuf.setLength(0);
                }
                tokens.add(String.valueOf(c));
                lastWasOperator = true;
            }
        }
        if (numBuf.length() > 0) {
            tokens.add(numBuf.toString());
        }

        return tokens;
    }

    static List<String> toRpn(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Deque<String> stack = new ArrayDeque<>();

        for (String token : tokens) {
            if (isNumber(token)) {
                output.add(token);
            } else if (PRECEDENCE.containsKey(token)) {
                while (!stack.isEmpty() && PRECEDENCE.containsKey(stack.peek())
                        && PRECEDENCE.get(stack.peek()) >= PRECEDENCE.get(token)) {
                    output.add(stack.pop());
                }
                stack.push(token);
            }
        }
        while (!stack.isEmpty()) {
            output.add(stack.pop());
        }

        return output;
    }

    static long evaluateRpn(List<String> rpn) {
        Deque<Long> stack = new ArrayDeque<>();

        for (String token : rpn) {
            if (isNumber(token)) {
                if (token.contains(".")) {
                    double val = Double.parseDouble(token);
                    stack.push((long) val);
                } else {
                    stack.push(Long.parseLong(token));
                }
            } else {
                long b = stack.pop();
                long a = stack.pop();
                switch (token) {
                    case "+":
                        stack.push(a + b);
                        break;
                    case "-":
                        stack.push(a - b);
                        break;
                    case "*":
                        stack.push(a * b);
                        break;
                    case "/":
                        if (b == 0) throw new ArithmeticException("Division by zero");
                        stack.push(a / b);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown operator: " + token);
                }
            }
        }

        if (stack.size() != 1) {
            throw new IllegalStateException("Invalid expression");
        }
        return stack.pop();
    }

    private static boolean isNumber(String s) {
        if (s == null || s.isEmpty()) return false;
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
