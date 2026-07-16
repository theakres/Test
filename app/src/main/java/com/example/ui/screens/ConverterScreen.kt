package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import com.example.ui.viewmodel.CalculatorViewModel
import com.example.ui.viewmodel.ConverterCategory
import com.example.util.Translator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val lang = viewModel.appLanguage

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {


        if (viewModel.converterCategory == ConverterCategory.AI_TRANSLATOR) {
            // --- AI SMART TRANSLATOR LAYOUT ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = Translator.translate("ai_translator", lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Text(
                            text = Translator.translate("ai_translator_desc", lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )

                        OutlinedTextField(
                            value = viewModel.aiPrompt,
                            onValueChange = { viewModel.aiPrompt = it },
                            placeholder = { Text(Translator.translate("ai_prompt_placeholder", lang)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .testTag("ai_prompt_input"),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.submitAiTranslation()
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    focusManager.clearFocus()
                                    viewModel.submitAiTranslation()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_submit_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(Translator.translate("ask_gemini", lang))
                            }

                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.clearAiTranslation()
                                }
                            ) {
                                Text(Translator.translate("clear", lang))
                            }
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = viewModel.isAiLoading || viewModel.aiResult.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Translator.translate("ai_result_title", lang),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (viewModel.isAiLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            SelectionContainer {
                                Text(
                                    text = parseMarkdown(viewModel.aiResult),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("ai_result_text")
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // --- STANDARD DUAL-INPUT TRANSLATOR LAYOUT ---
            
            // Currency Live Rate Sync Panel
            if (viewModel.converterCategory == ConverterCategory.CURRENCY) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val translatedStatus = when (viewModel.rateSyncStatus) {
                                    "Fallbacks Active" -> if (lang == "ru") "Резервные курсы активны" else "Fallbacks Active"
                                    "Rates Synced via Gemini" -> if (lang == "ru") "Курсы синхронизированы" else "Rates Synced via Gemini"
                                    "Syncing with Gemini..." -> if (lang == "ru") "Идёт синхронизация..." else "Syncing with Gemini..."
                                    else -> viewModel.rateSyncStatus
                                }
                                Text(
                                    text = translatedStatus,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (lang == "ru") "Использует Gemini для получения актуальных курсов валют" else "Uses Gemini to retrieve current global rates",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.refreshCurrencyRates()
                                },
                                enabled = !viewModel.isRefreshingRates,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("sync_rates_btn")
                            ) {
                                if (viewModel.isRefreshingRates) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Translator.translate("rate_sync", lang), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Dual Conversion Panels
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Source Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = Translator.translate("from_unit", lang),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Unit Selection Dropdown
                                UnitDropdownSelector(
                                    selectedUnit = viewModel.converterSourceUnit,
                                    units = viewModel.getCategoryUnits(viewModel.converterCategory),
                                    onUnitSelected = { viewModel.onSourceUnitSelected(it) },
                                    modifier = Modifier.weight(1.2f),
                                    lang = lang
                                )

                                // Number Text Field
                                val isHexInput = viewModel.converterCategory == ConverterCategory.NUMBER_SYSTEMS && viewModel.converterSourceUnit == "Hexadecimal"
                                OutlinedTextField(
                                    value = viewModel.converterSourceValue,
                                    onValueChange = { viewModel.onConverterSourceValueChanged(it) },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = if (isHexInput) KeyboardType.Text else KeyboardType.Number,
                                        capitalization = KeyboardCapitalization.Characters,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { focusManager.clearFocus() }
                                    ),
                                    modifier = Modifier
                                        .weight(1.8f)
                                        .testTag("converter_source_input"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    // Floating Swap Button Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        FloatingActionButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onConverterUnitSwapped()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("swap_units_btn"),
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = "Swap Units")
                        }
                    }

                    // Target Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = Translator.translate("to_unit", lang),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Unit Selection Dropdown
                                UnitDropdownSelector(
                                    selectedUnit = viewModel.converterTargetUnit,
                                    units = viewModel.getCategoryUnits(viewModel.converterCategory),
                                    onUnitSelected = { viewModel.onTargetUnitSelected(it) },
                                    modifier = Modifier.weight(1.2f),
                                    lang = lang
                                )

                                // Result Text View
                                Box(
                                    modifier = Modifier
                                        .weight(1.8f)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = viewModel.converterTargetValue.ifEmpty { "0" },
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        modifier = Modifier.testTag("converter_target_output")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Save to History Button
            item {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.saveCurrentConversionToHistory()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("save_conversion_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Translator.translate("save_to_history", lang))
                }
            }
        }
    }
}

@Composable
fun UnitDropdownSelector(
    selectedUnit: String,
    units: List<String>,
    onUnitSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    lang: String = "en"
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("dropdown_trigger_$selectedUnit"),
            color = Color.Transparent,
            border = null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Translator.translateUnit(selectedUnit, lang),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .testTag("dropdown_menu")
        ) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(Translator.translateUnit(unit, lang)) },
                    onClick = {
                        onUnitSelected(unit)
                        expanded = false
                    },
                    modifier = Modifier.testTag("dropdown_item_$unit")
                )
            }
        }
    }
}

@Composable
private fun parseMarkdown(text: String): AnnotatedString {
    val primaryColor = MaterialTheme.colorScheme.primary
    return remember(text, primaryColor) {
        buildAnnotatedString {
            val lines = text.split("\n")
            lines.forEachIndexed { index, line ->
                var currentLine = line
                var isHeader = false
                var headerLevel = 0
                
                // Parse headers (e.g. ## Header)
                if (currentLine.startsWith("#")) {
                    isHeader = true
                    while (currentLine.startsWith("#")) {
                        headerLevel++
                        currentLine = currentLine.drop(1)
                    }
                    currentLine = currentLine.trim()
                }

                // If list item, add a bullet point or formatting
                val isBullet = currentLine.startsWith("- ") || currentLine.startsWith("* ")
                if (isBullet) {
                    currentLine = "  • " + currentLine.drop(2).trim()
                }

                val startIdx = this.length

                // Parse inline formatting: bold (**), italic (*)
                var i = 0
                while (i < currentLine.length) {
                    if (currentLine.startsWith("**", i)) {
                        val endBold = currentLine.indexOf("**", i + 2)
                        if (endBold != -1) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(currentLine.substring(i + 2, endBold))
                            }
                            i = endBold + 2
                        } else {
                            append("**")
                            i += 2
                        }
                    } else if (currentLine.startsWith("*", i)) {
                        val endItalic = currentLine.indexOf("*", i + 1)
                        if (endItalic != -1) {
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(currentLine.substring(i + 1, endItalic))
                            }
                            i = endItalic + 1
                        } else {
                            append("*")
                            i += 1
                        }
                    } else {
                        append(currentLine[i])
                        i++
                    }
                }

                val endIdx = this.length
                if (isHeader) {
                    addStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            fontSize = if (headerLevel <= 2) 20.sp else 16.sp
                        ),
                        start = startIdx,
                        end = endIdx
                    )
                }

                if (index < lines.lastIndex) {
                    append("\n")
                }
            }
        }
    }
}
