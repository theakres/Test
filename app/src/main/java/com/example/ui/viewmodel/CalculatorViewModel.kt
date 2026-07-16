package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.HistoryItem
import com.example.data.repository.HistoryRepository
import com.example.data.network.GeminiClient
import com.example.util.MathEvaluator
import com.example.util.Translator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class ConverterCategory(val displayName: String) {
    CURRENCY("Currency"),
    LENGTH("Length"),
    TEMPERATURE("Temperature"),
    WEIGHT("Weight/Mass"),
    AREA("Area"),
    VOLUME("Volume"),
    DATA_STORAGE("Data / Storage"),
    NUMBER_SYSTEMS("Number Systems"),
    TIME("Time"),
    SPEED("Speed"),
    PRESSURE("Pressure"),
    ENERGY("Energy"),
    AI_TRANSLATOR("AI Smart Translator")
}

class CalculatorViewModel(
    application: Application,
    private val repository: HistoryRepository
) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("calculator_settings", Context.MODE_PRIVATE)

    // --- Settings & Localization State ---
    var appTheme by mutableStateOf("system") // "system", "light", "dark"
        private set
    var appLanguage by mutableStateOf("en") // "en", "ru", "uk", "be", "kk", "es", "pl", "de"
        private set

    // --- Calculator State ---
    var calculatorInput by mutableStateOf("")
        private set
    var calculatorPreviewResult by mutableStateOf("")
        private set
    var calculatorLastResult by mutableStateOf("")
        private set
    var isCalculatorError by mutableStateOf(false)
        private set
    var isScientificMode by mutableStateOf(false) // Toggle between Standard and Engineering / Scientific
        private set

    // --- Converter State ---
    var converterCategory by mutableStateOf(ConverterCategory.CURRENCY)
        private set
    var converterSourceUnit by mutableStateOf("USD")
        private set
    var converterTargetUnit by mutableStateOf("EUR")
        private set
    var converterSourceValue by mutableStateOf("1")
        private set
    var converterTargetValue by mutableStateOf("")
        private set

    // --- BMI State ---
    var bmiWeight by mutableStateOf("70")
    var bmiHeight by mutableStateOf("175")
    var bmiResult by mutableStateOf("")
        private set
    var bmiCategoryKey by mutableStateOf("") // "bmi_underweight", "bmi_normal", "bmi_overweight", "bmi_obese"
        private set
    var isBmiImperial by mutableStateOf(false) // Metric (kg/cm) vs Imperial (lb/inch)

    // --- Date Calculator State ---
    var startDateMillis by mutableStateOf(System.currentTimeMillis())
    var endDateMillis by mutableStateOf(System.currentTimeMillis() + 24 * 60 * 60 * 1000 * 5) // +5 days
    var dateDiffTotalDays by mutableStateOf("")
        private set
    var dateDiffWeeksAndDays by mutableStateOf("")
        private set
    var dateDetailedDiff by mutableStateOf("")
        private set

    // --- Fallback Exchange Rates ---
    val currencyRates = mutableStateMapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "GBP" to 0.79,
        "JPY" to 157.5,
        "CAD" to 1.37,
        "AUD" to 1.50,
        "CNY" to 7.26,
        "INR" to 83.5,
        "BRL" to 5.45,
        "RUB" to 90.0,
        "BYN" to 3.25,
        "UAH" to 41.0,
        "KZT" to 475.0
    )
    var isRefreshingRates by mutableStateOf(false)
        private set
    var rateSyncStatus by mutableStateOf("Fallbacks Active")
        private set

    // --- AI Translator State ---
    var aiPrompt by mutableStateOf("")
    var aiResult by mutableStateOf("")
    var isAiLoading by mutableStateOf(false)

    // --- History Flow ---
    val historyItems: StateFlow<List<HistoryItem>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Load settings from SharedPreferences
        appTheme = sharedPrefs.getString("theme_mode", "system") ?: "system"
        isScientificMode = sharedPrefs.getBoolean("scientific_mode", false)

        val systemLanguage = Locale.getDefault().language
        val defaultLang = when (systemLanguage) {
            "ru" -> "ru"
            "uk" -> "uk"
            "be" -> "be"
            "kk" -> "kk"
            "es" -> "es"
            "pl" -> "pl"
            "de" -> "de"
            else -> "en"
        }
        appLanguage = sharedPrefs.getString("app_language", defaultLang) ?: defaultLang
        
        triggerConversion()
        calculateBmi(saveToHistory = false)
        calculateDateDifference(saveToHistory = false)
    }

    // ==========================================
    // Settings Actions
    // ==========================================

    fun onThemeChanged(newTheme: String) {
        appTheme = newTheme
        sharedPrefs.edit().putString("theme_mode", newTheme).apply()
    }

    fun onLanguageChanged(newLanguage: String) {
        appLanguage = newLanguage
        sharedPrefs.edit().putString("app_language", newLanguage).apply()
    }

    fun toggleScientificMode() {
        isScientificMode = !isScientificMode
        sharedPrefs.edit().putBoolean("scientific_mode", isScientificMode).apply()
    }

    // ==========================================
    // Calculator Actions
    // ==========================================

    fun onCalculatorKeyPress(key: String) {
        isCalculatorError = false
        when (key) {
            "C" -> {
                calculatorInput = ""
                calculatorPreviewResult = ""
                calculatorLastResult = ""
            }
            "⌫" -> {
                if (calculatorInput.isNotEmpty()) {
                    calculatorInput = calculatorInput.dropLast(1)
                }
                updatePreviewResult()
            }
            "=" -> {
                if (calculatorInput.isNotEmpty()) {
                    val finalResult = MathEvaluator.evaluate(calculatorInput)
                    if (finalResult.startsWith("Error")) {
                        isCalculatorError = true
                        calculatorPreviewResult = finalResult
                    } else {
                        calculatorLastResult = finalResult
                        // Log math calculation to history
                        saveHistoryItem(HistoryItem(type = "MATH", description = calculatorInput, result = finalResult))
                        calculatorInput = finalResult
                        calculatorPreviewResult = ""
                    }
                }
            }
            "±" -> {
                negateExpression()
            }
            else -> {
                // If it's a function, append it with parenthesis for comfort
                val appendKey = when (key) {
                    "sin", "cos", "tan", "ln", "log" -> "$key("
                    "√" -> "√("
                    else -> key
                }
                calculatorInput += appendKey
                updatePreviewResult()
            }
        }
    }

    private fun isOperatorChar(str: String): Boolean {
        return str == "+" || str == "-" || str == "×" || str == "÷" || str == "%" || str == "^"
    }

    private fun negateExpression() {
        if (calculatorInput.isEmpty()) {
            calculatorInput = "-"
            return
        }
        if (calculatorInput.startsWith("-")) {
            calculatorInput = calculatorInput.removePrefix("-")
        } else {
            calculatorInput = "-$calculatorInput"
        }
        updatePreviewResult()
    }

    private fun updatePreviewResult() {
        if (calculatorInput.isBlank() || isOperatorChar(calculatorInput.last().toString())) {
            calculatorPreviewResult = ""
            return
        }
        viewModelScope.launch {
            val eval = MathEvaluator.evaluate(calculatorInput)
            if (!eval.startsWith("Error") && eval != calculatorInput) {
                calculatorPreviewResult = eval
            } else {
                calculatorPreviewResult = ""
            }
        }
    }

    // ==========================================
    // Converter Actions
    // ==========================================

    fun onConverterCategoryChanged(category: ConverterCategory) {
        converterCategory = category
        val defaults = getCategoryDefaults(category)
        converterSourceUnit = defaults.first
        converterTargetUnit = defaults.second
        converterSourceValue = if (category == ConverterCategory.NUMBER_SYSTEMS) "10" else "1"
        triggerConversion()
    }

    fun onConverterSourceValueChanged(newValue: String) {
        if (converterCategory == ConverterCategory.NUMBER_SYSTEMS) {
            val cleaned = newValue.trim()
            if (cleaned.isEmpty() || cleaned.matches(Regex("^[0-9a-fA-F]*$"))) {
                converterSourceValue = cleaned.uppercase()
                triggerConversion()
            }
        } else {
            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                converterSourceValue = newValue
                triggerConversion()
            }
        }
    }

    fun onConverterUnitSwapped() {
        val temp = converterSourceUnit
        converterSourceUnit = converterTargetUnit
        converterTargetUnit = temp
        triggerConversion()
    }

    fun onSourceUnitSelected(unit: String) {
        converterSourceUnit = unit
        triggerConversion()
    }

    fun onTargetUnitSelected(unit: String) {
        converterTargetUnit = unit
        triggerConversion()
    }

    private fun triggerConversion() {
        val value = converterSourceValue
        if (value.isEmpty() || value == ".") {
            converterTargetValue = ""
            return
        }

        if (converterCategory == ConverterCategory.NUMBER_SYSTEMS) {
            converterTargetValue = try {
                val sourceBase = when (converterSourceUnit) {
                    "Binary" -> 2
                    "Octal" -> 8
                    "Decimal" -> 10
                    "Hexadecimal" -> 16
                    else -> 10
                }
                val targetBase = when (converterTargetUnit) {
                    "Binary" -> 2
                    "Octal" -> 8
                    "Decimal" -> 10
                    "Hexadecimal" -> 16
                    else -> 10
                }
                val parsedLong = value.toLong(sourceBase)
                parsedLong.toString(targetBase).uppercase()
            } catch (e: Exception) {
                "Error"
            }
            return
        }

        val parsedVal = value.toDoubleOrNull()
        if (parsedVal == null) {
            converterTargetValue = "Error"
            return
        }

        val converted = performConversion(
            category = converterCategory,
            sourceUnit = converterSourceUnit,
            targetUnit = converterTargetUnit,
            valueVal = parsedVal
        )
        converterTargetValue = converted
    }

    private fun performConversion(
        category: ConverterCategory,
        sourceUnit: String,
        targetUnit: String,
        valueVal: Double
    ): String {
        return try {
            val result = when (category) {
                ConverterCategory.CURRENCY -> {
                    val rateSource = currencyRates[sourceUnit] ?: 1.0
                    val rateTarget = currencyRates[targetUnit] ?: 1.0
                    val valInUsd = valueVal / rateSource
                    valInUsd * rateTarget
                }
                ConverterCategory.LENGTH -> {
                    val toMeter = when (sourceUnit) {
                        "km" -> 1000.0
                        "m" -> 1.0
                        "cm" -> 0.01
                        "mm" -> 0.001
                        "mile" -> 1609.344
                        "yard" -> 0.9144
                        "foot" -> 0.3048
                        "inch" -> 0.0254
                        else -> 1.0
                    }
                    val fromMeter = when (targetUnit) {
                        "km" -> 0.001
                        "m" -> 1.0
                        "cm" -> 100.0
                        "mm" -> 1000.0
                        "mile" -> 1 / 1609.344
                        "yard" -> 1 / 0.9144
                        "foot" -> 1 / 0.3048
                        "inch" -> 1 / 0.0254
                        else -> 1.0
                    }
                    valueVal * toMeter * fromMeter
                }
                ConverterCategory.TEMPERATURE -> {
                    val inC = when (sourceUnit) {
                        "Celsius (°C)" -> valueVal
                        "Fahrenheit (°F)" -> (valueVal - 32) * 5 / 9
                        "Kelvin (K)" -> valueVal - 273.15
                        else -> valueVal
                    }
                    when (targetUnit) {
                        "Celsius (°C)" -> inC
                        "Fahrenheit (°F)" -> (inC * 9 / 5) + 32
                        "Kelvin (K)" -> inC + 273.15
                        else -> inC
                    }
                }
                ConverterCategory.WEIGHT -> {
                    // Supported Weight Units: kg, g, mg, µg, t, ct, lb, oz, st, gr
                    val toGram = when (sourceUnit) {
                        "kg" -> 1000.0
                        "g" -> 1.0
                        "mg" -> 0.001
                        "µg" -> 0.000001
                        "ton (t)" -> 1000000.0
                        "carat (ct)" -> 0.2
                        "lb" -> 453.59237
                        "oz" -> 28.34952
                        "stone (st)" -> 6350.29318
                        "grain (gr)" -> 0.06479891
                        else -> 1.0
                    }
                    val fromGram = when (targetUnit) {
                        "kg" -> 0.001
                        "g" -> 1.0
                        "mg" -> 1000.0
                        "µg" -> 1000000.0
                        "ton (t)" -> 0.000001
                        "carat (ct)" -> 5.0
                        "lb" -> 1 / 453.59237
                        "oz" -> 1 / 28.34952
                        "stone (st)" -> 1 / 6350.29318
                        "grain (gr)" -> 1 / 0.06479891
                        else -> 1.0
                    }
                    valueVal * toGram * fromGram
                }
                ConverterCategory.AREA -> {
                    val toSqMeter = when (sourceUnit) {
                        "sq meter (m²)" -> 1.0
                        "sq km (km²)" -> 1000000.0
                        "sq mile (mi²)" -> 2589988.11
                        "sq yard (yd²)" -> 0.836127
                        "sq foot (ft²)" -> 0.092903
                        "acre" -> 4046.856
                        else -> 1.0
                    }
                    val fromSqMeter = when (targetUnit) {
                        "sq meter (m²)" -> 1.0
                        "sq km (km²)" -> 1 / 1000000.0
                        "sq mile (mi²)" -> 1 / 2589988.11
                        "sq yard (yd²)" -> 1 / 0.836127
                        "sq foot (ft²)" -> 1 / 0.092903
                        "acre" -> 1 / 4046.856
                        else -> 1.0
                    }
                    valueVal * toSqMeter * fromSqMeter
                }
                ConverterCategory.VOLUME -> {
                    val toLiter = when (sourceUnit) {
                        "liter (L)" -> 1.0
                        "ml" -> 0.001
                        "gallon (gal)" -> 3.78541
                        "cup" -> 0.236588
                        else -> 1.0
                    }
                    val fromLiter = when (targetUnit) {
                        "liter (L)" -> 1.0
                        "ml" -> 1000.0
                        "gallon (gal)" -> 1 / 3.78541
                        "cup" -> 1 / 0.236588
                        else -> 1.0
                    }
                    valueVal * toLiter * fromLiter
                }
                ConverterCategory.DATA_STORAGE -> {
                    // Base unit: Byte (B)
                    val toByte = when (sourceUnit) {
                        "Bit (b)" -> 0.125
                        "Byte (B)" -> 1.0
                        "Kilobit (Kb)" -> 125.0
                        "Kilobyte (KB)" -> 1024.0
                        "Megabit (Mb)" -> 125000.0
                        "Megabyte (MB)" -> 1048576.0
                        "Gigabit (Gb)" -> 125000000.0
                        "Gigabyte (GB)" -> 1073741824.0
                        "Terabit (Tb)" -> 1.25E11
                        "Terabyte (TB)" -> 1.099511627776E12
                        else -> 1.0
                    }
                    val fromByte = when (targetUnit) {
                        "Bit (b)" -> 8.0
                        "Byte (B)" -> 1.0
                        "Kilobit (Kb)" -> 1 / 125.0
                        "Kilobyte (KB)" -> 1 / 1024.0
                        "Megabit (Mb)" -> 1 / 125000.0
                        "Megabyte (MB)" -> 1 / 1048576.0
                        "Gigabit (Gb)" -> 1 / 125000000.0
                        "Gigabyte (GB)" -> 1 / 1073741824.0
                        "Terabit (Tb)" -> 1 / 1.25E11
                        "Terabyte (TB)" -> 1 / 1.099511627776E12
                        else -> 1.0
                    }
                    valueVal * toByte * fromByte
                }
                ConverterCategory.TIME -> {
                    // Base unit: Second (s)
                    val toSecond = when (sourceUnit) {
                        "Second (s)" -> 1.0
                        "Minute (min)" -> 60.0
                        "Hour (h)" -> 3600.0
                        "Day (d)" -> 86400.0
                        "Week (wk)" -> 604800.0
                        "Year (yr)" -> 31536000.0
                        else -> 1.0
                    }
                    val fromSecond = when (targetUnit) {
                        "Second (s)" -> 1.0
                        "Minute (min)" -> 1.0 / 60.0
                        "Hour (h)" -> 1.0 / 3600.0
                        "Day (d)" -> 1.0 / 86400.0
                        "Week (wk)" -> 1.0 / 604800.0
                        "Year (yr)" -> 1.0 / 31536000.0
                        else -> 1.0
                    }
                    valueVal * toSecond * fromSecond
                }
                ConverterCategory.SPEED -> {
                    // Base unit: m/s
                    val toMps = when (sourceUnit) {
                        "m/s" -> 1.0
                        "km/h" -> 1.0 / 3.6
                        "mph" -> 0.44704
                        "knots" -> 0.514444
                        else -> 1.0
                    }
                    val fromMps = when (targetUnit) {
                        "m/s" -> 1.0
                        "km/h" -> 3.6
                        "mph" -> 1.0 / 0.44704
                        "knots" -> 1.0 / 0.514444
                        else -> 1.0
                    }
                    valueVal * toMps * fromMps
                }
                ConverterCategory.PRESSURE -> {
                    // Base unit: Pascal (Pa)
                    val toPa = when (sourceUnit) {
                        "Pascal (Pa)" -> 1.0
                        "Bar (bar)" -> 100000.0
                        "Atmosphere (atm)" -> 101325.0
                        "psi" -> 6894.76
                        else -> 1.0
                    }
                    val fromPa = when (targetUnit) {
                        "Pascal (Pa)" -> 1.0
                        "Bar (bar)" -> 1.0 / 100000.0
                        "Atmosphere (atm)" -> 1.0 / 101325.0
                        "psi" -> 1.0 / 6894.76
                        else -> 1.0
                    }
                    valueVal * toPa * fromPa
                }
                ConverterCategory.ENERGY -> {
                    // Base unit: Joule (J)
                    val toJoule = when (sourceUnit) {
                        "Joule (J)" -> 1.0
                        "Kilojoule (kJ)" -> 1000.0
                        "Calorie (cal)" -> 4.184
                        "Kilocalorie (kcal)" -> 4184.0
                        "Watt-hour (Wh)" -> 3600.0
                        "Kilowatt-hour (kWh)" -> 3600000.0
                        else -> 1.0
                    }
                    val fromJoule = when (targetUnit) {
                        "Joule (J)" -> 1.0
                        "Kilojoule (kJ)" -> 0.001
                        "Calorie (cal)" -> 1.0 / 4.184
                        "Kilocalorie (kcal)" -> 1.0 / 4184.0
                        "Watt-hour (Wh)" -> 1.0 / 3600.0
                        "Kilowatt-hour (kWh)" -> 1.0 / 3600000.0
                        else -> 1.0
                    }
                    valueVal * toJoule * fromJoule
                }
                else -> valueVal
            }
            formatDouble(result)
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun formatDouble(d: Double): String {
        return try {
            val bd = BigDecimal(d).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros()
            bd.toPlainString()
        } catch (e: Exception) {
            String.format("%.4f", d).trimEnd('0').trimEnd('.')
        }
    }

    private fun getCategoryDefaults(category: ConverterCategory): Pair<String, String> {
        return when (category) {
            ConverterCategory.CURRENCY -> Pair("USD", "EUR")
            ConverterCategory.LENGTH -> Pair("km", "m")
            ConverterCategory.TEMPERATURE -> Pair("Celsius (°C)", "Fahrenheit (°F)")
            ConverterCategory.WEIGHT -> Pair("kg", "g")
            ConverterCategory.AREA -> Pair("sq km (km²)", "sq meter (m²)")
            ConverterCategory.VOLUME -> Pair("liter (L)", "ml")
            ConverterCategory.DATA_STORAGE -> Pair("Megabyte (MB)", "Gigabyte (GB)")
            ConverterCategory.NUMBER_SYSTEMS -> Pair("Decimal", "Binary")
            ConverterCategory.TIME -> Pair("Second (s)", "Minute (min)")
            ConverterCategory.SPEED -> Pair("m/s", "km/h")
            ConverterCategory.PRESSURE -> Pair("Pascal (Pa)", "Bar (bar)")
            ConverterCategory.ENERGY -> Pair("Joule (J)", "Kilojoule (kJ)")
            ConverterCategory.AI_TRANSLATOR -> Pair("", "")
        }
    }

    fun getCategoryUnits(category: ConverterCategory): List<String> {
        return when (category) {
            ConverterCategory.CURRENCY -> currencyRates.keys.toList()
            ConverterCategory.LENGTH -> listOf("m", "km", "cm", "mm", "mile", "yard", "foot", "inch")
            ConverterCategory.TEMPERATURE -> listOf("Celsius (°C)", "Fahrenheit (°F)", "Kelvin (K)")
            ConverterCategory.WEIGHT -> listOf("kg", "g", "mg", "µg", "ton (t)", "carat (ct)", "lb", "oz", "stone (st)", "grain (gr)")
            ConverterCategory.AREA -> listOf("sq meter (m²)", "sq km (km²)", "sq mile (mi²)", "sq yard (yd²)", "sq foot (ft²)", "acre")
            ConverterCategory.VOLUME -> listOf("liter (L)", "ml", "gallon (gal)", "cup")
            ConverterCategory.DATA_STORAGE -> listOf("Bit (b)", "Byte (B)", "Kilobit (Kb)", "Kilobyte (KB)", "Megabit (Mb)", "Megabyte (MB)", "Gigabit (Gb)", "Gigabyte (GB)", "Terabit (Tb)", "Terabyte (TB)")
            ConverterCategory.NUMBER_SYSTEMS -> listOf("Decimal", "Binary", "Hexadecimal", "Octal")
            ConverterCategory.TIME -> listOf("Second (s)", "Minute (min)", "Hour (h)", "Day (d)", "Week (wk)", "Year (yr)")
            ConverterCategory.SPEED -> listOf("m/s", "km/h", "mph", "knots")
            ConverterCategory.PRESSURE -> listOf("Pascal (Pa)", "Bar (bar)", "Atmosphere (atm)", "psi")
            ConverterCategory.ENERGY -> listOf("Joule (J)", "Kilojoule (kJ)", "Calorie (cal)", "Kilocalorie (kcal)", "Watt-hour (Wh)", "Kilowatt-hour (kWh)")
            ConverterCategory.AI_TRANSLATOR -> emptyList()
        }
    }

    fun saveCurrentConversionToHistory() {
        val desc = "$converterSourceValue $converterSourceUnit to $converterTargetUnit"
        if (converterSourceValue.isNotEmpty() && converterTargetValue.isNotEmpty() && converterTargetValue != "Error") {
            saveHistoryItem(HistoryItem(type = "CONVERTER", description = desc, result = converterTargetValue))
        }
    }

    // ==========================================
    // BMI Actions
    // ==========================================

    fun calculateBmi(saveToHistory: Boolean = true) {
        val w = bmiWeight.toDoubleOrNull() ?: return
        val h = bmiHeight.toDoubleOrNull() ?: return
        if (w <= 0.0 || h <= 0.0) return

        val score = if (isBmiImperial) {
            // Imperial Formula: (weight in lbs / (height in inches) ^ 2) * 703
            (w / (h * h)) * 703.0
        } else {
            // Metric Formula: weight in kg / (height in meters) ^ 2
            val hInMeters = h / 100.0
            w / (hInMeters * hInMeters)
        }

        val roundedScore = BigDecimal(score).setScale(2, RoundingMode.HALF_UP).toDouble()
        bmiResult = roundedScore.toString()

        bmiCategoryKey = when {
            score < 18.5 -> "bmi_underweight"
            score < 25.0 -> "bmi_normal"
            score < 30.0 -> "bmi_overweight"
            else -> "bmi_obese"
        }

        if (saveToHistory) {
            val lbsText = Translator.translate("unit_lbs", appLanguage)
            val inchesText = Translator.translate("unit_inches", appLanguage)
            val kgText = Translator.translate("unit_kg", appLanguage)
            val cmText = Translator.translate("unit_cm", appLanguage)
            val bmiCheckText = Translator.translate("bmi_check_desc", appLanguage)
            
            val details = if (isBmiImperial) "$w $lbsText, $h $inchesText" else "$w $kgText, $h $cmText"
            saveHistoryItem(HistoryItem(type = "BMI", description = "$bmiCheckText ($details)", result = "BMI: $bmiResult"))
        }
    }

    fun toggleBmiUnits() {
        isBmiImperial = !isBmiImperial
        if (isBmiImperial) {
            // Convert existing metric values to imperial approximates
            val kg = bmiWeight.toDoubleOrNull() ?: 70.0
            val cm = bmiHeight.toDoubleOrNull() ?: 175.0
            bmiWeight = Math.round(kg * 2.20462).toString()
            bmiHeight = Math.round(cm * 0.393701).toString()
        } else {
            // Convert imperial to metric approximates
            val lb = bmiWeight.toDoubleOrNull() ?: 154.0
            val inch = bmiHeight.toDoubleOrNull() ?: 69.0
            bmiWeight = Math.round(lb / 2.20462).toString()
            bmiHeight = Math.round(inch / 0.393701).toString()
        }
        calculateBmi(saveToHistory = false)
    }

    // ==========================================
    // Date Calculator Actions
    // ==========================================

    fun calculateDateDifference(saveToHistory: Boolean = true) {
        val start = Calendar.getInstance().apply { timeInMillis = startDateMillis }
        val end = Calendar.getInstance().apply { timeInMillis = endDateMillis }

        // Swap if end is before start
        if (end.before(start)) {
            val temp = startDateMillis
            startDateMillis = endDateMillis
            endDateMillis = temp
            start.timeInMillis = startDateMillis
            end.timeInMillis = endDateMillis
        }

        val diffMs = kotlin.math.abs(endDateMillis - startDateMillis)
        val totalDays = TimeUnit.DAYS.convert(diffMs, TimeUnit.MILLISECONDS)
        val totalWeeks = totalDays / 7
        val remainingDays = totalDays % 7

        val weeksLabel = Translator.translate("weeks", appLanguage)
        val daysLabel = Translator.translate("days", appLanguage)
        val monthsLabel = Translator.translate("months", appLanguage)
        val yearsLabel = Translator.translate("years", appLanguage)
        val intervalLabel = Translator.translate("interval_desc", appLanguage)

        dateDiffTotalDays = totalDays.toString()
        dateDiffWeeksAndDays = if (totalWeeks > 0) "$totalWeeks $weeksLabel, $remainingDays $daysLabel" else "$remainingDays $daysLabel"

        // Calculate detailed Years, Months, Days
        var years = end.get(Calendar.YEAR) - start.get(Calendar.YEAR)
        var months = end.get(Calendar.MONTH) - start.get(Calendar.MONTH)
        var days = end.get(Calendar.DAY_OF_MONTH) - start.get(Calendar.DAY_OF_MONTH)

        if (days < 0) {
            months--
            // Borrow days from previous month of end date
            val borrowCal = Calendar.getInstance().apply {
                timeInMillis = endDateMillis
                add(Calendar.MONTH, -1)
            }
            days += borrowCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }

        if (months < 0) {
            years--
            months += 12
        }

        val detailedStr = StringBuilder()
        if (years > 0) detailedStr.append("$years $yearsLabel, ")
        if (months > 0 || years > 0) detailedStr.append("$months $monthsLabel, ")
        detailedStr.append("$days $daysLabel")

        dateDetailedDiff = detailedStr.toString()

        if (saveToHistory) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val desc = "$intervalLabel: ${sdf.format(Date(startDateMillis))} to ${sdf.format(Date(endDateMillis))}"
            saveHistoryItem(HistoryItem(type = "DATE_CALC", description = desc, result = "$totalDays $daysLabel ($dateDetailedDiff)"))
        }
    }

    // ==========================================
    // Gemini AI Live Rate Sync
    // ==========================================

    fun refreshCurrencyRates() {
        if (isRefreshingRates) return
        isRefreshingRates = true
        rateSyncStatus = "Syncing with Gemini..."
        
        viewModelScope.launch {
            val latestRates = GeminiClient.fetchLiveRates()
            if (latestRates != null) {
                latestRates.forEach { (code, rate) ->
                    currencyRates[code] = rate
                }
                rateSyncStatus = "Rates Synced via Gemini"
                triggerConversion()
            } else {
                rateSyncStatus = "Sync Failed, Using Fallbacks"
            }
            isRefreshingRates = false
        }
    }

    // ==========================================
    // AI Smart Translator
    // ==========================================

    fun submitAiTranslation() {
        val prompt = aiPrompt.trim()
        if (prompt.isEmpty() || isAiLoading) return
        isAiLoading = true
        aiResult = "AI is computing translation..."
        
        viewModelScope.launch {
            val systemInstruction = """
                You are an extremely fast, precise unit converter, translator, and calculator assistant.
                Respond DIRECTLY, instantly and as concisely as possible. No greeting, no preamble, no polite fluff. 
                Use clean markdown formatting:
                - Use `##` or `###` for clean section headers.
                - Use `**` for bold keywords, results, or numbers.
                - Use bullet points `-` for short lists.
                Keep descriptions extremely brief to minimize response time.
            """.trimIndent()
            
            val response = GeminiClient.generateText(prompt, systemInstruction)
            if (response == "API_KEY_MISSING") {
                aiResult = "Gemini API Key is missing! Please configure it in the Secrets panel in AI Studio with key 'GEMINI_API_KEY'."
            } else if (response.startsWith("ERROR:")) {
                aiResult = "Failed to translate: ${response.removePrefix("ERROR:")}"
            } else {
                aiResult = response
                saveHistoryItem(HistoryItem(type = "AI_TRANSLATION", description = prompt, result = "AI Answer Saved"))
            }
            isAiLoading = false
        }
    }

    fun clearAiTranslation() {
        aiPrompt = ""
        aiResult = ""
    }

    // ==========================================
    // History Actions
    // ==========================================

    private fun saveHistoryItem(item: HistoryItem) {
        viewModelScope.launch {
            repository.insert(item)
        }
    }

    fun deleteHistoryItem(item: HistoryItem) {
        viewModelScope.launch {
            repository.deleteById(item.id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}

class CalculatorViewModelFactory(
    private val application: Application,
    private val repository: HistoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalculatorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalculatorViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
