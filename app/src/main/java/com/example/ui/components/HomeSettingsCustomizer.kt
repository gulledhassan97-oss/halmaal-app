package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BookEntity
import com.example.data.local.HomeScreenSettingsEntity

private data class BrandThemePreset(
    val name: String,
    val primaryHex: Long,
    val darkAccentHex: Long,
    val ctaHex: Long,
    val canvasHex: Long
)

private val BRAND_THEME_PRESETS = listOf(
    BrandThemePreset("Hal-maal Logo", 0xFF0B2B5CL, 0xFF001027L, 0xFFD99B1EL, 0xFFFAF7F0L),
    BrandThemePreset("Coastal Blue", 0xFF4A8EB2L, 0xFF2A4E65L, 0xFFD95C5CL, 0xFFEBF4F8L),
    BrandThemePreset("Emerald Ink", 0xFF0F766EL, 0xFF0B4F4AL, 0xFFD97706L, 0xFFF4F8F7L),
    BrandThemePreset("Terracotta", 0xFFB45309L, 0xFF451A03L, 0xFF0F766EL, 0xFFFEF7EDL)
)

private val PRIMARY_SWATCHES = listOf(0xFF0B2B5CL, 0xFF4A8EB2L, 0xFF0F766EL, 0xFF3B5BDBL, 0xFFB45309L, 0xFF334155L)
private val DARK_ACCENT_SWATCHES = listOf(0xFF001027L, 0xFF2A4E65L, 0xFF0B4F4AL, 0xFF1E293BL, 0xFF451A03L, 0xFF0F172AL)
private val CTA_SWATCHES = listOf(0xFFD99B1EL, 0xFFD95C5CL, 0xFFD97706L, 0xFF059669L, 0xFFE11D48L, 0xFF0284C7L)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeSettingsCustomizer(
    currentSettings: HomeScreenSettingsEntity,
    books: List<BookEntity>,
    onSaveSettings: (HomeScreenSettingsEntity) -> Unit,
    onResetSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember(currentSettings) { mutableStateOf(currentSettings) }

    LaunchedEffect(currentSettings) {
        draft = currentSettings
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_settings_customizer")
    ) {
        val isNarrow = maxWidth < 360.dp

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Live Brand Palette & Color Branding Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Color Branding & Theme",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Live Preview Strip
                    Surface(
                        color = Color(draft.canvasBackgroundHex),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = draft.brandTitle.ifBlank { "Hal-maal" },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(draft.darkAccentColorHex),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Live Home Theme Preview",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(draft.primaryColorHex)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(color = Color(draft.primaryColorHex), shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = "Panel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(color = Color(draft.darkAccentColorHex), shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = "Pill",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(color = Color(draft.ctaHighlightHex), shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = "CTA",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text("Quick Palette Presets", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BRAND_THEME_PRESETS.forEach { preset ->
                            val isSelected = draft.primaryColorHex == preset.primaryHex &&
                                draft.darkAccentColorHex == preset.darkAccentHex
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    draft = draft.copy(
                                        primaryColorHex = preset.primaryHex,
                                        darkAccentColorHex = preset.darkAccentHex,
                                        ctaHighlightHex = preset.ctaHex,
                                        canvasBackgroundHex = preset.canvasHex
                                    )
                                },
                                label = { Text(preset.name, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(preset.primaryHex))
                                    )
                                }
                            )
                        }
                    }

                    SwatchPickerRow(
                        label = "Primary Panel Blue / Brand",
                        swatches = PRIMARY_SWATCHES,
                        selectedHex = draft.primaryColorHex,
                        onSelect = { draft = draft.copy(primaryColorHex = it) }
                    )
                    SwatchPickerRow(
                        label = "Dark Slate Buttons & Active Chips",
                        swatches = DARK_ACCENT_SWATCHES,
                        selectedHex = draft.darkAccentColorHex,
                        onSelect = { draft = draft.copy(darkAccentColorHex = it) }
                    )
                    SwatchPickerRow(
                        label = "Spotlight / Detail CTA Color",
                        swatches = CTA_SWATCHES,
                        selectedHex = draft.ctaHighlightHex,
                        onSelect = { draft = draft.copy(ctaHighlightHex = it) }
                    )
                }
            }

            // 2. Identity & Welcome Screen Customization
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Brand & Welcome Screen",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Show Welcome", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.width(6.dp))
                            Switch(
                                checked = draft.showWelcomeScreen,
                                onCheckedChange = { draft = draft.copy(showWelcomeScreen = it) },
                                modifier = Modifier.testTag("admin_toggle_welcome_screen")
                            )
                        }
                    }

                    if (isNarrow) {
                        OutlinedTextField(
                            value = draft.brandTitle,
                            onValueChange = { draft = draft.copy(brandTitle = it) },
                            label = { Text("Brand Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_brand_title_input")
                        )
                        OutlinedTextField(
                            value = draft.brandSubtitle,
                            onValueChange = { draft = draft.copy(brandSubtitle = it) },
                            label = { Text("Subtitle") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = draft.brandTitle,
                                onValueChange = { draft = draft.copy(brandTitle = it) },
                                label = { Text("Brand Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_brand_title_input")
                            )
                            OutlinedTextField(
                                value = draft.brandSubtitle,
                                onValueChange = { draft = draft.copy(brandSubtitle = it) },
                                label = { Text("Subtitle") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = draft.welcomeTitle,
                        onValueChange = { draft = draft.copy(welcomeTitle = it) },
                        label = { Text("Welcome Headline") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_welcome_title_input")
                    )
                    OutlinedTextField(
                        value = draft.welcomeSubtitle,
                        onValueChange = { draft = draft.copy(welcomeSubtitle = it) },
                        label = { Text("Welcome Description") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.welcomeButtonText,
                        onValueChange = { draft = draft.copy(welcomeButtonText = it) },
                        label = { Text("Start Button Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 3. HomeScreen Sections, Layout & Spotlight Book
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "HomeScreen Layout & Sections",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    SettingSwitchRow(
                        label = "Show Search Bar",
                        checked = draft.showSearchBar,
                        onCheckedChange = { draft = draft.copy(showSearchBar = it) }
                    )
                    SettingSwitchRow(
                        label = "Show Categories Filter Bar",
                        checked = draft.showCategoriesSection,
                        onCheckedChange = { draft = draft.copy(showCategoriesSection = it) }
                    )
                    SettingSwitchRow(
                        label = "Show Trending Books Section",
                        checked = draft.showTrendingSection,
                        onCheckedChange = { draft = draft.copy(showTrendingSection = it) }
                    )
                    SettingSwitchRow(
                        label = "Show Curved Recommended Panel",
                        checked = draft.showRecommendedPanel,
                        onCheckedChange = { draft = draft.copy(showRecommendedPanel = it) },
                        testTag = "admin_toggle_recommended_panel"
                    )

                    OutlinedTextField(
                        value = draft.categoriesHeading,
                        onValueChange = { draft = draft.copy(categoriesHeading = it) },
                        label = { Text("Categories Heading") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.trendingHeading,
                        onValueChange = { draft = draft.copy(trendingHeading = it) },
                        label = { Text("Trending Section Heading") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_trending_heading_input")
                    )
                    OutlinedTextField(
                        value = draft.recommendedHeading,
                        onValueChange = { draft = draft.copy(recommendedHeading = it) },
                        label = { Text("Recommended Panel Heading") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Trending Layout Style", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = draft.trendingLayoutStyle == "CAROUSEL",
                            onClick = { draft = draft.copy(trendingLayoutStyle = "CAROUSEL") },
                            label = { Text("Horizontal Carousel") },
                            modifier = Modifier.testTag("admin_layout_carousel_chip")
                        )
                        FilterChip(
                            selected = draft.trendingLayoutStyle == "GRID",
                            onClick = { draft = draft.copy(trendingLayoutStyle = "GRID") },
                            label = { Text("Multi-Column Grid") },
                            modifier = Modifier.testTag("admin_layout_grid_chip")
                        )
                    }

                    if (books.isNotEmpty()) {
                        Text("Featured Spotlight Book (More Book Screen)", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            books.forEach { book ->
                                val selected = (draft.featuredBookId ?: books.first().id) == book.id
                                FilterChip(
                                    selected = selected,
                                    onClick = { draft = draft.copy(featuredBookId = book.id) },
                                    label = { Text(book.title.take(22), style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }

            // 4. Save / Reset Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onResetSettings,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("admin_reset_home_settings_button")
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reset Default", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Button(
                    onClick = { onSaveSettings(draft) },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .weight(1.35f)
                        .heightIn(min = 50.dp)
                        .testTag("admin_save_home_settings_button")
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save & Apply Home", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwatchPickerRow(
    label: String,
    swatches: List<Long>,
    selectedHex: Long,
    onSelect: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            swatches.forEach { hex ->
                val selected = selectedHex == hex
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .clickable { onSelect(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}
