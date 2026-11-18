package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.data.local.BookEntity
import com.example.ui.adaptive.WidthSizeTier
import com.example.ui.adaptive.rememberAdaptiveSpec
import com.example.ui.state.CatalogFilter
import com.example.ui.state.HalmaalUiState
import com.example.ui.theme.GoldenStar
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.io.File
import java.util.Locale

@Composable
fun StorefrontScreen(
    uiState: HalmaalUiState,
    getCoverFile: (Long) -> File,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onFilterSelected: (CatalogFilter) -> Unit,
    onBookSelected: (BookEntity) -> Unit,
    onOpenMoreBooks: () -> Unit,
    onReadBook: (Long) -> Unit,
    onBuyBook: (BookEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.homeSettings
    val primaryColor = Color(settings.primaryColorHex)
    val darkAccent = Color(settings.darkAccentColorHex)
    val canvasBg = Color(settings.canvasBackgroundHex)
    val books = uiState.filteredBooks

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBg)
            .testTag("storefront_screen")
    ) {
        val spec = rememberAdaptiveSpec()
        val hPad = spec.horizontalPadding
        val gridColumns = spec.catalogGridColumns
        val recommendedCols = spec.recommendedColumns
        val carouselCardWidth = when (spec.widthTier) {
            WidthSizeTier.NARROW -> 110.dp
            WidthSizeTier.COMPACT -> 126.dp
            WidthSizeTier.MEDIUM -> 144.dp
            WidthSizeTier.EXPANDED -> 160.dp
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (spec.isShortScreen) 12.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Pill Search Bar
            if (settings.showSearchBar) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = {
                                Text(
                                    text = "Search books here...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Search",
                                        tint = darkAccent
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = spec.maxContentWidth)
                                .padding(horizontal = hPad)
                                .shadow(2.dp, RoundedCornerShape(28.dp))
                                .testTag("store_search_input")
                        )
                    }
                }
            }

            // 2. Categories Pill Strip
            if (settings.showCategoriesSection) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = spec.maxContentWidth)
                    ) {
                        Text(
                            text = settings.categoriesHeading.ifBlank { "Categories" },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = darkAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = hPad)
                        )
                        Spacer(Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = hPad),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.availableCategories) { category ->
                                val selected = uiState.selectedCategory.equals(category, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (selected) darkAccent else Color.White,
                                    shadowElevation = if (selected) 3.dp else 1.dp,
                                    modifier = Modifier
                                        .clickable { onCategorySelected(category) }
                                        .testTag("category_pill_${category.lowercase().replace(" ", "_")}")
                                ) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (selected) Color.White else darkAccent,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = hPad),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(CatalogFilter.entries) { filter ->
                                val selected = uiState.selectedFilter == filter
                                FilterChip(
                                    selected = selected,
                                    onClick = { onFilterSelected(filter) },
                                    label = {
                                        Text(
                                            text = filter.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color.White.copy(alpha = 0.72f),
                                        selectedContainerColor = primaryColor,
                                        selectedLabelColor = Color.White,
                                        labelColor = darkAccent
                                    ),
                                    border = null,
                                    modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // 3. Trending Books Section (Carousel or Adaptive Grid)
            if (settings.showTrendingSection) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = spec.maxContentWidth)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = hPad),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = settings.trendingHeading.ifBlank { "Trending Books" },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = darkAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = onOpenMoreBooks,
                                modifier = Modifier.testTag("trending_more_button")
                            ) {
                                Text("More", color = primaryColor, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (books.isEmpty()) {
                            EmptyStorefrontCard(
                                isLibraryFilter = uiState.selectedFilter == CatalogFilter.MY_LIBRARY,
                                primaryColor = primaryColor,
                                darkAccent = darkAccent,
                                modifier = Modifier.padding(horizontal = hPad, vertical = 12.dp)
                            )
                        } else if (settings.trendingLayoutStyle == "GRID") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = hPad),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                books.chunked(gridColumns).forEach { rowBooks ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowBooks.forEach { book ->
                                            TrendingPosterCard(
                                                book = book,
                                                coverFile = getCoverFile(book.id),
                                                isOwned = uiState.isBookOwned(book.id),
                                                primaryColor = primaryColor,
                                                darkAccent = darkAccent,
                                                onClick = { onBookSelected(book) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        repeat(gridColumns - rowBooks.size) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = hPad),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(books, key = { "trending_${it.id}" }) { book ->
                                    TrendingPosterCard(
                                        book = book,
                                        coverFile = getCoverFile(book.id),
                                        isOwned = uiState.isBookOwned(book.id),
                                        primaryColor = primaryColor,
                                        darkAccent = darkAccent,
                                        onClick = { onBookSelected(book) },
                                        modifier = Modifier.width(carouselCardWidth)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Curved "More Recommended" Panel (spans edge-to-edge, inner content adapts columns to screen width)
            if (settings.showRecommendedPanel && books.isNotEmpty()) {
                item {
                    Surface(
                        color = primaryColor,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = spec.maxContentWidth)
                                    .padding(horizontal = hPad, vertical = 14.dp)
                                    .padding(bottom = 18.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = darkAccent.copy(alpha = 0.85f),
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clickable(onClick = onOpenMoreBooks)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.KeyboardArrowUp,
                                                contentDescription = "Open More Book",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = settings.recommendedHeading.ifBlank { "More Recommended" },
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = onOpenMoreBooks,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("more_books_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreHoriz,
                                            contentDescription = "View More Books",
                                            tint = Color.White
                                        )
                                    }
                                }

                                Spacer(Modifier.height(6.dp))

                                val recommendedItems = books.take(if (recommendedCols > 1) 6 else 4)
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    recommendedItems.chunked(recommendedCols).forEach { rowBooks ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            rowBooks.forEach { book ->
                                                val owned = uiState.isBookOwned(book.id)
                                                RecommendedHorizontalBookCard(
                                                    book = book,
                                                    coverFile = getCoverFile(book.id),
                                                    isOwned = owned,
                                                    darkAccent = darkAccent,
                                                    primaryColor = primaryColor,
                                                    compact = spec.isNarrowScreen,
                                                    onCardClick = { onBookSelected(book) },
                                                    onActionClick = {
                                                        if (owned) onReadBook(book.id) else onBuyBook(book)
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            repeat(recommendedCols - rowBooks.size) {
                                                Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingPosterCard(
    book: BookEntity,
    coverFile: File,
    isOwned: Boolean,
    primaryColor: Color,
    darkAccent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("book_card_${book.id}")
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(book.accentColorHex),
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (coverFile.exists()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = coverFile),
                        contentDescription = "Cover preview for ${book.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOwned) Color(0xFF059669) else Color.White.copy(alpha = 0.94f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (isOwned) "Read" else String.format(Locale.US, "$%.2f", book.price),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isOwned) Color.White else darkAccent,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(7.dp))
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

@Composable
private fun RecommendedHorizontalBookCard(
    book: BookEntity,
    coverFile: File,
    isOwned: Boolean,
    darkAccent: Color,
    primaryColor: Color,
    compact: Boolean,
    onCardClick: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("recommended_card_${book.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 10.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(book.accentColorHex),
                modifier = Modifier
                    .width(if (compact) 46.dp else 56.dp)
                    .aspectRatio(0.72f)
            ) {
                if (coverFile.exists()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = coverFile),
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = darkAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "by ${book.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = primaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = GoldenStar,
                            modifier = Modifier.size(if (compact) 11.dp else 13.dp)
                        )
                    }
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "${book.totalPages}p",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            val ringSize = if (compact) 48.dp else 54.dp
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(ringSize)
                    .clip(CircleShape)
                    .clickable(onClick = onActionClick)
                    .testTag(if (isOwned) "read_book_button_${book.id}" else "buy_book_button_${book.id}")
            ) {
                CircularProgressIndicator(
                    progress = { if (isOwned) 1f else 0.35f },
                    color = if (isOwned) Color(0xFF059669) else GoldenStar,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 3.5.dp,
                    modifier = Modifier.size(ringSize - 4.dp)
                )
                Text(
                    text = if (isOwned) "Read" else String.format(Locale.US, "$%.0f", book.price),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = darkAccent,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun EmptyStorefrontCard(
    isLibraryFilter: Boolean,
    primaryColor: Color,
    darkAccent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (isLibraryFilter) {
                    "Your library has no matching books yet"
                } else {
                    "No publications matched your search"
                },
                style = MaterialTheme.typography.titleMedium,
                color = darkAccent,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Preview Page 1 for free or unlock full books via ZAAD & eDahab.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
