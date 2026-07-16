package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.CalculatorViewModel
import com.example.util.Translator

@Composable
fun BmiScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val lang = viewModel.appLanguage

    val categoryColor = when (viewModel.bmiCategoryKey) {
        "bmi_underweight" -> Color(0xFF3AB0FF) // Light Blue
        "bmi_normal" -> Color(0xFF4CAF50) // Healthy Green
        "bmi_overweight" -> Color(0xFFFF9800) // Orange Warning
        "bmi_obese" -> Color(0xFFF44336) // Urgent Red
        else -> MaterialTheme.colorScheme.primary
    }

    val adviceText = when (viewModel.bmiCategoryKey) {
        "bmi_underweight" -> when (lang) {
            "ru" -> "Рекомендуется полноценное сбалансированное питание и консультация врача для оценки дефицита массы."
            "uk" -> "Рекомендується повноцінне збалансоване харчування та консультація лікаря для оцінки дефіциту маси."
            "be" -> "Рэкамендуецца паўнавартаснае збалансаванае харчаванне і кансультацыя ўрача для ацэнкі дэфіцыту масы."
            "kk" -> "Толыққанды теңгерімді тамақтану және салмақ тапшылығын бағалау үшін дәрігермен кеңесу ұсынылады."
            "es" -> "Se recomienda una nutrición equilibrada y consultar a un médico para evaluar el peso corporal bajo."
            "pl" -> "Zaleca się pełnowartościowe zbilansowane odżywianie i konsultację lekarską w celu oceny niedowagi."
            "de" -> "Eine ausgewogene Ernährung und ärztliche Beratung zur Abklärung des Untergewichts werden empfohlen."
            else -> "A rich, balanced diet and medical consultation to evaluate weight deficit are highly recommended."
        }
        "bmi_normal" -> when (lang) {
            "ru" -> "Отличный показатель! Продолжайте поддерживать здоровый образ жизни, правильное питание и физическую активность."
            "uk" -> "Чудовий показник! Продовжуйте підтримувати здоровий спосіб життя, правильне харчування та фізичну активність."
            "be" -> "Выдатны паказчык! Працягвайце падтрымліваць здаровы лад жыцця, правільнае харчаванне і фізічную актыўнасць."
            "kk" -> "Тамаша көрсеткіш! Салауатты өмір салтын, дұрыс тамақтануды және белсенділікті қолдауды жалғастырыңыз."
            "es" -> "¡Excelente puntuación! Siga manteniendo un estilo de vida saludable, alimentación equilibrada y actividad física."
            "pl" -> "Świetny wynik! Kontynuuj zdrowy styl życia, prawidłowe odżywianie i aktywność fizyczną."
            "de" -> "Hervorragender Wert! Behalten Sie Ihren gesunden Lebensstil, eine ausgewogene Ernährung und regelmäßige Aktivität bei."
            else -> "Excellent metric! Keep up the healthy lifestyle, proper nutrition, and consistent physical activity."
        }
        "bmi_overweight" -> when (lang) {
            "ru" -> "Небольшой избыток веса. Рекомендуется ограничить простые углеводы и увеличить ежедневную аэробную активность."
            "uk" -> "Невеликий надлишок ваги. Рекомендується обмежити прості вуглеводи та збільшити щоденну аеробну активність."
            "be" -> "Невялікі лішак вагі. Рэкамендуецца абмежаваць простыя вугляводы і павялічыць штодзённую аэробную актыўнасць."
            "kk" -> "Сәл артық салмақ. Қарапайым көмірсуларды шектеп, күнделікті аэробтық белсенділікті арттыру ұсынылады."
            "es" -> "Ligero sobrepeso. Se aconseja limitar los carbohidratos simples y aumentar la actividad aeróbica diaria."
            "pl" -> "Lekka nadwaga. Zaleca się ograniczenie węglowodanów prostych i zwiększenie codziennej aktywności aerobowej."
            "de" -> "Leichtes Übergewicht. Es wird empfohlen, einfache Kohlenhydrate zu reduzieren und die tägliche Bewegung zu steigern."
            else -> "Slightly overweight. It is recommended to reduce simple sugars and boost your daily aerobic active minutes."
        }
        "bmi_obese" -> when (lang) {
            "ru" -> "Показатель указывает на ожирение. Рекомендуется обратиться к диетологу/эндокринологу для составления безопасного плана лечения."
            "uk" -> "Показник вказує на ожиріння. Рекомендується звернутися до дієтолога/ендокринолога для складання безпечного плану лікування."
            "be" -> "Паказчык паказвае на абтыўную вагу. Рэкамендуецца звярнуцца да дыетолага/эндакрынолага для складання бяспечнага плана лячэння."
            "kk" -> "Көрсеткіш семіздікті білдіреді. Қауіпсіз емдеу жоспарын құру үшін диетолог немесе эндокринологпен кеңесу ұсынылады."
            "es" -> "La puntuación indica obesidad. Se aconseja consultar con un endocrinólogo o nutricionista para un plan de tratamiento seguro."
            "pl" -> "Wynik wskazuje na otyłość. Zaleca się konsultację z dietetykiem lub endokrynologiem w celu opracowania planu działania."
            "de" -> "Der Wert deutet auf Adipositas hin. Ein Besuch bei einem Ernährungsberater oder Endokrinologen zur Erstellung eines Behandlungsplans wird empfohlen."
            else -> "This score indicates obesity. Consulting with a professional dietitian or endocrinologist for a safe lifestyle plan is highly recommended."
        }
        else -> ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessibilityNew,
                    contentDescription = "BMI",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = Translator.translate("bmi", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = Translator.translate("bmi_desc", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Unit Switcher Metric / Imperial
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (viewModel.isBmiImperial) Translator.translate("bmi_imperial", lang) else Translator.translate("bmi_metric", lang),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = viewModel.isBmiImperial,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleBmiUnits()
                },
                thumbContent = {
                    Icon(
                        imageVector = Icons.Default.Scale,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            )
        }

        // Input Weight & Height Row/Column
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Height input
            OutlinedTextField(
                value = viewModel.bmiHeight,
                onValueChange = {
                    if (it.isEmpty() || it.matches(Regex("^\\d*$"))) {
                        viewModel.bmiHeight = it
                    }
                },
                label = { Text(Translator.translate("bmi_height", lang) + if (viewModel.isBmiImperial) " (in)" else " (cm)") },
                placeholder = { Text(if (viewModel.isBmiImperial) "70" else "175") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("bmi_height_input")
            )

            // Weight input
            OutlinedTextField(
                value = viewModel.bmiWeight,
                onValueChange = {
                    if (it.isEmpty() || it.matches(Regex("^\\d*$"))) {
                        viewModel.bmiWeight = it
                    }
                },
                label = { Text(Translator.translate("bmi_weight", lang) + if (viewModel.isBmiImperial) " (lbs)" else " (kg)") },
                placeholder = { Text(if (viewModel.isBmiImperial) "150" else "70") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.calculateBmi()
                    }
                ),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("bmi_weight_input")
            )
        }

        // Calculate Button
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                focusManager.clearFocus()
                viewModel.calculateBmi()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("bmi_calculate_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = Translator.translate("bmi_calculate", lang),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Result Visualization Card
        AnimatedVisibility(
            visible = viewModel.bmiResult.isNotEmpty(),
            enter = fadeIn() + slideInVertically(initialOffsetY = { 40 })
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Big BMI display circle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(categoryColor.copy(alpha = 0.15f))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = viewModel.bmiResult,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = categoryColor,
                                fontSize = 34.sp
                            )
                            Text(
                                text = "BMI",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Category text
                    Text(
                        text = Translator.translate(viewModel.bmiCategoryKey, lang),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor,
                        modifier = Modifier.testTag("bmi_result_category")
                    )

                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.outlineVariant)

                    // Advice section
                    if (adviceText.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = Translator.translate("bmi_advice", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = adviceText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
