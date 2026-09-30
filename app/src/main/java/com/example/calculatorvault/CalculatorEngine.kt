package com.example.calculatorvault

import java.util.Locale

/**
 * Plain four-function calculator logic (chained, like a typical phone
 * calculator - not a full expression parser). Deliberately has ZERO
 * knowledge of the vault, PIN, or security layer: MainActivity is the only
 * place that decides "this looks like a PIN attempt" by inspecting
 * [wasPureNumberEntry] before calling [equals].
 */
class CalculatorEngine {

    private var accumulator: Double = 0.0
    private var pendingOperator: Char? = null
    private var currentInput: StringBuilder = StringBuilder("0")
    private var overwriteOnNextDigit = true
    private var operatorUsedSinceClear = false

    /** True if the user has typed only digits/decimal point since the last AC - no operator, no "=". */
    fun wasPureNumberEntry(): Boolean = !operatorUsedSinceClear

    fun currentText(): String = currentInput.toString()

    fun expressionPreview(): String {
        val op = pendingOperator ?: return ""
        return "${trimNumber(accumulator)} $op"
    }

    fun inputDigit(digit: Char) {
        if (overwriteOnNextDigit) {
            currentInput = StringBuilder(if (digit == '0') "0" else digit.toString())
            overwriteOnNextDigit = (digit == '0')
        } else {
            if (currentInput.toString() == "0") currentInput = StringBuilder()
            if (currentInput.length < MAX_DIGITS) currentInput.append(digit)
        }
    }

    fun inputDot() {
        if (overwriteOnNextDigit) {
            currentInput = StringBuilder("0.")
            overwriteOnNextDigit = false
            return
        }
        if (!currentInput.contains('.')) currentInput.append('.')
    }

    fun inputOperator(op: Char) {
        val currentValue = currentInput.toString().toDoubleOrNull() ?: 0.0
        if (pendingOperator != null && !overwriteOnNextDigit) {
            accumulator = applyOperator(accumulator, currentValue, pendingOperator!!)
            currentInput = StringBuilder(trimNumber(accumulator))
        } else {
            accumulator = currentValue
        }
        pendingOperator = op
        operatorUsedSinceClear = true
        overwriteOnNextDigit = true
    }

    fun inputPercent() {
        val value = currentInput.toString().toDoubleOrNull() ?: return
        val result = if (pendingOperator != null) accumulator * (value / 100.0) else value / 100.0
        currentInput = StringBuilder(trimNumber(result))
        overwriteOnNextDigit = true
    }

    fun inputPlusMinus() {
        val value = currentInput.toString().toDoubleOrNull() ?: return
        currentInput = StringBuilder(trimNumber(-value))
    }

    fun backspace() {
        if (overwriteOnNextDigit) return
        if (currentInput.isNotEmpty()) currentInput.deleteCharAt(currentInput.length - 1)
        if (currentInput.isEmpty() || currentInput.toString() == "-") {
            currentInput = StringBuilder("0")
            overwriteOnNextDigit = true
        }
    }

    /** Returns the formatted result string. */
    fun equals(): String {
        val currentValue = currentInput.toString().toDoubleOrNull() ?: 0.0
        val result = if (pendingOperator != null) {
            applyOperator(accumulator, currentValue, pendingOperator!!)
        } else {
            currentValue
        }
        currentInput = StringBuilder(trimNumber(result))
        accumulator = result
        pendingOperator = null
        overwriteOnNextDigit = true
        operatorUsedSinceClear = false
        return currentInput.toString()
    }

    fun clear() {
        accumulator = 0.0
        pendingOperator = null
        currentInput = StringBuilder("0")
        overwriteOnNextDigit = true
        operatorUsedSinceClear = false
    }

    private fun applyOperator(a: Double, b: Double, op: Char): Double = when (op) {
        '+' -> a + b
        '-' -> a - b
        '×' -> a * b
        '÷' -> if (b == 0.0) Double.NaN else a / b
        else -> b
    }

    private fun trimNumber(value: Double): String {
        if (value.isNaN()) return "Error"
        if (value == value.toLong().toDouble() && kotlin.math.abs(value) < 1e15) {
            return value.toLong().toString()
        }
        return String.format(Locale.US, "%.8f", value).trimEnd('0').trimEnd('.')
    }

    companion object {
        private const val MAX_DIGITS = 15
    }
}
