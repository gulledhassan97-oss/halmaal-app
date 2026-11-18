package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.state.BookEditorModalState

private val EDITOR_ACCENT_SWATCHES = listOf(
    0xFF0F766EL to "Teal",
    0xFF1E3A8AL to "Cobalt",
    0xFF9A3412L to "Terracotta",
    0xFF065F46L to "Forest",
    0xFF581C87L to "Plum",
    0xFF1E293BL to "Slate"
)

private val EDITOR_CATEGORY_PRESETS = listOf(
    "Heritage & Poetry",
    "Technology & Fintech",
    "Architecture",
    "Business",
    "Digital Edition"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookEditorSheet(
    state: BookEditorModalState,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAccentColorSelected: (Long) -> Unit,
    onToggleManuscriptMode: (Boolean) -> Unit,
    onManuscriptTextChange: (String) -> Unit,
    onSelectPdfUri: (Uri?) -> Unit,
    onClearSelectedPdf: () -> Unit,
    onSubmit: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onSelectPdfUri(uri)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            val isNarrow = maxWidth < 360.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = if (isNarrow) 16.dp else 24.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.isEditMode) "Update Publication" else "Upload New Book",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (state.isEditMode) {
                            "Edit metadata, cover color, or replace PDF pages for ${state.editingBook?.totalPages ?: 0}p edition."
                        } else {
                            "Upload a PDF to rasterize protected pages or compose a digital edition."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    enabled = !state.isSaving,
                    modifier = Modifier.testTag("editor_close_button")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close book editor")
                }
            }

            Spacer(Modifier.height(14.dp))

            state.errorMessage?.let { err ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = { Text("Book Title *") },
                singleLine = true,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_title_input")
            )

            Spacer(Modifier.height(10.dp))

            if (isNarrow) {
                OutlinedTextField(
                    value = state.author,
                    onValueChange = onAuthorChange,
                    label = { Text("Author") },
                    singleLine = true,
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_author_input")
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.priceText,
                    onValueChange = onPriceChange,
                    label = { Text("Price (USD) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_price_input")
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = state.author,
                        onValueChange = onAuthorChange,
                        label = { Text("Author") },
                        singleLine = true,
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("editor_author_input")
                    )
                    OutlinedTextField(
                        value = state.priceText,
                        onValueChange = onPriceChange,
                        label = { Text("Price (USD) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("editor_price_input")
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = state.category,
                onValueChange = onCategoryChange,
                label = { Text("Category") },
                singleLine = true,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_category_input")
            )

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EDITOR_CATEGORY_PRESETS.forEach { preset ->
                    FilterChip(
                        selected = state.category.equals(preset, ignoreCase = true),
                        onClick = { onCategoryChange(preset) },
                        enabled = !state.isSaving,
                        label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = { Text("Synopsis / Description") },
                minLines = 2,
                maxLines = 4,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_description_input")
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Cover Spine & Theme Color",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EDITOR_ACCENT_SWATCHES.forEach { (hex, label) ->
                    val selected = state.accentColorHex == hex
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable(enabled = !state.isSaving) { onAccentColorSelected(hex) }
                            .testTag("editor_color_$label"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "$label selected",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (state.isEditMode) "Book Content (Optional Replacement)" else "Book Content Source *",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = !state.useManuscriptMode,
                    onClick = { onToggleManuscriptMode(false) },
                    enabled = !state.isSaving,
                    leadingIcon = {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text("PDF File") },
                    modifier = Modifier.testTag("editor_source_pdf_chip")
                )
                FilterChip(
                    selected = state.useManuscriptMode,
                    onClick = { onToggleManuscriptMode(true) },
                    enabled = !state.isSaving,
                    leadingIcon = {
                        Icon(Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text("Compose Edition") },
                    modifier = Modifier.testTag("editor_source_manuscript_chip")
                )
            }

            Spacer(Modifier.height(10.dp))

            if (!state.useManuscriptMode) {
                if (state.isEditMode && state.selectedPdfLabel == null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "Current source: ${state.editingBook?.pdfFilename} (${state.editingBook?.totalPages ?: 0} pages). Leave blank to keep existing pages.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("editor_pick_pdf_button"),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = state.selectedPdfLabel ?: if (state.isEditMode) {
                                "Replace PDF File"
                            } else {
                                "Select PDF File *"
                            },
                            maxLines = 1
                        )
                    }
                    if (state.selectedPdfLabel != null) {
                        IconButton(
                            onClick = onClearSelectedPdf,
                            enabled = !state.isSaving,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear selected PDF")
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = state.manuscriptText,
                    onValueChange = onManuscriptTextChange,
                    label = {
                        Text(
                            if (state.isEditMode) {
                                "Replacement Manuscript Text (blank line = new section)"
                            } else {
                                "Manuscript Content (separate paragraphs with blank lines) *"
                            }
                        )
                    },
                    minLines = 4,
                    maxLines = 8,
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_manuscript_input")
                )
            }

            if (state.isSaving) {
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text(
                    text = state.progressText ?: "Processing publication...",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onSubmit,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("editor_submit_button"),
                shape = MaterialTheme.shapes.small
            ) {
                Icon(
                    imageVector = if (state.isEditMode) Icons.Filled.Save else Icons.Filled.CloudUpload,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (state.isEditMode) "Save Book Updates" else "Publish Book to Catalog"
                )
            }
        }
        }
    }
}
