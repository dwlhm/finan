package com.dwlhm.finan.util.math;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExpressionEvaluatorTest {

    @Test
    public void evaluate_simple_addition() {
        assertEquals(175_000L, ExpressionEvaluator.evaluate("150000+25000"));
    }

    @Test
    public void evaluate_simple_subtraction() {
        assertEquals(125_000L, ExpressionEvaluator.evaluate("150000-25000"));
    }

    @Test
    public void evaluate_simple_multiplication() {
        assertEquals(300_000L, ExpressionEvaluator.evaluate("100000*3"));
    }

    @Test
    public void evaluate_simple_division() {
        assertEquals(75_000L, ExpressionEvaluator.evaluate("150000/2"));
    }

    @Test
    public void evaluate_precedence_multiply_before_add() {
        assertEquals(250_000L, ExpressionEvaluator.evaluate("100000+50000*3"));
    }

    @Test
    public void evaluate_precedence_divide_before_subtract() {
        assertEquals(80_000L, ExpressionEvaluator.evaluate("100000-60000/3"));
    }

    @Test
    public void evaluate_left_to_right_same_precedence() {
        assertEquals(100_000L, ExpressionEvaluator.evaluate("200000-50000-50000"));
    }

    @Test
    public void evaluate_with_display_operators() {
        assertEquals(175_000L, ExpressionEvaluator.evaluate("150000+25000"));
    }

    @Test
    public void evaluate_with_minus_sign() {
        assertEquals(125_000L, ExpressionEvaluator.evaluate("150000−25000"));
    }

    @Test
    public void evaluate_negative_first_operand() {
        assertEquals(50_000L, ExpressionEvaluator.evaluate("-100000+150000"));
    }

    @Test
    public void handle_spaces() {
        assertEquals(100_000L, ExpressionEvaluator.evaluate("50000 + 50000"));
    }

    @Test
    public void evaluate_single_number() {
        assertEquals(50_000L, ExpressionEvaluator.evaluate("50000"));
    }

    @Test
    public void evaluate_zero_addition() {
        assertEquals(100_000L, ExpressionEvaluator.evaluate("100000+0"));
    }

    @Test(expected = ArithmeticException.class)
    public void evaluate_division_by_zero() {
        ExpressionEvaluator.evaluate("100000/0");
    }

    @Test
    public void evaluate_decimal_division_truncates() {
        assertEquals(33_333L, ExpressionEvaluator.evaluate("100000/3"));
    }

    @Test
    public void evaluate_chain_addition() {
        assertEquals(300_000L, ExpressionEvaluator.evaluate("100000+100000+100000"));
    }

    @Test
    public void evaluate_chain_mixed() {
        assertEquals(245_000L, ExpressionEvaluator.evaluate("100000+50000*3-10000/2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void evaluate_empty_expression() {
        ExpressionEvaluator.evaluate("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void evaluate_null_expression() {
        ExpressionEvaluator.evaluate(null);
    }

    @Test
    public void isOperator_returns_true() {
        assertEquals(true, ExpressionEvaluator.isOperator('+'));
        assertEquals(true, ExpressionEvaluator.isOperator('-'));
        assertEquals(true, ExpressionEvaluator.isOperator('*'));
        assertEquals(true, ExpressionEvaluator.isOperator('/'));
        assertEquals(true, ExpressionEvaluator.isOperator('×'));
        assertEquals(true, ExpressionEvaluator.isOperator('÷'));
        assertEquals(true, ExpressionEvaluator.isOperator('−'));
    }

    @Test
    public void isOperator_returns_false() {
        assertEquals(false, ExpressionEvaluator.isOperator('5'));
        assertEquals(false, ExpressionEvaluator.isOperator('a'));
        assertEquals(false, ExpressionEvaluator.isOperator('.'));
    }

    @Test
    public void tokenize_simple() {
        assertEquals(java.util.List.of("150000", "+", "25000"),
                ExpressionEvaluator.tokenize("150000+25000"));
    }

    @Test
    public void tokenize_with_negative() {
        assertEquals(java.util.List.of("-100000", "+", "150000"),
                ExpressionEvaluator.tokenize("-100000+150000"));
    }
}
