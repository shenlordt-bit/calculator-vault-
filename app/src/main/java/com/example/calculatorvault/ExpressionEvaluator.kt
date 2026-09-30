package com.example.calculatorvault

import kotlin.math.PI
import kotlin.math.E
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.tan
import kotlin.math.asin
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.sqrt
import kotlin.math.ln
import kotlin.math.log10

class ExpressionEvaluator(private val degreeMode: Boolean) {

    private lateinit var text: String
    private var pos: Int = 0

    fun evaluate(rawExpression: String): Double {
        text = autoCloseParens(rawExpression.replace(" ", ""))
        pos = 0
        if (text.isEmpty()) return 0.0
        val result = parseExpression()
        if (pos != text.length) {
            throw IllegalArgumentException("Unexpected trailing characters at $pos")
        }
        return result
    }

    private fun autoCloseParens(s: String): String {
        val openCount = s.count { it == '(' } - s.count { it == ')' }
        return if (openCount > 0) s + ")".repeat(openCount) else s
    }

    private fun peek(): Char? = if (pos < text.length) text[pos] else null

    private fun parseExpression(): Double {
        var value = parseTerm()
        while (true) {
            when (peek()) {
                '+' -> { pos++; value += parseTerm() }
                '-', '\u2212' -> { pos++; value -= parseTerm() }
                else -> return value
            }
        }
    }

    private fun parseTerm(): Double {
        var value = parsePower()
        while (true) {
            when (peek()) {
                '×', '*' -> { pos++; value *= parsePower() }
                '÷', '/' -> {
                    pos++
                    val divisor = parsePower()
                    value = if (divisor == 0.0) Double.NaN else value / divisor
                }
                else -> return value
            }
        }
    }

    private fun parsePower(): Double {
        val base = parseUnary()
        if (peek() == '^') {
            pos++
            val exponent = parsePower()
            return base.pow(exponent)
        }
        return base
    }

    private fun parseUnary(): Double {
        if (peek() == '-' || peek() == '\u2212') {
            pos++
            return -parseUnary()
        }
        return parsePostfix()
    }

    private fun parsePostfix(): Double {
        var value = parseAtom()
        while (true) {
            when (peek()) {
                '!' -> { pos++; value = factorial(value) }
                '%' -> { pos++; value /= 100.0 }
                else -> return value
            }
        }
    }

    private fun parseAtom(): Double {
        val c = peek() ?: throw IllegalArgumentException("Unexpected end of expression")
        return when {
            c == '(' -> {
                pos++
                val v = parseExpression()
                consumeClose()
                v
            }
            c == 'π' -> { pos++; PI }
            c.isDigit() || c == '.' -> parseNumber()
            text.startsWith("asin(", pos) -> { pos += 5; val v = parseExpression(); consumeClose(); fromDeg(asin(v)) }
            text.startsWith("acos(", pos) -> { pos += 5; val v = parseExpression(); consumeClose(); fromDeg(acos(v)) }
            text.startsWith("atan(", pos) -> { pos += 5; val v = parseExpression(); consumeClose(); fromDeg(atan(v)) }
            text.startsWith("sin(", pos) -> { pos += 4; val v = parseExpression(); consumeClose(); sin(toRad(v)) }
            text.startsWith("cos(", pos) -> { pos += 4; val v = parseExpression(); consumeClose(); cos(toRad(v)) }
            text.startsWith("tan(", pos) -> { pos += 4; val v = parseExpression(); consumeClose(); tan(toRad(v)) }
            text.startsWith("ln(", pos) -> { pos += 3; val v = parseExpression(); consumeClose(); ln(v) }
            text.startsWith("lg(", pos) -> { pos += 3; val v = parseExpression(); consumeClose(); log10(v) }
            text.startsWith("√(", pos) -> { pos += 2; val v = parseExpression(); consumeClose(); sqrt(v) }
            c == 'e' -> { pos++; E }
            else -> throw IllegalArgumentException("Unexpected character '$c' at $pos")
        }
    }

    private fun consumeClose() {
        if (peek() == ')') pos++
    }

    private fun parseNumber(): Double {
        val start = pos
        while (pos < text.length && (text[pos].isDigit() || text[pos] == '.')) pos++
        return text.substring(start, pos).toDoubleOrNull()
            ?: throw IllegalArgumentException("Invalid number at $start")
    }

    private fun toRad(v: Double): Double = if (degreeMode) Math.toRadians(v) else v
    private fun fromDeg(v: Double): Double = if (degreeMode) Math.toDegrees(v) else v

    private fun factorial(v: Double): Double {
        if (v < 0 || v != floor(v) || v > 170) return Double.NaN
        var result = 1.0
        var i = 2
        while (i <= v.toInt()) { result *= i; i++ }
        return result
    }
}
