package com.example.calculatorvault

import java.util.Locale

class CalculatorEngine {

    private val expr = StringBuilder("0")
    private var justEvaluated = false
    private var degreeMode = true
    private var secondMode = false
    private var lastAnswer = "0"
    private var lastEvaluatedExpression: String? = null

    fun currentText(): String = expr.toString()
    fun lastExpression(): String? = lastEvaluatedExpression
    fun isDegreeMode(): Boolean = degreeMode
    fun isSecondMode(): Boolean = secondMode

    fun toggleDegreeMode() { degreeMode = !degreeMode }
    fun toggleSecondMode() { secondMode = !secondMode }

    fun inputDigit(d: Char) {
        startFreshExpressionIfJustEvaluated()
        if (expr.toString() == "0") expr.setLength(0)
        expr.append(d)
    }

    fun inputDot() {
        startFreshExpressionIfJustEvaluated()
        if (expr.isEmpty()) expr.append("0")
        expr.append('.')
    }

    fun inputParenOpen() {
        startFreshExpressionIfJustEvaluated()
        if (expr.toString() == "0") expr.setLength(0)
        expr.append('(')
    }

    fun inputParenClose() {
        startFreshExpressionIfJustEvaluated()
        expr.append(')')
    }

    fun inputBinaryOperator(symbol: Char) {
        justEvaluated = false
        if (expr.isEmpty()) expr.append("0")
        expr.append(symbol)
    }

    fun inputFunction(token: String) {
        startFreshExpressionIfJustEvaluated()
        if (expr.toString() == "0") expr.setLength(0)
        expr.append(token)
    }

    fun inputConstant(symbol: Char) {
        startFreshExpressionIfJustEvaluated()
        if (expr.toString() == "0") expr.setLength(0)
        expr.append(symbol)
    }

    fun inputFactorial() {
        justEvaluated = false
        expr.append('!')
    }

    fun inputPercent() {
        justEvaluated = false
        expr.append('%')
    }

    fun inputReciprocal() {
        startFreshExpressionIfJustEvaluated()
        val current = if (expr.toString() == "0") "" else expr.toString()
        expr.setLength(0)
        expr.append("1/(").append(current).append(")")
    }

    fun inputPlusMinus() {
        startFreshExpressionIfJustEvaluated()
        val s = expr.toString()
        if (s.startsWith("-")) expr.deleteCharAt(0) else expr.insert(0, "-")
    }

    fun insertLastAnswer() {
        startFreshExpressionIfJustEvaluated()
        if (expr.toString() == "0") expr.setLength(0)
        expr.append(lastAnswer)
    }

    fun backspace() {
        justEvaluated = false
        if (expr.isNotEmpty()) expr.deleteCharAt(expr.length - 1)
        if (expr.isEmpty()) expr.append("0")
    }

    fun clear() {
        expr.setLength(0)
        expr.append("0")
        justEvaluated = false
        lastEvaluatedExpression = null
    }

    fun equals(): String {
        val exprStr = expr.toString()
        return try {
            val value = ExpressionEvaluator(degreeMode).evaluate(exprStr)
            val formatted = formatResult(value)
            lastEvaluatedExpression = exprStr
            lastAnswer = formatted
            expr.setLength(0)
            expr.append(formatted)
            justEvaluated = true
            formatted
        } catch (e: Exception) {
            expr.setLength(0)
            expr.append("Error")
            justEvaluated = true
            "Error"
        }
    }

    private fun startFreshExpressionIfJustEvaluated() {
        if (justEvaluated) {
            expr.setLength(0)
            expr.append("0")
            justEvaluated = false
        }
    }

    private fun formatResult(value: Double): String {
        if (value.isNaN()) return "Error"
        if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
        if (value == value.toLong().toDouble() && kotlin.math.abs(value) < 1e15) {
            return value.toLong().toString()
        }
        return String.format(Locale.US, "%.8f", value).trimEnd('0').trimEnd('.')
    }
}
