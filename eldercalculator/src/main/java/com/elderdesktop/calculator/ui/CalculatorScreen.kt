package com.elderdesktop.calculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalculatorScreen() {
    var displayValue by remember { mutableStateOf("0") }
    var lastNumeric by remember { mutableStateOf(false) }
    var stateError by remember { mutableStateOf(false) }
    var lastDot by remember { mutableStateOf(false) }

    fun evaluate(expression: String): Double {
        return try {
            val operators = listOf("+", "-", "*", "/")
            var op = ""
            for (o in operators) {
                if (expression.contains(o)) {
                    op = o
                    break
                }
            }

            if (op.isEmpty()) return expression.toDouble()

            val parts = expression.split(op)
            if (parts.size < 2 || parts[1].isEmpty()) return parts[0].toDouble()
            
            val v1 = parts[0].toDouble()
            val v2 = parts[1].toDouble()

            when (op) {
                "+" -> v1 + v2
                "-" -> v1 - v2
                "*" -> v1 * v2
                "/" -> if (v2 != 0.0) v1 / v2 else 0.0
                else -> 0.0
            }
        } catch (_: Exception) {
            0.0
        }
    }

    fun onDigit(digit: String) {
        if (stateError) {
            displayValue = digit
            stateError = false
        } else {
            if (displayValue == "0") {
                displayValue = digit
            } else {
                displayValue += digit
            }
        }
        lastNumeric = true
    }

    fun onOperator(operator: String) {
        if (lastNumeric && !stateError) {
            displayValue += operator
            lastNumeric = false
            lastDot = false
        }
    }

    fun onEqual() {
        if (lastNumeric && !stateError) {
            try {
                val result = evaluate(displayValue)
                displayValue = if (result % 1 == 0.0) result.toInt().toString() else result.toString()
                lastDot = displayValue.contains(".")
            } catch (_: Exception) {
                displayValue = "Error"
                stateError = true
                lastNumeric = false
            }
        }
    }

    fun onClear() {
        displayValue = "0"
        lastNumeric = false
        stateError = false
        lastDot = false
    }

    fun onDelete() {
        if (displayValue.length > 1) {
            displayValue = displayValue.substring(0, displayValue.length - 1)
        } else {
            displayValue = "0"
        }
        val lastChar = displayValue.last()
        lastNumeric = lastChar.isDigit()
        lastDot = displayValue.contains(".")
    }

    fun onPercent() {
        if (lastNumeric && !stateError) {
            try {
                val value = displayValue.toDouble() / 100
                displayValue = value.toString()
            } catch (_: Exception) {
                displayValue = "Error"
                stateError = true
                lastNumeric = false
            }
        }
    }

    fun onDecimal() {
        if (lastNumeric && !stateError && !lastDot) {
            displayValue += "."
            lastNumeric = false
            lastDot = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White)
                .padding(24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = displayValue,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1
            )
        }

        // Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(3f)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val buttonModifier = Modifier
                .weight(1f)
                .fillMaxHeight()

            // Row 1
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcButton("C", Color(0xFFBDBDBD), Color.Black, buttonModifier) { onClear() }
                CalcButton("DEL", Color(0xFFBDBDBD), Color.Black, buttonModifier) { onDelete() }
                CalcButton("%", Color(0xFFBDBDBD), Color.Black, buttonModifier) { onPercent() }
                CalcButton("÷", Color(0xFFFF9800), Color.White, buttonModifier) { onOperator("/") }
            }

            // Row 2
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcButton("7", Color(0xFFE0E0E0), Color.Black, buttonModifier) { onDigit("7") }
                CalcButton("8", Color(0xFFE0E0E0), Color.Black, buttonModifier) { onDigit("8") }
                CalcButton("9", Color(0xFFE0E0E0), Color.Black, buttonModifier) { onDigit("9") }
                CalcButton("×", Color(0xFFFF9800), Color.White, buttonModifier) { onOperator("*") }
            }

            // Row 3
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcButton("4", Color(0xFFE0E0E0), Color.Black, buttonModifier) { onDigit("4") }
                CalcButton("5", Color(0xFFE0E0E0), Color.Black, buttonModifier) { onDigit("5") }
                CalcButton("6", Color(0xFFE0E0E0), Color.Black, buttonModifier) { onDigit("6") }
                CalcButton("−", Color(0xFFFF9800), Color.White, buttonModifier) { onOperator("-") }
            }

            // Row 4
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcButton("1", Color(0xFFE0E0E0), Color.Black, buttonModifier) { onDigit("1") }
                CalcButton("2", Color(0xFFE0E0E0), Color.Black, buttonModifier) { onDigit("2") }
                CalcButton("3", Color(0xFFE0E0E0), Color.Black, buttonModifier) { onDigit("3") }
                CalcButton("+", Color(0xFFFF9800), Color.White, buttonModifier) { onOperator("+") }
            }

            // Row 5
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcButton(".", Color(0xFFE0E0E0), Color.Black, buttonModifier) { onDecimal() }
                CalcButton("0", Color(0xFFE0E0E0), Color.Black, buttonModifier) { onDigit("0") }
                CalcButton("=", Color(0xFF4CAF50), Color.White, Modifier.weight(2f).fillMaxHeight()) { onEqual() }
            }
        }
    }
}

@Composable
fun CalcButton(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = text, fontSize = 32.sp, fontWeight = FontWeight.Bold)
    }
}
