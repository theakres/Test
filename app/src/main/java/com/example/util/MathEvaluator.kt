package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.*

object MathEvaluator {

    /**
     * Evaluates a mathematical expression string and returns the result as a formatted String.
     * Supports basic operators: +, -, ×, ÷, %
     * Supports scientific functions: sin, cos, tan, ln, log, √, ^, π, e, (, )
     */
    fun evaluate(expression: String): String {
        if (expression.isBlank()) return ""
        
        try {
            val normalized = preprocess(expression)
            val resultValue = parseExpression(normalized)
            
            if (resultValue.isNaN()) return "Error"
            if (resultValue.isInfinite()) return "Error: Infinite"
            
            return formatResult(resultValue)
        } catch (e: ArithmeticException) {
            return "Error: " + (e.message ?: "Division by zero")
        } catch (e: Exception) {
            return "Error"
        }
    }

    private fun preprocess(expr: String): String {
        val s = expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace(" ", "")

        val result = StringBuilder()
        for (i in s.indices) {
            val curr = s[i]
            result.append(curr)
            if (i < s.length - 1) {
                val next = s[i + 1]
                val isCurrDigitOrRightBracketOrConst = curr.isDigit() || curr == ')' || curr == 'π' || curr == 'e'
                val isNextLeftBracketOrConstOrFunc = next == '(' || next == 'π' || next == 'e' || next == '√' || 
                        (next in 'a'..'z')
                
                if (isCurrDigitOrRightBracketOrConst && isNextLeftBracketOrConstOrFunc) {
                    result.append('*')
                }
            }
        }
        return result.toString()
    }

    private fun parseExpression(str: String): Double {
        return ExpressionParser(str).parse()
    }

    private fun formatResult(value: Double): String {
        if (value == value.toLong().toDouble()) {
            return value.toLong().toString()
        }
        // Format to 10 decimal places, stripping trailing zeros
        val bd = BigDecimal(value.toString())
        val scaled = bd.setScale(10, RoundingMode.HALF_UP).stripTrailingZeros()
        return scaled.toPlainString()
    }

    private class ExpressionParser(val str: String) {
        var pos = -1
        var ch = 0

        fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
            return x
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+'.code)) x += parseTerm() // addition
                else if (eat('-'.code)) x -= parseTerm() // subtraction
                else break
            }
            return x
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) x *= parseFactor() // multiplication
                else if (eat('/'.code)) {
                    val next = parseFactor()
                    if (next == 0.0) throw ArithmeticException("Division by zero")
                    x /= next // division
                }
                else if (eat('%'.code)) {
                    val next = parseFactor()
                    x %= next
                }
                else break
            }
            return x
        }

        fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor() // unary plus
            if (eat('-'.code)) return -parseFactor() // unary minus

            var x: Double
            val startPos = this.pos
            if (eat('('.code)) { // parentheses
                x = parseExpression()
                eat(')'.code)
            } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) { // numbers
                while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                x = str.substring(startPos, this.pos).toDouble()
            } else if (ch >= 'a'.code && ch <= 'z'.code || ch == '√'.code || ch == 'π'.code) { // functions/constants
                while (ch >= 'a'.code && ch <= 'z'.code || ch == '√'.code || ch == 'π'.code) nextChar()
                val func = str.substring(startPos, this.pos)
                if (func == "π") {
                    x = Math.PI
                } else if (func == "e") {
                    x = Math.E
                } else {
                    val arg = parseFactor()
                    x = when (func) {
                        "sqrt", "√" -> sqrt(arg)
                        "sin" -> sin(Math.toRadians(arg))
                        "cos" -> cos(Math.toRadians(arg))
                        "tan" -> tan(Math.toRadians(arg))
                        "log" -> log10(arg)
                        "ln" -> ln(arg)
                        else -> throw RuntimeException("Unknown function: $func")
                    }
                }
            } else {
                throw RuntimeException("Unexpected token: " + ch.toChar())
            }

            if (eat('^'.code)) x = x.pow(parseFactor()) // exponentiation

            return x
        }
    }
}
