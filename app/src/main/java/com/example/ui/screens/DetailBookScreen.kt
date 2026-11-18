package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Visibility
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.data.local.BookEntity
import com.example.data.local.HomeScreenSettingsEntity
import com.example.ui.adaptive.rememberAdaptiveSpec
import com.example.ui.theme.GoldenStar
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.io.File
import java.util.Locale

@Composable
fun DetailBookScreen(
    book: BookEntity,
    settings: HomeScreenSettingsEntity,
    coverFile: File,
    isOwned: Boolean,
    onBackClick: () -> Unit,
    onPreviewOrRead: () -> Unit,
    onBuyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = Color(settings.primaryColorHex)
    val darkAccent = Color(settings.darkAccentColorHex)
    val canvasBg = Color(settings.canvasBackgroundHex)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBg)
            .statusBarsPadding()
            .testTag("detail_book_screen")
    ) {
        val spec = rememberAdaptiveSpec()

        if (spec.useTwoPaneLayout) {
            // Wide / Landscape / Foldable / Tablet: Side-by-Side 2-Pane layout
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .navigationBarsPadding()
                        .padding(horizontal = spec.horizontalPadding, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DetailHeaderBar(
                        darkAccent = darkAccent,
                        onBackClick = onBackClick
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DetailFloatingCover(
                            book = book,
                            coverFile = coverFile,
                            modifier = Modifier
                                .heightIn(max = (spec.maxHeight * 0.68f).coerceIn(140.dp, 360.dp))
                                .aspectRatio(0.70f, matchHeightConstraintsFirst = true)
                        )
                    }
                }

                Surface(
                    color = primaryColor,
                    shape = RoundedCornerShape(topStart = 34.dp, bottomStart = 34.dp),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                ) {
                    val infoModifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (spec.isShortScreen) {
                                Modifier.verticalScroll(rememberScrollState())
                            } else {
                                Modifier
                            }
                        )
                        .navigationBarsPadding()
                        .padding(
                            horizontal = if (spec.isShortScreen) 18.dp else 24.dp,
                            vertical = if (spec.isShortScreen) 14.dp else 20.dp
                        )
                    DetailInfoPanelContent(
                        book = book,
                        darkAccent = darkAccent,
                        isOwned = isOwned,
                        isNarrow = spec.isNarrowScreen,
                        isScrollableWholePanel = spec.isShortScreen,
                        onPreviewOrRead = onPreviewOrRead,
                        onBuyClick = onBuyClick,
                        modifier = infoModifier
                    )
                }
            }
        } else if (spec.maxHeight < 540.dp) {
            // Ultra-short portrait / split-screen: full vertical scroll so nothing clips
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                DetailHeaderBar(
                    darkAccent = darkAccent,
                    onBackClick = onBackClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DetailFloatingCover(
                        book = book,
                        coverFile = coverFile,
                        modifier = Modifier
                            .heightIn(max = 170.dp)
                            .aspectRatio(0.70f)
                    )
                }
                Surface(
                    color = primaryColor,
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DetailInfoPanelContent(
                        book = book,
                        darkAccent = darkAccent,
                        isOwned = isOwned,
                        isNarrow = spec.isNarrowScreen,
                        isScrollableWholePanel = true,
                        onPreviewOrRead = onPreviewOrRead,
                        onBuyClick = onBuyClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    )
                }
            }
        } else {
            // Standard Portrait Phone
            val coverStageMaxHeight = (spec.maxHeight * 0.30f).coerceIn(145.dp, 248.dp)
            Column(modifier = Modifier.fillMaxSize()) {
                DetailHeaderBar(
                    darkAccent = darkAccent,
                    onBackClick = onBackClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = coverStageMaxHeight)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DetailFloatingCover(
                        book = book,
                        coverFile = coverFile,
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(0.70f, matchHeightConstraintsFirst = true)
                    )
                }

                Surface(
                    color = primaryColor,
                    shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    DetailInfoPanelContent(
                        book = book,
                        darkAccent = darkAccent,
                        isOwned = isOwned,
                        isNarrow = spec.isNarrowScreen,
                        isScrollableWholePanel = false,
                        onPreviewOrRead = onPreviewOrRead,
                        onBuyClick = onBuyClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(
                                horizontal = if (spec.isNarrowScreen) 18.dp else 24.dp,
                                vertical = 20.dp
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailHeaderBar(
    darkAccent: Color,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
                    .testTag("detail_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = darkAccent
                )
            }
        }

        Text(
            text = "Detail Book",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = darkAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun DetailFloatingCover(
    book: BookEntity,
    coverFile: File,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(book.accentColorHex),
        modifier = modifier.shadow(16.dp, RoundedCornerShape(16.dp))
    ) {
        if (coverFile.exists()) {
            Image(
                painter = rememberAsyncImagePainter(model = coverFile),
                contentDescription = "Cover of ${book.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailInfoPanelContent(
    book: BookEntity,
    darkAccent: Color,
    isOwned: Boolean,
    isNarrow: Boolean,
    isScrollableWholePanel: Boolean,
    onPreviewOrRead: () -> Unit,
    onBuyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format(Locale.US, "$%.2f", book.price),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFFFDE047)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(12.dp))

            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isOwned) Icons.Filled.CheckCircle else Icons.Filled.Bookmark,
                        contentDescription = if (isOwned) "Owned" else "Protected edition",
                        tint = if (isOwned) Color(0xFF059669) else darkAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Frosted 3-Column Book Stats Bar
        Surface(
            color = Color.White.copy(alpha = 0.22f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isNarrow) 10.dp else 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rating",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = GoldenStar,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "4.8",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isNarrow) "Pages" else "Number of pages",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${book.totalPages} Pages",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = book.category.substringBefore(" "),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        val synopsisText = book.description.ifBlank {
            "Read-only protected digital edition on Hal-maal Digital Publishing."
        }
        if (isScrollableWholePanel) {
            Text(
                text = synopsisText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.94f)
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = synopsisText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.94f)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (isOwned) {
            Button(
                onClick = onPreviewOrRead,
                colors = ButtonDefaults.buttonColors(
                    containerColor = darkAccent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("detail_read_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Open Flipbook Reader", fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPreviewOrRead,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = darkAccent
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .testTag("detail_preview_button")
                ) {
                    Icon(
                        Icons.Outlined.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Preview", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Button(
                    onClick = onBuyClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = darkAccent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                    modifier = Modifier
                        .weight(1.25f)
                        .heightIn(min = 52.dp)
                        .testTag("detail_buy_button")
                ) {
                    Icon(
                        Icons.Filled.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Buy Book", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
