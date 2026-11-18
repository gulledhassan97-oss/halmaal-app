package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.data.local.BookEntity
import com.example.ui.adaptive.WidthSizeTier
import com.example.ui.adaptive.rememberAdaptiveSpec
import com.example.ui.state.HalmaalUiState
import com.example.ui.theme.GoldenStar
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.io.File
import java.util.Locale

@Composable
fun MoreBooksScreen(
    uiState: HalmaalUiState,
    getCoverFile: (Long) -> File,
    onBackClick: () -> Unit,
    onBookDetailClick: (BookEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.homeSettings
    val primaryColor = Color(settings.primaryColorHex)
    val darkAccent = Color(settings.darkAccentColorHex)
    val ctaCoral = Color(settings.ctaHighlightHex)
    val canvasBg = Color(settings.canvasBackgroundHex)

    val spotlightBook = uiState.featuredBook
    val allBooks = uiState.filteredBooks

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBg)
            .testTag("more_books_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        val spec = rememberAdaptiveSpec()
        val hPad = spec.horizontalPadding
        val gridMinCellWidth = when (spec.widthTier) {
            WidthSizeTier.NARROW -> 124.dp
            WidthSizeTier.COMPACT -> 146.dp
            WidthSizeTier.MEDIUM -> 164.dp
            WidthSizeTier.EXPANDED -> 182.dp
        }
        val spotlightCoverWidth = when (spec.widthTier) {
            WidthSizeTier.NARROW -> 98.dp
            WidthSizeTier.COMPACT -> 126.dp
            WidthSizeTier.MEDIUM -> 146.dp
            WidthSizeTier.EXPANDED -> 166.dp
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = spec.maxContentWidth)
                .fillMaxWidth()
        ) {
            // Header ("More Book") with accessible 48dp touch target
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = hPad, vertical = if (spec.isShortScreen) 8.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("more_books_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Explore",
                            tint = darkAccent
                        )
                    }
                }

                Text(
                    text = "More Book",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = darkAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.size(48.dp))
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = gridMinCellWidth),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = hPad, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top E-Book Reading Spotlight Card
                if (spotlightBook != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Top E-Book Reading",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = darkAccent
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onBookDetailClick(spotlightBook) },
                                horizontalArrangement = Arrangement.spacedBy(if (spec.isNarrowScreen) 12.dp else 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val coverFile = getCoverFile(spotlightBook.id)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(spotlightBook.accentColorHex),
                                    modifier = Modifier
                                        .width(spotlightCoverWidth)
                                        .aspectRatio(0.70f)
                                        .shadow(8.dp, RoundedCornerShape(14.dp))
                                ) {
                                    if (coverFile.exists()) {
                                        Image(
                                            painter = rememberAsyncImagePainter(model = coverFile),
                                            contentDescription = spotlightBook.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.MenuBook,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(38.dp)
                                            )
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = spotlightBook.title,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = darkAccent,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "by ${spotlightBook.author}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = primaryColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (spec.widthTier >= WidthSizeTier.MEDIUM && spotlightBook.description.isNotBlank()) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = spotlightBook.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = darkAccent.copy(alpha = 0.72f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        repeat(5) {
                                            Icon(
                                                imageVector = Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = GoldenStar,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = String.format(Locale.US, "$%.2f", spotlightBook.price),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = darkAccent
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Button(
                                        onClick = { onBookDetailClick(spotlightBook) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ctaCoral,
                                            contentColor = if (ctaCoral.luminance() > 0.36f) darkAccent else Color.White
                                        ),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("featured_detail_button")
                                    ) {
                                        Text(
                                            text = "Detail",
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = settings.recommendedHeading.ifBlank { "More Recommended" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = darkAccent
                    )
                }

                items(allBooks, key = { "more_${it.id}" }) { book ->
                    val coverFile = getCoverFile(book.id)
                    val owned = uiState.isBookOwned(book.id)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onBookDetailClick(book) }
                            .testTag("more_book_item_${book.id}")
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(book.accentColorHex),
                            shadowElevation = 5.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.70f)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (coverFile.exists()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = coverFile),
                                        contentDescription = book.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Surface(
                                    color = if (owned) Color(0xFF059669) else darkAccent.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = if (owned) "Owned" else String.format(Locale.US, "$%.2f", book.price),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "by ${book.author}",
                            style = MaterialTheme.typography.labelSmall,
                            color = primaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = darkAccent,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
