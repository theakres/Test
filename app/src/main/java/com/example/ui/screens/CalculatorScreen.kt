package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.CalculatorViewModel
import com.example.util.Translator

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val lang = viewModel.appLanguage

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Math Display Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            val scrollState = rememberScrollState()
            
            // Raw Expression
            Text(
                text = viewModel.calculatorInput.ifEmpty { "0" },
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = if (viewModel.calculatorInput.length > 10) 30.sp else 42.sp
                ),
                color = if (viewModel.isCalculatorError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(vertical = 4.dp)
                    .testTag("calculator_display")
            )

            // Faded Live Preview / Result
            if (viewModel.calculatorPreviewResult.isNotEmpty()) {
                Text(
                    text = viewModel.calculatorPreviewResult,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Normal
                    ),
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .testTag("calculator_preview")
                )
            } else if (viewModel.calculatorLastResult.isNotEmpty() && viewModel.calculatorInput == viewModel.calculatorLastResult) {
                Text(
                    text = "Ans = ${viewModel.calculatorLastResult}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // 2. Tactile Keypad (with smooth Mode Switch Animation!)
        AnimatedContent(
            targetState = viewModel.isScientificMode,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.92f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.92f))
            },
            label = "keypad_mode_transition",
            modifier = Modifier.fillMaxWidth()
        ) { isSci ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val rows = if (isSci) {
                    listOf(
                        listOf("🧮", "sin", "cos", "tan", "log"),
                        listOf("C", "±", "√", "ln", "÷"),
                        listOf("7", "8", "9", "(", "×"),
                        listOf("4", "5", "6", ")", "-"),
                        listOf("1", "2", "3", "π", "+"),
                        listOf("0", ".", "e", "⌫", "=")
                    )
                } else {
                    listOf(
                        listOf("🔬", "C", "±", "÷"),
                        listOf("7", "8", "9", "×"),
                        listOf("4", "5", "6", "-"),
                        listOf("1", "2", "3", "+"),
                        listOf("0", ".", "⌫", "=")
                    )
                }

                for (row in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (key in row) {
                            CalculatorButton(
                                text = key,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (key == "🔬" || key == "🧮") {
                                        viewModel.toggleScientificMode()
                                    } else {
                                        viewModel.onCalculatorKeyPress(key)
                                    }
                                },
                                isScientific = isSci,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(if (isSci) 1.25f else 1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(
    text: String,
    onClick: () -> Unit,
    isScientific: Boolean,
    modifier: Modifier = Modifier
) {
    val isToggle = text == "🔬" || text == "🧮"
    val isOperator = text == "+" || text == "-" || text == "×" || text == "÷" || text == "=" || text == "^"
    val isAction = text == "C" || text == "±" || text == "%" || text == "⌫" || text == "(" || text == ")"
    val isFunction = text == "sin" || text == "cos" || text == "tan" || text == "ln" || text == "log" || text == "√"
    val isConst = text == "π" || text == "e"
    
    val containerColor = when {
        text == "=" -> MaterialTheme.colorScheme.primary
        text == "C" -> MaterialTheme.colorScheme.errorContainer
        isToggle -> MaterialTheme.colorScheme.tertiaryContainer
        isOperator -> MaterialTheme.colorScheme.primaryContainer
        isAction -> MaterialTheme.colorScheme.tertiaryContainer
        isFunction -> MaterialTheme.colorScheme.secondaryContainer
        isConst -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
    }

    val contentColor = when {
        text == "=" -> MaterialTheme.colorScheme.onPrimary
        text == "C" -> MaterialTheme.colorScheme.onErrorContainer
        isToggle -> MaterialTheme.colorScheme.onTertiaryContainer
        isOperator -> MaterialTheme.colorScheme.onPrimaryContainer
        isAction -> MaterialTheme.colorScheme.onTertiaryContainer
        isFunction -> MaterialTheme.colorScheme.onSecondaryContainer
        isConst -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick)
            .testTag("btn_$text")
    ) {
        if (text == "⌫") {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                tint = contentColor,
                modifier = Modifier.size(if (isScientific) 20.dp else 24.dp)
            )
        } else if (text == "🔬") {
            Icon(
                imageVector = Icons.Default.Science,
                contentDescription = "Switch to Scientific",
                tint = contentColor,
                modifier = Modifier.size(if (isScientific) 20.dp else 24.dp)
            )
        } else if (text == "🧮") {
            Icon(
                imageVector = Icons.Default.Calculate,
                contentDescription = "Switch to Standard",
                tint = contentColor,
                modifier = Modifier.size(if (isScientific) 20.dp else 24.dp)
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = if (isOperator || isAction || isFunction) FontWeight.Bold else FontWeight.Medium,
                    fontSize = if (text.length > 2) 15.sp else if (isScientific) 18.sp else 22.sp
                ),
                color = contentColor
            )
        }
    }
}
