package com.elderdesktop.calculator

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var display: TextView
    private var lastNumeric: Boolean = false
    private var stateError: Boolean = false
    private var lastDot: Boolean = false

    @SuppressLint("SourceLockedOrientation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        display = findViewById(R.id.display)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupButtons()
    }

    private fun setupButtons() {
        val buttons = listOf(
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        )

        for (id in buttons) {
            findViewById<Button>(id).setOnClickListener { onDigit(it) }
        }

        findViewById<Button>(R.id.btn_dot).setOnClickListener { onDecimalPoint() }
        findViewById<Button>(R.id.btn_clear).setOnClickListener { onClear() }
        findViewById<Button>(R.id.btn_delete).setOnClickListener { onDelete() }
        findViewById<Button>(R.id.btn_add).setOnClickListener { onOperator("+") }
        findViewById<Button>(R.id.btn_subtract).setOnClickListener { onOperator("-") }
        findViewById<Button>(R.id.btn_multiply).setOnClickListener { onOperator("*") }
        findViewById<Button>(R.id.btn_divide).setOnClickListener { onOperator("/") }
        findViewById<Button>(R.id.btn_percent).setOnClickListener { onPercent() }
        findViewById<Button>(R.id.btn_equals).setOnClickListener { onEqual() }
    }

    fun onDigit(view: View) {
        if (stateError) {
            display.text = (view as Button).text
            stateError = false
        } else {
            if (display.text.toString() == "0") {
                display.text = (view as Button).text
            } else {
                display.append((view as Button).text)
            }
        }
        lastNumeric = true
    }

    fun onDecimalPoint() {
        if (lastNumeric && !stateError && !lastDot) {
            display.append(".")
            lastNumeric = false
            lastDot = true
        }
    }

    fun onOperator(operator: String) {
        if (lastNumeric && !stateError) {
            display.append(operator)
            lastNumeric = false
            lastDot = false
        }
    }

    fun onClear() {
        display.text = "0"
        lastNumeric = false
        stateError = false
        lastDot = false
    }

    fun onDelete() {
        val text = display.text.toString()
        if (text.length > 1) {
            display.text = text.substring(0, text.length - 1)
        } else {
            display.text = "0"
        }
        
        val newText = display.text.toString()
        lastNumeric = newText.last().isDigit()
        lastDot = newText.contains(".")
    }

    @SuppressLint("SetTextI18n")
    fun onPercent() {
        if (lastNumeric && !stateError) {
            try {
                val value = display.text.toString().toDouble() / 100
                display.text = value.toString()
            } catch (_: Exception) {
                display.text = "Error"
                stateError = true
                lastNumeric = false
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun onEqual() {
        if (lastNumeric && !stateError) {
            val text = display.text.toString()
            try {
                val result = evaluate(text)
                display.text = result.toString()
                lastDot = display.text.contains(".")
            } catch (_: Exception) {
                display.text = "Error"
                stateError = true
                lastNumeric = false
            }
        }
    }

    private fun evaluate(expression: String): Double {
        // Simple evaluation logic for demo purposes
        // In a real app, use a proper expression evaluator
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
            val v1 = parts[0].toDouble()
            val v2 = parts[1].toDouble()
            
            when (op) {
                "+" -> v1 + v2
                "-" -> v1 - v2
                "*" -> v1 * v2
                "/" -> v1 / v2
                else -> 0.0
            }
        } catch (_: Exception) {
            0.0
        }
    }
}
