package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BookEntity
import com.example.ui.components.PhysicalFlipbookCanvas
import com.example.ui.components.buildBookSpreads
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ReaderCanvas
import com.example.ui.theme.ReaderChrome
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FlipbookReaderScreen(
    book: BookEntity,
    buyerEmail: String?,
    isOwned: Boolean,
    getPageFile: (Long, Int) -> File,
    onCheckAccess: suspend (Long, Int) -> Boolean,
    onBuyToUnlock: (BookEntity) -> Unit,
    onCloseReader: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val totalPages = book.totalPages.coerceAtLeast(1)
    val spreads = remember(totalPages) { buildBookSpreads(totalPages) }
    val flipAnimatable = remember { Animatable(0f) }

    BackHandler(onBack = onCloseReader)

    // Enable FLAG_SECURE while inside the protected reader to block screenshots & screen capture
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ReaderCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val isNarrow = maxWidth < 380.dp
        val isShort = maxHeight < 480.dp
        val defaultSpread = maxWidth >= 600.dp
        var isSpreadMode by remember(defaultSpread) { mutableStateOf(defaultSpread) }
        var currentIndex by remember { mutableIntStateOf(0) }

        val maxIndex = if (isSpreadMode) spreads.lastIndex else (totalPages - 1)
        val safeIndex = currentIndex.coerceIn(0, maxIndex)

        val currentPageLabel = if (isSpreadMode) {
            val spread = spreads[safeIndex]
            when {
                spread.leftPage != null && spread.rightPage != null ->
                    "Pages ${spread.leftPage}–${spread.rightPage} / $totalPages"
                spread.rightPage != null ->
                    "Cover (Page ${spread.rightPage}) / $totalPages"
                else ->
                    "Page ${spread.leftPage ?: 1} / $totalPages"
            }
        } else {
            "Page ${safeIndex + 1} / $totalPages"
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Reader Header Bar with Spread/Single toggle & Close Reader button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ReaderChrome)
                    .padding(horizontal = if (isNarrow) 10.dp else 14.dp, vertical = if (isShort) 4.dp else 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (isOwned) {
                                "Protected • $currentPageLabel"
                            } else {
                                "Preview • $currentPageLabel"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA1A1AA),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(6.dp))

                FilterChip(
                    selected = isSpreadMode,
                    onClick = {
                        val currentPage = if (isSpreadMode) {
                            (spreads[safeIndex].leftPage ?: spreads[safeIndex].rightPage ?: 1)
                        } else {
                            safeIndex + 1
                        }
                        val nextMode = !isSpreadMode
                        isSpreadMode = nextMode
                        currentIndex = if (nextMode) {
                            spreads.indexOfFirst { it.leftPage == currentPage || it.rightPage == currentPage }
                                .coerceAtLeast(0)
                        } else {
                            (currentPage - 1).coerceIn(0, totalPages - 1)
                        }
                        scope.launch { flipAnimatable.snapTo(0f) }
                    },
                    label = {
                        Text(
                            text = if (isSpreadMode) "2-Page" else "1-Page",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isSpreadMode) Icons.AutoMirrored.Filled.MenuBook else Icons.Filled.CropPortrait,
                            contentDescription = "Toggle book spread mode",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF27272A),
                        labelColor = Color.White,
                        iconColor = Color(0xFF2DD4BF),
                        selectedContainerColor = Color(0xFF0F766E),
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    ),
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .testTag("toggle_spread_mode_chip")
                )

                Button(
                    onClick = onCloseReader,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = MaterialTheme.shapes.small,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = if (isNarrow) 10.dp else 14.dp,
                        vertical = 8.dp
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("close_reader_button")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close reader", modifier = Modifier.size(18.dp))
                    if (!isNarrow) {
                        Spacer(Modifier.width(4.dp))
                        Text("Close", color = Color.White, maxLines = 1)
                    }
                }
            }

            // Interactive 3D Physical Book Flipbook Canvas
            PhysicalFlipbookCanvas(
                book = book,
                isSpreadMode = isSpreadMode,
                currentIndex = safeIndex,
                flipAnimatable = flipAnimatable,
                buyerEmail = buyerEmail,
                isOwned = isOwned,
                getPageFile = getPageFile,
                onCheckAccess = onCheckAccess,
                onUnlockClick = { onBuyToUnlock(book) },
                onIndexCommitted = { newIdx -> currentIndex = newIdx },
                modifier = Modifier.weight(1f)
            )

            // Bottom Scrubber & Animated Page-Turn Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ReaderChrome)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (safeIndex > 0 && !flipAnimatable.isRunning) {
                            scope.launch {
                                flipAnimatable.animateTo(-1f, tween(420, easing = FastOutSlowInEasing))
                                currentIndex = safeIndex - 1
                                flipAnimatable.snapTo(0f)
                            }
                        }
                    },
                    enabled = safeIndex > 0,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("reader_prev_page")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous page",
                        tint = Color.White
                    )
                }

                if (maxIndex > 0) {
                    Slider(
                        value = safeIndex.toFloat(),
                        onValueChange = { target ->
                            val idx = target.toInt().coerceIn(0, maxIndex)
                            if (idx != safeIndex) {
                                currentIndex = idx
                                scope.launch { flipAnimatable.snapTo(0f) }
                            }
                        },
                        valueRange = 0f..maxIndex.toFloat(),
                        steps = (maxIndex - 1).coerceAtLeast(0),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF2DD4BF),
                            activeTrackColor = Color(0xFF0F766E)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Text(
                    text = if (isSpreadMode) "${safeIndex + 1}/${spreads.size}" else "${safeIndex + 1}/$totalPages",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = {
                        if (safeIndex < maxIndex && !flipAnimatable.isRunning) {
                            scope.launch {
                                flipAnimatable.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
                                currentIndex = safeIndex + 1
                                flipAnimatable.snapTo(0f)
                            }
                        }
                    },
                    enabled = safeIndex < maxIndex,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("reader_next_page")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next page",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
