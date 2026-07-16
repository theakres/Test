package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.CalculatorViewModel
import com.example.ui.viewmodel.ConverterCategory
import com.example.util.Translator

sealed interface ActiveTool {
    object Menu : ActiveTool
    data class Converter(val category: ConverterCategory) : ActiveTool
    object Bmi : ActiveTool
    object DateCalc : ActiveTool
}

data class HubItem(
    val id: String,
    val labelKey: String,
    val icon: ImageVector,
    val activeTool: ActiveTool
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsHubScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val lang = viewModel.appLanguage

    var activeTool by remember { mutableStateOf<ActiveTool>(ActiveTool.Menu) }

    val hubItems = remember(lang) {
        listOf(
            HubItem("currency", "currency", Icons.Default.CurrencyExchange, ActiveTool.Converter(ConverterCategory.CURRENCY)),
            HubItem("length", "length", Icons.Default.Straighten, ActiveTool.Converter(ConverterCategory.LENGTH)),
            HubItem("temp", "temperature", Icons.Default.DeviceThermostat, ActiveTool.Converter(ConverterCategory.TEMPERATURE)),
            HubItem("weight", "weight", Icons.Default.Scale, ActiveTool.Converter(ConverterCategory.WEIGHT)),
            HubItem("area", "area", Icons.Default.Layers, ActiveTool.Converter(ConverterCategory.AREA)),
            HubItem("volume", "volume", Icons.Default.WaterDrop, ActiveTool.Converter(ConverterCategory.VOLUME)),
            HubItem("data", "data_storage", Icons.Default.SdCard, ActiveTool.Converter(ConverterCategory.DATA_STORAGE)),
            HubItem("binary", "number_systems", Icons.Default.Numbers, ActiveTool.Converter(ConverterCategory.NUMBER_SYSTEMS)),
            HubItem("time", "time", Icons.Default.AccessTime, ActiveTool.Converter(ConverterCategory.TIME)),
            HubItem("speed", "speed", Icons.Default.Speed, ActiveTool.Converter(ConverterCategory.SPEED)),
            HubItem("pressure", "pressure", Icons.Default.Compress, ActiveTool.Converter(ConverterCategory.PRESSURE)),
            HubItem("energy", "energy", Icons.Default.Bolt, ActiveTool.Converter(ConverterCategory.ENERGY)),
            HubItem("bmi", "bmi", Icons.Default.AccessibilityNew, ActiveTool.Bmi),
            HubItem("date", "date_calc", Icons.Default.CalendarMonth, ActiveTool.DateCalc),
            HubItem("ai", "ai_translator", Icons.Default.AutoAwesome, ActiveTool.Converter(ConverterCategory.AI_TRANSLATOR))
        )
    }

    AnimatedContent(
        targetState = activeTool,
        transitionSpec = {
            if (targetState is ActiveTool.Menu) {
                (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
            } else {
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
            }
        },
        label = "active_tool_transition"
    ) { currentTool ->
        when (currentTool) {
            is ActiveTool.Menu -> {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = Translator.translate("tools", lang),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(hubItems, key = { it.id }) { item ->
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (item.activeTool is ActiveTool.Converter) {
                                            viewModel.onConverterCategoryChanged(item.activeTool.category)
                                        }
                                        activeTool = item.activeTool
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp)
                                    .testTag("hub_item_${item.id}"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (item.id == "ai") MaterialTheme.colorScheme.tertiaryContainer 
                                            else MaterialTheme.colorScheme.secondaryContainer
                                        )
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = Translator.translate(item.labelKey, lang),
                                        tint = if (item.id == "ai") MaterialTheme.colorScheme.tertiary 
                                               else MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = Translator.translate(item.labelKey, lang),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = {
                            val titleStr = when (currentTool) {
                                is ActiveTool.Converter -> Translator.translate(
                                    when (currentTool.category) {
                                        ConverterCategory.CURRENCY -> "currency"
                                        ConverterCategory.LENGTH -> "length"
                                        ConverterCategory.TEMPERATURE -> "temperature"
                                        ConverterCategory.WEIGHT -> "weight"
                                        ConverterCategory.AREA -> "area"
                                        ConverterCategory.VOLUME -> "volume"
                                        ConverterCategory.DATA_STORAGE -> "data_storage"
                                        ConverterCategory.NUMBER_SYSTEMS -> "number_systems"
                                        ConverterCategory.TIME -> "time"
                                        ConverterCategory.SPEED -> "speed"
                                        ConverterCategory.PRESSURE -> "pressure"
                                        ConverterCategory.ENERGY -> "energy"
                                        ConverterCategory.AI_TRANSLATOR -> "ai_translator"
                                    }, lang
                                )
                                is ActiveTool.Bmi -> Translator.translate("bmi", lang)
                                is ActiveTool.DateCalc -> Translator.translate("date_calc", lang)
                                else -> ""
                            }
                            Text(text = titleStr, fontWeight = FontWeight.Bold)
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    activeTool = ActiveTool.Menu
                                },
                                modifier = Modifier.testTag("sub_tool_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (currentTool) {
                            is ActiveTool.Converter -> {
                                ConverterScreen(viewModel = viewModel)
                            }
                            is ActiveTool.Bmi -> {
                                BmiScreen(viewModel = viewModel)
                            }
                            is ActiveTool.DateCalc -> {
                                DateCalcScreen(viewModel = viewModel)
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}
