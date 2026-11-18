package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.R
import com.example.data.local.BookEntity
import com.example.data.local.BookSalesStat
import com.example.data.local.HomeScreenSettingsEntity
import com.example.ui.adaptive.rememberAdaptiveSpec
import com.example.ui.components.HomeSettingsCustomizer
import com.example.ui.state.HalmaalUiState
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.io.File
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDashboardScreen(
    uiState: HalmaalUiState,
    getCoverFile: (Long) -> File,
    onOpenUploadBook: () -> Unit,
    onOpenEditBook: (BookEntity) -> Unit,
    onPreviewBook: (Long) -> Unit,
    onClearUploadFeedback: () -> Unit,
    onDeleteBook: (BookEntity) -> Unit,
    onSaveHomeSettings: (HomeScreenSettingsEntity) -> Unit,
    onResetHomeSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val adminState = uiState.adminState
    val primaryColor = Color(uiState.homeSettings.primaryColorHex)
    val darkAccent = Color(uiState.homeSettings.darkAccentColorHex)

    var selectedAdminTab by remember { mutableIntStateOf(0) } // 0 = Books & Upload, 1 = HomeScreen Settings
    var catalogQuery by remember { mutableStateOf("") }
    var bookToDelete by remember { mutableStateOf<BookEntity?>(null) }

    val statsByBookId = remember(adminState.salesPerBook) {
        adminState.salesPerBook.associateBy { it.bookId }
    }
    val managedBooks = remember(uiState.books, catalogQuery) {
        if (catalogQuery.isBlank()) {
            uiState.books
        } else {
            uiState.books.filter { book ->
                book.title.contains(catalogQuery, ignoreCase = true) ||
                    book.author.contains(catalogQuery, ignoreCase = true) ||
                    book.category.contains(catalogQuery, ignoreCase = true)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("super_admin_dashboard"),
        contentAlignment = Alignment.TopCenter
    ) {
        val spec = rememberAdaptiveSpec()
        val hPad = spec.horizontalPadding
        val bookGridColumns = if (spec.maxWidth >= 780.dp) 2 else 1

        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = spec.maxContentWidth)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = hPad, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Administrator Hero Banner + Upload Trigger
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(primaryColor, darkAccent)
                            )
                        )
                        .padding(if (spec.isNarrowScreen) 14.dp else 18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = Color(0xFFFDE047),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "ADMIN (${uiState.currentUser?.email ?: "Admin"})",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            Surface(
                                color = Color(0xFF001027),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFD99B1E).copy(alpha = 0.7f)),
                                modifier = Modifier.size(width = 56.dp, height = 34.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.halmaal_brand_logo),
                                    contentDescription = "Hal-maal Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Catalog & HomeScreen Control",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Upload PDFs, update books, and customize the entire HomeScreen layout & branding.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                        Spacer(Modifier.height(14.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onOpenUploadBook,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = darkAccent
                                ),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .weight(1f, fill = true)
                                    .widthIn(min = 148.dp)
                                    .heightIn(min = 48.dp)
                                    .testTag("super_admin_upload_book_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Upload Book", fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { selectedAdminTab = if (selectedAdminTab == 0) 1 else 0 },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_quick_toggle_homescreen")
                            ) {
                                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (selectedAdminTab == 0) "Home Settings" else "Books",
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Section Switcher Tabs (FlowRow wraps gracefully on very narrow screens)
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedAdminTab == 0,
                        onClick = { selectedAdminTab = 0 },
                        label = {
                            Text(
                                text = "Books & Analytics (${uiState.books.size})",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = darkAccent,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f, fill = !spec.isNarrowScreen)
                            .testTag("admin_tab_books")
                    )
                    FilterChip(
                        selected = selectedAdminTab == 1,
                        onClick = { selectedAdminTab = 1 },
                        label = {
                            Text(
                                text = "HomeScreen Settings",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = darkAccent,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f, fill = !spec.isNarrowScreen)
                            .testTag("admin_tab_home_settings")
                    )
                }
            }

            if (selectedAdminTab == 1) {
                item {
                    HomeSettingsCustomizer(
                        currentSettings = uiState.homeSettings,
                        books = uiState.books,
                        onSaveSettings = onSaveHomeSettings,
                        onResetSettings = onResetHomeSettings
                    )
                }
            } else {
                // Analytics KPIs
                item {
                    if (spec.maxWidth >= 560.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AnalyticsMetricCard("Catalog Books", adminState.totalBooks.toString(), Icons.Filled.MenuBook, primaryColor, Modifier.weight(1f))
                            AnalyticsMetricCard("Users", adminState.totalUsers.toString(), Icons.Filled.Group, darkAccent, Modifier.weight(1f))
                            AnalyticsMetricCard("Paid Orders", adminState.paidOrders.toString(), Icons.Filled.ShoppingCartCheckout, Color(uiState.homeSettings.ctaHighlightHex), Modifier.weight(1f))
                            AnalyticsMetricCard("Revenue", String.format(Locale.US, "$%.2f", adminState.totalRevenue), Icons.Filled.AttachMoney, Color(0xFF059669), Modifier.weight(1f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AnalyticsMetricCard("Catalog Books", adminState.totalBooks.toString(), Icons.Filled.MenuBook, primaryColor, Modifier.weight(1f))
                                AnalyticsMetricCard("Users", adminState.totalUsers.toString(), Icons.Filled.Group, darkAccent, Modifier.weight(1f))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AnalyticsMetricCard("Paid Orders", adminState.paidOrders.toString(), Icons.Filled.ShoppingCartCheckout, Color(uiState.homeSettings.ctaHighlightHex), Modifier.weight(1f))
                                AnalyticsMetricCard("Revenue", String.format(Locale.US, "$%.2f", adminState.totalRevenue), Icons.Filled.AttachMoney, Color(0xFF059669), Modifier.weight(1f))
                            }
                        }
                    }
                }

                adminState.uploadSuccess?.let { msg ->
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = onClearUploadFeedback, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Manage & Update Books (${managedBooks.size})",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        OutlinedTextField(
                            value = catalogQuery,
                            onValueChange = { catalogQuery = it },
                            placeholder = {
                                Text(
                                    text = "Search catalog by title, author, or category...",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("super_admin_search_input")
                        )
                    }
                }

                if (managedBooks.isEmpty()) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("No matching books in catalog", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                } else {
                    items(managedBooks.chunked(bookGridColumns), key = { row -> "admin_row_${row.first().id}" }) { rowBooks ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowBooks.forEach { book ->
                                SuperAdminBookRowCard(
                                    book = book,
                                    stat = statsByBookId[book.id],
                                    coverFile = getCoverFile(book.id),
                                    onUpdateClick = { onOpenEditBook(book) },
                                    onPreviewClick = { onPreviewBook(book.id) },
                                    onDeleteClick = { bookToDelete = book },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(bookGridColumns - rowBooks.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    bookToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Delete Publication") },
            text = { Text("Remove \"${target.title}\" and all rasterized pages? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBook(target)
                        bookToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SuperAdminBookRowCard(
    book: BookEntity,
    stat: BookSalesStat?,
    coverFile: File,
    onUpdateClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_book_card_${book.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = Color(book.accentColorHex),
                    modifier = Modifier.size(width = 60.dp, height = 84.dp)
                ) {
                    if (coverFile.exists()) {
                        Image(
                            painter = rememberAsyncImagePainter(model = coverFile),
                            contentDescription = "${book.title} cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Color.White)
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            color = Color(book.accentColorHex).copy(alpha = 0.14f),
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                text = book.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(book.accentColorHex),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = String.format(Locale.US, "$%.2f", book.price),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${book.author} • ${book.totalPages} pages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = String.format(
                            Locale.US,
                            "%d sales • $%.2f revenue • %s",
                            stat?.sales ?: 0,
                            stat?.bookRevenue ?: 0.0,
                            book.pdfFilename
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onUpdateClick,
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("admin_edit_book_${book.id}")
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Update Book", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = onPreviewClick,
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("admin_preview_book_${book.id}")
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Read", maxLines = 1)
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("admin_delete_book_${book.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete ${book.title}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsMetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
