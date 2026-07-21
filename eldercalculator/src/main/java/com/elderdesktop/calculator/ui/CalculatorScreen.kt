package com.elderdesktop.calculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elderdesktop.calculator.R
import com.elderdesktop.calculator.util.CalculatorUtils

@Composable
fun CalculatorScreen() {
    var displayValue by remember { mutableStateOf("0") }
    var lastNumeric by remember { mutableStateOf(false) }
    var stateError by remember { mutableStateOf(false) }
    var lastDot by remember { mutableStateOf(false) }
    
    val isHolo = MaterialTheme.colorScheme.primary.toArgb() == 0xFF33B5E5.toInt()
    val backgroundColor = if (isHolo) Color.Black else Color(0xFFF5F5F5)
    val displayBg = if (isHolo) Color.Black else Color.White
    val displayTextColor = if (isHolo) Color.White else Color.Black
    val operatorColor = if (isHolo) Color(0xFF33B5E5) else Color(0xFFFF9800)
    val numberBtnColor = if (isHolo) Color(0xFF222222) else Color(0xFFE0E0E0)
    val specialBtnColor = if (isHolo) Color(0xFF444444) else Color(0xFFBDBDBD)
    val shape = if (isHolo) RoundedCornerShape(4.dp) else RoundedCornerShape(16.dp)
    val errorText = stringResource(R.string.error)

    fun onDigit(digit: String) {
        if (stateError) {
            displayValue = digit
            stateError = false
        } else if (displayValue == "0") {
            displayValue = digit
        } else {
            displayValue += digit
        }
        lastNumeric = true
    }

    fun onOperator(op: String) {
        if (lastNumeric && !stateError) {
            displayValue += op
            lastNumeric = false
            lastDot = false
        }
    }

    fun onEqual() {
        if (lastNumeric && !stateError) {
            try {
                val result = CalculatorUtils.evaluate(displayValue)
                displayValue = if (result.isNaN()) errorText else {
                    val formatted = result.toString()
                    if (formatted.endsWith(".0")) formatted.substring(0, formatted.length - 2) else formatted
                }
                stateError = result.isNaN()
                lastNumeric = !result.isNaN()
                lastDot = displayValue.contains(".")
            } catch (_: Exception) {
                displayValue = errorText
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
            val lastChar = displayValue.last()
            lastNumeric = lastChar.isDigit()
            lastDot = displayValue.contains(".")
        } else {
            onClear()
        }
    }

    fun onPercent() {
        if (lastNumeric && !stateError) {
            try {
                val value = displayValue.toDouble() / 100
                displayValue = value.toString()
                if (displayValue.endsWith(".0")) displayValue = displayValue.substring(0, displayValue.length - 2)
                lastDot = displayValue.contains(".")
            } catch (_: Exception) {
                stateError = true
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

    Column(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f).background(displayBg).padding(24.dp), contentAlignment = Alignment.BottomEnd) {
            Text(text = displayValue, fontSize = 64.sp, fontWeight = FontWeight.Bold, color = displayTextColor, maxLines = 1)
        }
        Column(modifier = Modifier.fillMaxWidth().weight(3f).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val buttonModifier = Modifier.weight(1f).fillMaxHeight()
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcButton("C", specialBtnColor, displayTextColor, buttonModifier, shape) { onClear() }
                CalcButton("DEL", specialBtnColor, displayTextColor, buttonModifier, shape) { onDelete() }
                CalcButton("%", specialBtnColor, displayTextColor, buttonModifier, shape) { onPercent() }
                CalcButton("÷", operatorColor, Color.White, buttonModifier, shape) { onOperator("/") }
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcButton("7", numberBtnColor, displayTextColor, buttonModifier, shape) { onDigit("7") }
                CalcButton("8", numberBtnColor, displayTextColor, buttonModifier, shape) { onDigit("8") }
                CalcButton("9", numberBtnColor, displayTextColor, buttonModifier, shape) { onDigit("9") }
                CalcButton("×", operatorColor, Color.White, buttonModifier, shape) { onOperator("*") }
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcButton("4", numberBtnColor, displayTextColor, buttonModifier, shape) { onDigit("4") }
                CalcButton("5", numberBtnColor, displayTextColor, buttonModifier, shape) { onDigit("5") }
                CalcButton("6", numberBtnColor, displayTextColor, buttonModifier, shape) { onDigit("6") }
                CalcButton("−", operatorColor, Color.White, buttonModifier, shape) { onOperator("-") }
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcButton("1", numberBtnColor, displayTextColor, buttonModifier, shape) { onDigit("1") }
                CalcButton("2", numberBtnColor, displayTextColor, buttonModifier, shape) { onDigit("2") }
                CalcButton("3", numberBtnColor, displayTextColor, buttonModifier, shape) { onDigit("3") }
                CalcButton("+", operatorColor, Color.White, buttonModifier, shape) { onOperator("+") }
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcButton(".", numberBtnColor, displayTextColor, buttonModifier, shape) { onDecimal() }
                CalcButton("0", numberBtnColor, displayTextColor, buttonModifier, shape) { onDigit("0") }
                CalcButton("=", Color(0xFF4CAF50), Color.White, Modifier.weight(2f).fillMaxHeight(), shape) { onEqual() }
            }
        }
    }
}

@Composable
fun CalcButton(text: String, backgroundColor: Color, contentColor: Color, modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp), onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier, shape = shape, colors = ButtonDefaults.buttonColors(containerColor = backgroundColor, contentColor = contentColor), contentPadding = PaddingValues(0.dp)) {
        Text(text = text, fontSize = 32.sp, fontWeight = FontWeight.Bold)
    }
}
