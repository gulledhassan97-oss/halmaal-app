package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.AuthModalSheet
import com.example.ui.components.BookEditorSheet
import com.example.ui.components.SifaloPayCheckoutSheet
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.DetailBookScreen
import com.example.ui.screens.FlipbookReaderScreen
import com.example.ui.screens.LibraryOrdersScreen
import com.example.ui.screens.MoreBooksScreen
import com.example.ui.screens.StorefrontScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.HalmaalTheme
import com.example.ui.viewmodel.HalmaalViewModel

object Routes {
    const val STORE = "store"
    const val MORE_BOOKS = "more_books"
    const val LIBRARY = "library"
    const val ADMIN = "admin"
    const val DETAIL = "detail/{bookId}"
    const val READER = "reader/{bookId}"

    fun detail(bookId: Long): String = "detail/$bookId"
    fun reader(bookId: Long): String = "reader/$bookId"
}

private data class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: HalmaalViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            HalmaalTheme(
                primaryColorHex = uiState.homeSettings.primaryColorHex,
                darkAccentColorHex = uiState.homeSettings.darkAccentColorHex,
                ctaHighlightHex = uiState.homeSettings.ctaHighlightHex,
                canvasBackgroundHex = uiState.homeSettings.canvasBackgroundHex
            ) {
                HalmaalApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HalmaalApp(viewModel: HalmaalViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.homeSettings
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isFullScreenRoute = currentRoute?.startsWith("reader/") == true ||
        currentRoute?.startsWith("detail/") == true

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.dismissSnackbar()
        }
    }

    if (settings.showWelcomeScreen && !uiState.welcomeDismissed) {
        WelcomeScreen(
            settings = settings,
            onStartClick = viewModel::dismissWelcomeScreen
        )
        return
    }

    val destinations = buildList {
        add(
            NavDestination(
                route = Routes.STORE,
                label = "Explore",
                selectedIcon = Icons.Filled.Explore,
                unselectedIcon = Icons.Outlined.Explore,
                tag = "nav_explore"
            )
        )
        add(
            NavDestination(
                route = Routes.MORE_BOOKS,
                label = "Reading",
                selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
                unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook,
                tag = "nav_more_books"
            )
        )
        add(
            NavDestination(
                route = Routes.LIBRARY,
                label = "Bookmark",
                selectedIcon = Icons.AutoMirrored.Filled.LibraryBooks,
                unselectedIcon = Icons.AutoMirrored.Outlined.LibraryBooks,
                tag = "nav_library"
            )
        )
        if (uiState.isAdmin) {
            add(
                NavDestination(
                    route = Routes.ADMIN,
                    label = "Admin",
                    selectedIcon = Icons.Filled.AdminPanelSettings,
                    unselectedIcon = Icons.Outlined.AdminPanelSettings,
                    tag = "nav_admin"
                )
            )
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp && !isFullScreenRoute
        val isNarrowWidth = maxWidth < 385.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(settings.canvasBackgroundHex),
            contentWindowInsets = if (isFullScreenRoute) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (!isFullScreenRoute && currentRoute != Routes.MORE_BOOKS) {
                    TopAppBar(
                        title = {
                            Surface(
                                color = Color.Transparent,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color(0xFF001027),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(settings.ctaHighlightHex).copy(alpha = 0.65f)),
                                        modifier = Modifier.size(
                                            width = if (isNarrowWidth) 40.dp else 48.dp,
                                            height = if (isNarrowWidth) 30.dp else 34.dp
                                        )
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.halmaal_brand_logo),
                                            contentDescription = "Hal-maal Logo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(Modifier.width(if (isNarrowWidth) 8.dp else 10.dp))
                                    androidx.compose.foundation.layout.Column {
                                        Text(
                                            text = settings.brandTitle.ifBlank { "Hal-maal" },
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(settings.darkAccentColorHex),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!isNarrowWidth) {
                                            Text(
                                                text = settings.brandSubtitle.ifBlank { "Digital Publishing" },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(settings.primaryColorHex),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        actions = {
                            val user = uiState.currentUser
                            if (user == null) {
                                OutlinedButton(
                                    onClick = viewModel::openLoginModal,
                                    contentPadding = PaddingValues(
                                        horizontal = if (isNarrowWidth) 10.dp else 16.dp,
                                        vertical = 8.dp
                                    ),
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .testTag("header_login_button")
                                ) {
                                    Text("Login", maxLines = 1)
                                }
                                FilledTonalButton(
                                    onClick = viewModel::openRegisterModal,
                                    contentPadding = PaddingValues(
                                        horizontal = if (isNarrowWidth) 10.dp else 16.dp,
                                        vertical = 8.dp
                                    ),
                                    modifier = Modifier
                                        .padding(end = if (isNarrowWidth) 4.dp else 8.dp)
                                        .testTag("header_register_button")
                                ) {
                                    Text("Register", maxLines = 1)
                                }
                            } else {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = Color(settings.primaryColorHex).copy(alpha = 0.14f),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = Color(settings.darkAccentColorHex),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = user.email.substringBefore("@"),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(settings.darkAccentColorHex),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.logout()
                                        if (currentRoute == Routes.ADMIN) {
                                            navigateToTab(Routes.STORE)
                                        }
                                    },
                                    modifier = Modifier.testTag("header_logout_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Sign out",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(settings.canvasBackgroundHex)
                        )
                    )
                }
            },
            bottomBar = {
                if (!isFullScreenRoute && !useNavRail) {
                    NavigationBar(
                        containerColor = Color.White,
                        modifier = Modifier
                            .shadow(10.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    ) {
                        destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(dest.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        containerColor = Color.White
                    ) {
                        Spacer(Modifier.size(8.dp))
                        destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(dest.tag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.STORE,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(Routes.STORE) {
                        StorefrontScreen(
                            uiState = uiState,
                            getCoverFile = { bookId -> viewModel.getPageFile(bookId, 1) },
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onCategorySelected = viewModel::selectCategory,
                            onFilterSelected = viewModel::selectFilter,
                            onBookSelected = { book -> navController.navigate(Routes.detail(book.id)) },
                            onOpenMoreBooks = { navigateToTab(Routes.MORE_BOOKS) },
                            onReadBook = { bookId -> navController.navigate(Routes.reader(bookId)) },
                            onBuyBook = viewModel::openCheckout
                        )
                    }

                    composable(Routes.MORE_BOOKS) {
                        MoreBooksScreen(
                            uiState = uiState,
                            getCoverFile = { bookId -> viewModel.getPageFile(bookId, 1) },
                            onBackClick = { navigateToTab(Routes.STORE) },
                            onBookDetailClick = { book -> navController.navigate(Routes.detail(book.id)) }
                        )
                    }

                    composable(
                        route = Routes.DETAIL,
                        arguments = listOf(navArgument("bookId") { type = NavType.LongType })
                    ) { entry ->
                        val bookId = entry.arguments?.getLong("bookId") ?: -1L
                        val book = uiState.books.find { it.id == bookId }
                        if (book != null) {
                            DetailBookScreen(
                                book = book,
                                settings = settings,
                                coverFile = viewModel.getPageFile(book.id, 1),
                                isOwned = uiState.isBookOwned(book.id),
                                onBackClick = { navController.popBackStack() },
                                onPreviewOrRead = { navController.navigate(Routes.reader(book.id)) },
                                onBuyClick = { viewModel.openCheckout(book) }
                            )
                        } else {
                            LaunchedEffect(Unit) {
                                navController.popBackStack()
                            }
                        }
                    }

                    composable(Routes.LIBRARY) {
                        LibraryOrdersScreen(
                            uiState = uiState,
                            onOpenLogin = viewModel::openLoginModal,
                            onReadBook = { bookId -> navController.navigate(Routes.reader(bookId)) }
                        )
                    }

                    composable(Routes.ADMIN) {
                        if (uiState.isAdmin) {
                            AdminDashboardScreen(
                                uiState = uiState,
                                getCoverFile = { bookId -> viewModel.getPageFile(bookId, 1) },
                                onOpenUploadBook = viewModel::openCreateBookEditor,
                                onOpenEditBook = viewModel::openEditBookEditor,
                                onPreviewBook = { bookId -> navController.navigate(Routes.reader(bookId)) },
                                onClearUploadFeedback = viewModel::clearUploadFeedback,
                                onDeleteBook = viewModel::deleteBook,
                                onSaveHomeSettings = viewModel::saveHomeSettings,
                                onResetHomeSettings = viewModel::resetHomeSettings
                            )
                        } else {
                            LaunchedEffect(Unit) {
                                navigateToTab(Routes.STORE)
                            }
                        }
                    }

                    composable(
                        route = Routes.READER,
                        arguments = listOf(navArgument("bookId") { type = NavType.LongType })
                    ) { entry ->
                        val bookId = entry.arguments?.getLong("bookId") ?: -1L
                        val book = uiState.books.find { it.id == bookId }
                        if (book != null) {
                            FlipbookReaderScreen(
                                book = book,
                                buyerEmail = uiState.currentUser?.email,
                                isOwned = uiState.isBookOwned(book.id),
                                getPageFile = viewModel::getPageFile,
                                onCheckAccess = viewModel::canAccessPage,
                                onBuyToUnlock = { target ->
                                    navController.popBackStack()
                                    viewModel.openCheckout(target)
                                },
                                onCloseReader = { navController.popBackStack() }
                            )
                        } else {
                            LaunchedEffect(Unit) {
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }

    // Login / Register + 6-Digit OTP Sheet
    uiState.authModal?.let { authState ->
        AuthModalSheet(
            state = authState,
            onDismiss = viewModel::closeAuthModal,
            onSwitchMode = viewModel::switchAuthMode,
            onEmailChange = viewModel::updateAuthEmail,
            onPasswordChange = viewModel::updateAuthPassword,
            onOtpChange = viewModel::updateAuthOtp,
            onSubmitLogin = viewModel::submitLogin,
            onSubmitRegister = viewModel::submitRegister,
            onSubmitVerifyOtp = viewModel::submitVerifyOtp,
            onPrefillAdmin = viewModel::prefillAdminCredentials
        )
    }

    // Sifalopay ZAAD / eDahab Checkout Sheet
    uiState.checkoutModal?.let { checkoutState ->
        SifaloPayCheckoutSheet(
            state = checkoutState,
            isLiveKeyConfigured = uiState.isLivePaymentConfigured,
            onDismiss = viewModel::closeCheckout,
            onSelectGateway = viewModel::selectCheckoutGateway,
            onSelectCurrency = viewModel::selectCheckoutCurrency,
            onPhoneChange = viewModel::updateCheckoutPhone,
            onInitiatePayment = viewModel::initiateSifaloPayment,
            onVerifyAndUnlock = viewModel::verifyAndUnlockPayment,
            onOpenReaderNow = { bookId ->
                viewModel.closeCheckout()
                navController.navigate(Routes.reader(bookId))
            }
        )
    }

    // Administrator Book Upload / Update Sheet
    uiState.bookEditorModal?.let { editorState ->
        BookEditorSheet(
            state = editorState,
            onDismiss = viewModel::closeBookEditor,
            onTitleChange = viewModel::updateEditorTitle,
            onAuthorChange = viewModel::updateEditorAuthor,
            onPriceChange = viewModel::updateEditorPrice,
            onCategoryChange = viewModel::updateEditorCategory,
            onDescriptionChange = viewModel::updateEditorDescription,
            onAccentColorSelected = viewModel::updateEditorAccentColor,
            onToggleManuscriptMode = viewModel::toggleEditorManuscriptMode,
            onManuscriptTextChange = viewModel::updateEditorManuscriptText,
            onSelectPdfUri = viewModel::selectEditorPdf,
            onClearSelectedPdf = viewModel::clearEditorPdf,
            onSubmit = viewModel::submitBookEditor
        )
    }
}
