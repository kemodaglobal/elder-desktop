package com.elderdesktop.calculator.util

object CalculatorUtils {
    fun evaluate(expression: String): Double {
        return try {
            val tokens = expression.replace("−", "-").replace("×", "*").replace("÷", "/")
            
            val operators = mutableListOf<Char>()
            val values = mutableListOf<Double>()
            
            var i = 0
            while (i < tokens.length) {
                if (tokens[i] == ' ') { i++; continue }
                if (tokens[i].isDigit() || tokens[i] == '.') {
                    val sb = StringBuilder()
                    while (i < tokens.length && (tokens[i].isDigit() || tokens[i] == '.')) {
                        sb.append(tokens[i++])
                    }
                    values.add(sb.toString().toDouble())
                    i--
                } else {
                    while (operators.isNotEmpty() && hasPrecedence(tokens[i], operators.last())) {
                        values.add(applyOp(operators.removeAt(operators.size - 1), values.removeAt(values.size - 1), values.removeAt(values.size - 1)))
                    }
                    operators.add(tokens[i])
                }
                i++
            }
            while (operators.isNotEmpty()) {
                values.add(applyOp(operators.removeAt(operators.size - 1), values.removeAt(values.size - 1), values.removeAt(values.size - 1)))
            }
            values[0]
        } catch (_: Exception) {
            Double.NaN
        }
    }

    private fun hasPrecedence(op1: Char, op2: Char): Boolean {
        return !((op1 == '*' || op1 == '/') && (op2 == '+' || op2 == '-'))
    }

    private fun applyOp(op: Char, b: Double, a: Double): Double {
        return when (op) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> if (b != 0.0) a / b else Double.NaN
            else -> 0.0
        }
    }
}
