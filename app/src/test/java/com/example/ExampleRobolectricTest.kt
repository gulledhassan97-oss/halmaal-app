package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BookEntity
import com.example.data.local.DEFAULT_WELCOME_TITLE
import com.example.data.local.HalmaalDatabase
import com.example.data.local.HomeScreenSettingsEntity
import com.example.data.remote.GatewayVerificationResult
import com.example.data.remote.PaymentGateway
import com.example.data.remote.SifaloPayGateway
import com.example.data.repository.AuthResult
import com.example.data.repository.HalmaalRepository
import com.example.data.storage.BookPageStorage
import com.example.ui.screens.DetailBookScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.HalmaalTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: HalmaalDatabase
    private lateinit var repository: HalmaalRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = HalmaalDatabase.createInMemory(context)
        repository = HalmaalRepository(
            database = db,
            pageStorage = BookPageStorage(context),
            sifaloPayGateway = SifaloPayGateway()
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun appNameMatchesHalmaal() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Hal-maal", appName)
    }

    @Test
    fun registrationOtpVerificationAndSifaloCheckoutFlow() = runTest {
        repository.ensureSeeded()
        val books = repository.allBooks.first()
        assertTrue(books.isNotEmpty())
        val firstBook = books.first()

        // Register user & verify OTP
        val regResult = repository.registerUser("reader@halmaal.com", "pass1234")
        assertTrue(regResult is AuthResult.RegisteredPendingOtp)
        val otp = (regResult as AuthResult.RegisteredPendingOtp).sandboxOtpCode

        // Login before verification must fail
        val preVerifyLogin = repository.login("reader@halmaal.com", "pass1234")
        assertTrue(preVerifyLogin is AuthResult.Error)

        // Verify OTP
        val verifyRes = repository.verifyOtp("reader@halmaal.com", otp)
        assertTrue(verifyRes is AuthResult.Verified)

        // Login after verification succeeds
        val loginRes = repository.login("reader@halmaal.com", "pass1234")
        assertTrue(loginRes is AuthResult.LoggedIn)
        val user = (loginRes as AuthResult.LoggedIn).user

        // Page 1 is public preview; Page 2 is locked before purchase
        assertTrue(repository.canAccessPage(user, firstBook.id, 1))
        assertFalse(repository.canAccessPage(user, firstBook.id, 2))

        // Initiate ZAAD checkout using 9-digit local phone (auto-standardized to 252...)
        val checkoutRes = repository.initiateBookCheckout(
            userId = user.id,
            book = firstBook,
            gateway = PaymentGateway.ZAAD,
            phone = "0634445566",
            currency = "USD"
        )
        assertTrue(checkoutRes.isSuccess)
        val (order, _) = checkoutRes.getOrThrow()
        assertEquals("252634445566", order.phone)
        assertTrue(order.sifalopayRef.startsWith("ORD-"))

        val verifyPayment = repository.verifyAndCompleteOrder(
            orderId = order.sifalopayRef,
            sid = order.sid,
            isSandboxMode = true
        )
        assertTrue(verifyPayment is GatewayVerificationResult.VerifiedPaid)

        // Page 2 is now unlocked for read-only Flipbook viewing
        assertTrue(repository.canAccessPage(user, firstBook.id, 2))
    }

    @Test
    fun buildBookSpreadsPairsCoverAndInnerPagesCorrectly() {
        val singlePage = com.example.ui.components.buildBookSpreads(1)
        assertEquals(
            listOf(com.example.ui.components.BookSpread(leftPage = null, rightPage = 1)),
            singlePage
        )

        val fivePages = com.example.ui.components.buildBookSpreads(5)
        assertEquals(
            listOf(
                com.example.ui.components.BookSpread(leftPage = null, rightPage = 1),
                com.example.ui.components.BookSpread(leftPage = 2, rightPage = 3),
                com.example.ui.components.BookSpread(leftPage = 4, rightPage = 5)
            ),
            fivePages
        )

        val sixPages = com.example.ui.components.buildBookSpreads(6)
        assertEquals(4, sixPages.size)
        assertEquals(
            com.example.ui.components.BookSpread(leftPage = 6, rightPage = null),
            sixPages.last()
        )
    }

    @Test
    fun superAdminCanUploadAndUpdateBooks() = runTest {
        repository.ensureSeeded()

        val adminLogin = repository.login(
            HalmaalRepository.ADMIN_EMAIL,
            HalmaalRepository.ADMIN_DEFAULT_PASS
        )
        assertTrue(adminLogin is AuthResult.LoggedIn)
        val superAdmin = (adminLogin as AuthResult.LoggedIn).user
        assertEquals("super_admin", superAdmin.role)

        // Upload a new book via manuscript edition
        val uploadResult = repository.uploadAndRasterizeBook(
            title = "Somali Maritime History",
            author = "Dr. Xasan Nuur",
            description = "Trade routes across the Indian Ocean.",
            price = 18.50,
            category = "History",
            accentColorHex = 0xFF1E3A8A,
            pdfUri = null,
            manuscriptText = "Opening chapter on monsoon navigation.\n\nSecond paragraph.\n\nThird chapter on coastal ports."
        ) { _, _ -> }
        assertTrue(uploadResult.isSuccess)
        val created = uploadResult.getOrThrow()
        assertTrue(created.totalPages >= 2)
        assertTrue(repository.canAccessPage(superAdmin, created.id, 2))
        assertTrue(repository.getPageFile(created.id, 1).exists())

        // Update the uploaded book's title, price, category, and content
        val updateResult = repository.updateBook(
            bookId = created.id,
            title = "Somali Maritime History (2nd Edition)",
            author = "Dr. Xasan Nuur",
            description = "Expanded historical archive.",
            price = 21.00,
            category = "Heritage & Poetry",
            accentColorHex = 0xFF9A3412,
            newPdfUri = null,
            newManuscriptText = "Updated foreword.\n\nSecond paragraph.\n\nThird section.\n\nFourth section.\n\nFifth section."
        )
        assertTrue(updateResult.isSuccess)
        val updated = updateResult.getOrThrow()
        assertEquals("Somali Maritime History (2nd Edition)", updated.title)
        assertEquals(21.00, updated.price, 0.001)
        assertEquals("Heritage & Poetry", updated.category)
        assertEquals(0xFF9A3412, updated.accentColorHex)

        val fetched = repository.getBookById(created.id)!!
        assertEquals("Somali Maritime History (2nd Edition)", fetched.title)
        assertEquals(21.00, fetched.price, 0.001)
    }

    @Test
    fun adminCredentialsAndHomeScreenSettingsPersistAndReset() = runTest {
        repository.ensureSeeded()

        val login = repository.login("Admin", "Halmaal@@50")
        assertTrue(login is AuthResult.LoggedIn)
        val admin = (login as AuthResult.LoggedIn).user
        assertEquals("super_admin", admin.role)

        val defaultSettings = repository.homeSettings.first()
        assertEquals(0xFF0B2B5CL, defaultSettings.primaryColorHex)
        assertTrue(defaultSettings.showWelcomeScreen)
        assertEquals(DEFAULT_WELCOME_TITLE, defaultSettings.welcomeTitle)

        val customized = defaultSettings.copy(
            brandTitle = "Hal-maal Press",
            showWelcomeScreen = false,
            trendingHeading = "Featured Releases",
            trendingLayoutStyle = "GRID",
            primaryColorHex = 0xFF0F766EL
        )
        repository.saveHomeSettings(customized)

        val saved = repository.homeSettings.first()
        assertEquals("Hal-maal Press", saved.brandTitle)
        assertFalse(saved.showWelcomeScreen)
        assertEquals("Featured Releases", saved.trendingHeading)
        assertEquals("GRID", saved.trendingLayoutStyle)
        assertEquals(0xFF0F766EL, saved.primaryColorHex)

        repository.resetHomeSettings()
        val reset = repository.homeSettings.first()
        assertEquals("Hal-maal", reset.brandTitle)
        assertTrue(reset.showWelcomeScreen)
        assertEquals(0xFF0B2B5CL, reset.primaryColorHex)
    }

    @Test
    @Config(sdk = [34], qualifiers = "w720dp-h360dp-land")
    fun adaptiveLayoutsFitNarrowLandscapeAndTabletDevices() {
        val settings = HomeScreenSettingsEntity()
        val sampleBook = BookEntity(
            id = 1L,
            title = "Hal-maal Heritage",
            author = "Axmed Cali",
            description = "Digital publishing archive.",
            price = 14.99,
            category = "Heritage & Poetry",
            pdfFilename = "heritage.pdf",
            totalPages = 12,
            accentColorHex = 0xFF0B2B5CL
        )
        val dummyCover = java.io.File(context.cacheDir, "nonexistent_cover.webp")

        // Landscape phone (720dp x 360dp) renders 2-pane Detail cleanly with CTAs visible
        composeTestRule.setContent {
            HalmaalTheme {
                DetailBookScreen(
                    book = sampleBook,
                    settings = settings,
                    coverFile = dummyCover,
                    isOwned = false,
                    onBackClick = {},
                    onPreviewOrRead = {},
                    onBuyClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("detail_book_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detail_buy_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detail_preview_button").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w320dp-h520dp-port")
    fun welcomeScreenRendersOnCompactNarrowPhone() {
        val settings = HomeScreenSettingsEntity()
        composeTestRule.setContent {
            HalmaalTheme {
                WelcomeScreen(
                    settings = settings,
                    onStartClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("welcome_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_headline_text").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(DEFAULT_WELCOME_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_start_button").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w1024dp-h768dp-land")
    fun welcomeScreenRendersOnTablet() {
        val settings = HomeScreenSettingsEntity()
        composeTestRule.setContent {
            HalmaalTheme {
                WelcomeScreen(
                    settings = settings,
                    onStartClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("welcome_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_headline_text").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(DEFAULT_WELCOME_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_start_button").assertIsDisplayed()
    }

    @Test
    fun zaadPhoneNormalizationHandlesCommonSomaliFormats() {
        assertEquals(
            "252634445566",
            com.example.data.repository.SecurityUtils.standardizeSomaliPhone("0634445566")
        )
        assertEquals(
            "252634445566",
            com.example.data.repository.SecurityUtils.standardizeSomaliPhone("634445566")
        )
        assertEquals(
            "252634445566",
            com.example.data.repository.SecurityUtils.standardizeSomaliPhone("+252 63 444 5566")
        )
        assertEquals(
            "252634445566",
            com.example.data.repository.SecurityUtils.standardizeSomaliPhone("2520634445566")
        )
        assertTrue(com.example.data.repository.SecurityUtils.isValidSomaliMobilePhone("2520634445566"))
    }

    @Test
    fun zaadCheckoutSheetShowsInlineErrorAndRespondsOnPayClick() {
        val sampleBook = BookEntity(
            id = 1L,
            title = "Hal-maal Heritage",
            author = "Axmed Cali",
            description = "Digital publishing archive.",
            price = 14.99,
            category = "Heritage & Poetry",
            pdfFilename = "heritage.pdf",
            totalPages = 12,
            accentColorHex = 0xFF0B2B5CL
        )
        var payClicked = false
        val errorState = com.example.ui.state.CheckoutModalState(
            book = sampleBook,
            selectedGateway = PaymentGateway.ZAAD,
            phone = "",
            errorMessage = "Enter a valid wallet number (e.g. 252634000000 or 634000000)."
        )

        composeTestRule.setContent {
            HalmaalTheme {
                com.example.ui.components.SifaloPayCheckoutSheet(
                    state = errorState,
                    isLiveKeyConfigured = false,
                    onDismiss = {},
                    onSelectGateway = {},
                    onSelectCurrency = {},
                    onPhoneChange = {},
                    onInitiatePayment = { payClicked = true },
                    onVerifyAndUnlock = {},
                    onOpenReaderNow = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("checkout_error_banner").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("checkout_pay_now_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("checkout_pay_now_button").performClick()
        assertTrue(payClicked)
    }
}
