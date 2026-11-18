package com.example.data.repository

import android.net.Uri
import com.example.data.local.BookDao
import com.example.data.local.BookEntity
import com.example.data.local.BookSalesStat
import com.example.data.local.HalmaalDatabase
import com.example.data.local.HomeScreenSettingsEntity
import com.example.data.local.HomeSettingsDao
import com.example.data.local.LibraryDao
import com.example.data.local.OrderDao
import com.example.data.local.OrderEntity
import com.example.data.local.UserDao
import com.example.data.local.UserEntity
import com.example.data.local.UserLibraryEntity
import com.example.data.remote.GatewayCheckoutResult
import com.example.data.remote.GatewayVerificationResult
import com.example.data.remote.PaymentGateway
import com.example.data.remote.SifaloPayGateway
import com.example.data.storage.BookPageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

sealed interface AuthResult {
    data class RegisteredPendingOtp(val email: String, val sandboxOtpCode: String) : AuthResult
    data class LoggedIn(val user: UserEntity) : AuthResult
    data class Verified(val message: String) : AuthResult
    data class Error(val message: String) : AuthResult
}

data class StarterBookSeed(
    val title: String,
    val author: String,
    val description: String,
    val price: Double,
    val category: String,
    val accentColorHex: Long,
    val chapters: List<Pair<String, String>>
)

class HalmaalRepository(
    private val database: HalmaalDatabase,
    private val pageStorage: BookPageStorage,
    private val sifaloPayGateway: SifaloPayGateway
) {
    private val userDao: UserDao = database.userDao()
    private val bookDao: BookDao = database.bookDao()
    private val orderDao: OrderDao = database.orderDao()
    private val libraryDao: LibraryDao = database.libraryDao()
    private val homeSettingsDao: HomeSettingsDao = database.homeSettingsDao()

    companion object {
        const val ADMIN_EMAIL = "Admin"
        const val ADMIN_DEFAULT_PASS = "Halmaal@@50"
        private const val LEGACY_ADMIN_EMAIL = "admin@halmaal.com"
        private const val OTP_VALIDITY_MS = 10 * 60 * 1000L // 10 minutes
    }

    val allBooks: Flow<List<BookEntity>> = bookDao.observeAllBooks()
    val userCount: Flow<Int> = userDao.observeUserCount()
    val paidOrdersCount: Flow<Int> = orderDao.observePaidOrdersCount()
    val totalRevenue: Flow<Double> = orderDao.observeTotalRevenue()
    val bookSalesStats: Flow<List<BookSalesStat>> = orderDao.observeBookSalesStats()
    val homeSettings: Flow<HomeScreenSettingsEntity> = homeSettingsDao.observeSettings()

    val isLivePaymentKeyConfigured: Boolean
        get() = sifaloPayGateway.isConfiguredWithLiveCredentials

    fun observeOwnedBookIds(userId: Long): Flow<List<Long>> = libraryDao.observeOwnedBookIds(userId)

    fun observeOrdersForUser(userId: Long): Flow<List<OrderEntity>> = orderDao.observeOrdersForUser(userId)

    suspend fun getBookById(bookId: Long): BookEntity? = bookDao.getBookById(bookId)

    fun getPageFile(bookId: Long, pageNum: Int): File = pageStorage.getPageFile(bookId, pageNum)

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        database.runInTransaction {
            val primaryAdmin = userDao.getByEmail(ADMIN_EMAIL)
            if (primaryAdmin == null) {
                userDao.insert(
                    UserEntity(
                        email = ADMIN_EMAIL,
                        passwordHash = SecurityUtils.hashSecret(ADMIN_DEFAULT_PASS),
                        isActive = true,
                        role = "super_admin"
                    )
                )
            }
            if (userDao.getByEmail(LEGACY_ADMIN_EMAIL) == null) {
                userDao.insert(
                    UserEntity(
                        email = LEGACY_ADMIN_EMAIL,
                        passwordHash = SecurityUtils.hashSecret(ADMIN_DEFAULT_PASS),
                        isActive = true,
                        role = "super_admin"
                    )
                )
            }

            val existingSettings = database.readableDatabase.rawQuery(
                "SELECT 1 FROM home_settings WHERE id = 1 LIMIT 1",
                null
            ).use { it.moveToFirst() }
            if (!existingSettings) {
                homeSettingsDao.saveSettings(HomeScreenSettingsEntity())
            }

            if (bookDao.getBookCount() == 0) {
                starterCatalog().forEach { seed ->
                    val bookId = bookDao.insertBook(
                        BookEntity(
                            title = seed.title,
                            author = seed.author,
                            description = seed.description,
                            price = seed.price,
                            pdfFilename = "seed_${seed.title.lowercase().replace(" ", "_")}.pdf",
                            totalPages = seed.chapters.size + 1,
                            category = seed.category,
                            accentColorHex = seed.accentColorHex
                        )
                    )
                    val renderedPages = pageStorage.renderStarterBookPages(
                        bookId = bookId,
                        title = seed.title,
                        author = seed.author,
                        description = seed.description,
                        category = seed.category,
                        accentColorHex = seed.accentColorHex,
                        chapters = seed.chapters
                    )
                    bookDao.updateTotalPages(bookId, renderedPages)
                }
            }
        }
    }

    suspend fun saveHomeSettings(settings: HomeScreenSettingsEntity) = withContext(Dispatchers.IO) {
        homeSettingsDao.saveSettings(settings)
    }

    suspend fun resetHomeSettings() = withContext(Dispatchers.IO) {
        homeSettingsDao.saveSettings(HomeScreenSettingsEntity())
    }

    suspend fun registerUser(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (!SecurityUtils.isValidEmail(cleanEmail)) {
            return@withContext AuthResult.Error("Please enter a valid email address.")
        }
        if (password.length < 6) {
            return@withContext AuthResult.Error("Password must be at least 6 characters.")
        }

        val existing = userDao.getByEmail(cleanEmail)
        val otp = SecurityUtils.generateSixDigitCode()
        val otpHash = SecurityUtils.hashSecret(otp)
        val expiresAt = System.currentTimeMillis() + OTP_VALIDITY_MS

        if (existing != null) {
            if (existing.isActive) {
                return@withContext AuthResult.Error("Email is already registered and active.")
            } else {
                userDao.updateVerificationCode(existing.id, otpHash, expiresAt)
                return@withContext AuthResult.RegisteredPendingOtp(cleanEmail, otp)
            }
        }

        userDao.insert(
            UserEntity(
                email = cleanEmail,
                passwordHash = SecurityUtils.hashSecret(password),
                isActive = false,
                verificationCodeHash = otpHash,
                codeExpiresAt = expiresAt,
                role = "user"
            )
        )
        AuthResult.RegisteredPendingOtp(cleanEmail, otp)
    }

    suspend fun verifyOtp(email: String, code: String): AuthResult = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val user = userDao.getByEmail(cleanEmail)
            ?: return@withContext AuthResult.Error("Account not found for $cleanEmail.")

        val storedHash = user.verificationCodeHash
        val expiresAt = user.codeExpiresAt ?: 0L
        if (storedHash == null || System.currentTimeMillis() > expiresAt) {
            return@withContext AuthResult.Error("Verification code expired. Please request a new code.")
        }

        if (!SecurityUtils.verifySecret(code.trim(), storedHash)) {
            return@withContext AuthResult.Error("Invalid 6-digit verification code.")
        }

        userDao.activateUser(user.id)
        AuthResult.Verified("Account activated successfully. You can now sign in.")
    }

    suspend fun login(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val user = userDao.getByEmail(cleanEmail)
            ?: return@withContext AuthResult.Error("Invalid email or password.")

        if (!SecurityUtils.verifySecret(password, user.passwordHash)) {
            return@withContext AuthResult.Error("Invalid email or password.")
        }

        if (!user.isActive) {
            return@withContext AuthResult.Error("Account not activated. Please verify your 6-digit code first.")
        }

        AuthResult.LoggedIn(user)
    }

    suspend fun initiateBookCheckout(
        userId: Long,
        book: BookEntity,
        gateway: PaymentGateway,
        phone: String,
        currency: String = "USD"
    ): Result<Pair<OrderEntity, GatewayCheckoutResult>> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.isValidSomaliMobilePhone(phone)) {
            return@withContext Result.failure(
                IllegalArgumentException("Enter a valid wallet number (e.g. 252634000000 or 634000000).")
            )
        }
        val standardizedPhone = SecurityUtils.standardizeSomaliPhone(phone)
        val orderId = SecurityUtils.generateSifaloOrderId()

        // Never trust UI price: always use canonical price from database BookEntity
        val canonicalBook = bookDao.getBookById(book.id) ?: book

        val gatewayResult = sifaloPayGateway.initiateCheckout(
            orderId = orderId,
            amount = canonicalBook.price,
            currency = currency,
            gateway = gateway,
            standardizedPhone = standardizedPhone
        )

        when (gatewayResult) {
            is GatewayCheckoutResult.Failed -> {
                Result.failure(IllegalStateException(gatewayResult.error))
            }
            is GatewayCheckoutResult.ImmediatePaid -> {
                val order = OrderEntity(
                    userId = userId,
                    bookId = canonicalBook.id,
                    bookTitle = canonicalBook.title,
                    amount = canonicalBook.price,
                    currency = currency,
                    gateway = gateway.id,
                    phone = standardizedPhone,
                    status = "completed",
                    sifalopayRef = orderId,
                    sid = gatewayResult.sid
                )
                val rowId = orderDao.insertOrder(order)
                libraryDao.insertLibraryEntry(
                    UserLibraryEntity(userId = userId, bookId = canonicalBook.id)
                )
                Result.success(order.copy(id = rowId) to gatewayResult)
            }
            is GatewayCheckoutResult.RedirectRequired -> {
                val order = OrderEntity(
                    userId = userId,
                    bookId = canonicalBook.id,
                    bookTitle = canonicalBook.title,
                    amount = canonicalBook.price,
                    currency = currency,
                    gateway = gateway.id,
                    phone = standardizedPhone,
                    status = "pending",
                    sifalopayRef = orderId,
                    sid = gatewayResult.sid
                )
                val rowId = orderDao.insertOrder(order)
                Result.success(order.copy(id = rowId) to gatewayResult)
            }
            is GatewayCheckoutResult.PendingUssd -> {
                val order = OrderEntity(
                    userId = userId,
                    bookId = canonicalBook.id,
                    bookTitle = canonicalBook.title,
                    amount = canonicalBook.price,
                    currency = currency,
                    gateway = gateway.id,
                    phone = standardizedPhone,
                    status = "pending",
                    sifalopayRef = orderId,
                    sid = gatewayResult.sid
                )
                val rowId = orderDao.insertOrder(order)
                Result.success(order.copy(id = rowId) to gatewayResult)
            }
        }
    }

    suspend fun verifyAndCompleteOrder(
        orderId: String,
        sid: String,
        isSandboxMode: Boolean
    ): GatewayVerificationResult = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderByRef(orderId)
            ?: return@withContext GatewayVerificationResult.Failed("Order reference not found.")

        // Idempotency check: if already completed, ensure library entry exists without double-processing
        if (order.status == "completed") {
            libraryDao.insertLibraryEntry(
                UserLibraryEntity(userId = order.userId, bookId = order.bookId)
            )
            return@withContext GatewayVerificationResult.VerifiedPaid(
                orderId = order.sifalopayRef,
                sid = order.sid.ifBlank { sid },
                isSandboxMode = isSandboxMode
            )
        }

        val verification = sifaloPayGateway.verifyPaymentStatus(
            orderId = order.sifalopayRef,
            sid = sid.ifBlank { order.sid },
            isSandboxMode = isSandboxMode
        )
        if (verification is GatewayVerificationResult.VerifiedPaid) {
            orderDao.updateOrderStatus(order.id, "completed", verification.sid)
            // Unlock strictly for in-app read-only Flipbook viewing (no PDF download granted)
            libraryDao.insertLibraryEntry(
                UserLibraryEntity(userId = order.userId, bookId = order.bookId)
            )
        }
        verification
    }

    suspend fun canAccessPage(user: UserEntity?, bookId: Long, pageNum: Int): Boolean {
        if (pageNum <= 1) return true
        if (user == null) return false
        if (user.role == "super_admin" || user.role == "admin") return true
        return libraryDao.isBookOwned(user.id, bookId)
    }

    suspend fun uploadAndRasterizeBook(
        title: String,
        author: String,
        description: String,
        price: Double,
        category: String,
        accentColorHex: Long = 0xFF0F766E,
        pdfUri: Uri?,
        manuscriptText: String? = null,
        onProgress: (Int, Int) -> Unit
    ): Result<BookEntity> = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim()
        val cleanAuthor = author.trim().ifBlank { "Unknown Author" }
        val cleanCategory = category.trim().ifBlank { "Publication" }
        val cleanDesc = description.trim()
        val resolvedFilename = when {
            pdfUri != null -> pageStorage.resolveDisplayName(pdfUri)
            !manuscriptText.isNullOrBlank() -> "manuscript_${cleanTitle.lowercase().replace(" ", "_")}.edition"
            else -> return@withContext Result.failure(
                IllegalArgumentException("Select a PDF file or enter manuscript text.")
            )
        }

        val bookId = bookDao.insertBook(
            BookEntity(
                title = cleanTitle,
                author = cleanAuthor,
                description = cleanDesc,
                price = price,
                pdfFilename = resolvedFilename,
                totalPages = 0,
                category = cleanCategory,
                accentColorHex = accentColorHex
            )
        )

        if (pdfUri != null) {
            val rasterizeResult = pageStorage.rasterizePdfFromUri(bookId, pdfUri, onProgress)
            rasterizeResult.fold(
                onSuccess = { totalPages ->
                    bookDao.updateTotalPages(bookId, totalPages)
                    Result.success(bookDao.getBookById(bookId)!!)
                },
                onFailure = { error ->
                    bookDao.deleteBookById(bookId)
                    pageStorage.deleteBookFiles(bookId)
                    Result.failure(error)
                }
            )
        } else {
            val totalPages = pageStorage.renderManuscriptBookPages(
                bookId = bookId,
                title = cleanTitle,
                author = cleanAuthor,
                description = cleanDesc,
                category = cleanCategory,
                accentColorHex = accentColorHex,
                manuscriptText = manuscriptText.orEmpty()
            )
            onProgress(totalPages, totalPages)
            bookDao.updateTotalPages(bookId, totalPages)
            Result.success(bookDao.getBookById(bookId)!!)
        }
    }

    suspend fun updateBook(
        bookId: Long,
        title: String,
        author: String,
        description: String,
        price: Double,
        category: String,
        accentColorHex: Long,
        newPdfUri: Uri? = null,
        newManuscriptText: String? = null,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<BookEntity> = withContext(Dispatchers.IO) {
        val existing = bookDao.getBookById(bookId)
            ?: return@withContext Result.failure(IllegalArgumentException("Book not found."))

        val cleanTitle = title.trim()
        val cleanAuthor = author.trim().ifBlank { "Unknown Author" }
        val cleanCategory = category.trim().ifBlank { "Publication" }
        val cleanDesc = description.trim()

        var updatedTotalPages = existing.totalPages
        var updatedFilename = existing.pdfFilename

        if (newPdfUri != null) {
            val rasterizeResult = pageStorage.rasterizePdfFromUri(bookId, newPdfUri, onProgress)
            if (rasterizeResult.isFailure) {
                return@withContext Result.failure(
                    rasterizeResult.exceptionOrNull() ?: IllegalStateException("Failed to rasterize replacement PDF.")
                )
            }
            updatedTotalPages = rasterizeResult.getOrThrow()
            updatedFilename = pageStorage.resolveDisplayName(newPdfUri)
        } else if (!newManuscriptText.isNullOrBlank()) {
            updatedTotalPages = pageStorage.renderManuscriptBookPages(
                bookId = bookId,
                title = cleanTitle,
                author = cleanAuthor,
                description = cleanDesc,
                category = cleanCategory,
                accentColorHex = accentColorHex,
                manuscriptText = newManuscriptText
            )
            onProgress(updatedTotalPages, updatedTotalPages)
            updatedFilename = "manuscript_${bookId}.edition"
        } else if (existing.pdfFilename.startsWith("seed_") || existing.pdfFilename.startsWith("manuscript_")) {
            pageStorage.refreshEditorialCover(
                bookId = bookId,
                title = cleanTitle,
                author = cleanAuthor,
                category = cleanCategory,
                accentColorHex = accentColorHex
            )
        }

        val updatedBook = existing.copy(
            title = cleanTitle,
            author = cleanAuthor,
            description = cleanDesc,
            price = price,
            pdfFilename = updatedFilename,
            totalPages = updatedTotalPages,
            category = cleanCategory,
            accentColorHex = accentColorHex
        )
        bookDao.updateBook(updatedBook)
        Result.success(updatedBook)
    }

    suspend fun deleteBook(bookId: Long) = withContext(Dispatchers.IO) {
        bookDao.deleteBookById(bookId)
        libraryDao.deleteEntriesForBook(bookId)
        pageStorage.deleteBookFiles(bookId)
    }

    private fun starterCatalog(): List<StarterBookSeed> = listOf(
        StarterBookSeed(
            title = "Geeska & Suugaanta: Horn of Africa Oral Heritage",
            author = "Dr. Cabdiraxmaan Warsame",
            description = "An illustrated digital anthology tracing Somali poetic meters, pastoral proverbs, and historic coastal trade manuscripts from Zeila to Mogadishu.",
            price = 12.50,
            category = "Heritage & Poetry",
            accentColorHex = 0xFF0F766E,
            chapters = listOf(
                "Prologue: Voices Across the Red Sea" to "For centuries, scholars and mariners along the Gulf of Aden preserved history through metered verse known as Gabay and Jiifto. Every cadence carried lineage, geography, and seasonal memory.\n\nIn this opening chapter, we examine how oral memory evolved alongside early handwritten Arabic-script Somali manuscripts (Wadaad writing) across Berbera, Harar, and Brava.",
                "Chapter I: The Architecture of Gabay" to "Traditional Somali poetry relies on strict alliteration (Xarafraac) sustained from the opening hemistich to the closing couplet.\n\nA single master poem could settle disputes between pastoral encampments, chart monsoon trade routes, or record astronomical calendars used by camel herders across the Nugaal Valley.",
                "Chapter II: Maritime Chronicles of Zeila" to "Archaeological layers at ancient port settlements reveal porcelain from Quanzhou beside frankincense amphorae and hand-bound legal treatises.\n\nMerchants used standardized weights and seasonal dhows timed precisely to the Xagaa and Deyr winds.",
                "Chapter III: Preserving Manuscripts Digitally" to "Hal-maal Digital Publishing partners with regional archivists to rasterize fragile folios into read-only high-contrast editions—ensuring authors and custodians retain full intellectual stewardship."
            )
        ),
        StarterBookSeed(
            title = "Digital Commerce in East Africa",
            author = "Hodan Maxamed Cali",
            description = "A practical guide to mobile money architecture, ZAAD & eDahab merchant ecosystems, micro-lending rails, and resilient fintech design.",
            price = 14.00,
            category = "Technology & Fintech",
            accentColorHex = 0xFF1E3A8A,
            chapters = listOf(
                "1. The Mobile-First Leapfrog" to "Long before widespread card terminals arrived, mobile wallet networks transformed everyday commerce in Hargeisa, Garowe, and Mogadishu.\n\nThrough simple USSD menus and instant SMS settlement, fruit vendors, publishers, and logistics fleets built a cashless economy.",
                "2. Merchant APIs & Webhook Integrity" to "Integrating ZAAD (Telesom) and eDahab (Somtel) through unified gateways like Sifalopay requires strict server-side verification.\n\nNever trust client callbacks alone: always re-query the transaction reference against the gateway status endpoint before unlocking digital goods.",
                "3. Designing Offline-Resilient Apps" to "High-latency cellular corridors demand optimistic UI feedback paired with idempotent transaction references (SF-...) and clear receipt trails for customers.",
                "4. Scaling Somali Digital Publishing" to "By combining $2–$15 mobile money micro-transactions with protected flipbook reading, independent authors can reach readers across the Horn and diaspora instantly."
            )
        ),
        StarterBookSeed(
            title = "Architecture of Ancient Berbera & Taleex",
            author = "Eng. Mustafe Jaamac",
            description = "Architectural field drawings and stone masonry studies of historic coral-rag merchant houses, fortresses, and coastal lighthouses.",
            price = 16.00,
            category = "Architecture",
            accentColorHex = 0xFF9A3412,
            chapters = listOf(
                "Stone, Lime & Sea Breeze" to "Coral-rag limestone harvested along raised reef terraces provided natural thermal mass against coastal heat. Builders bound blocks with slow-cured lime mortar mixed with crushed shell.",
                "Courtyard Proportions & Wind Catchers" to "Multi-story merchant residences in the Shaab and Moscovite quarters oriented tall shuttered windows toward afternoon sea breezes while shading interior majlis halls.",
                "The Stone Towers of Taleex" to "Inland fortifications demonstrate dry-stone engineering and cistern channels designed to capture seasonal flash rainwater across arid limestone plateaus.",
                "Adaptive Reuse Today" to "Modern architects across the Horn of Africa are reviving lime-plaster cooling facades and shaded colonnades for contemporary civic libraries and studios."
            )
        ),
        StarterBookSeed(
            title = "Modern Somali Entrepreneurship",
            author = "Fartuun Axmed Nuur",
            description = "Case studies of renewable energy micro-grids, telecom cooperatives, livestock export logistics, and youth-led tech studios.",
            price = 9.99,
            category = "Business",
            accentColorHex = 0xFF065F46,
            chapters = listOf(
                "Trust Networks & Hagbad Capital" to "Traditional rotating savings circles (Hagbad / Ayuuto) remain one of the highest-trust capital formation engines in the region, boasting near-zero default rates.",
                "Solar Micro-Grids & Cold Chains" to "Distributed solar arrays coupled with lithium storage have slashed electricity costs for coastal fisheries and agro-processing cooperatives.",
                "Logistics From Port to Market" to "Digital manifests and mobile-money escrow allow wholesale buyers in inland towns to settle shipments upon verified arrival.",
                "Building Enduring Institutions" to "Transitioning from founder-led trading houses to audited, technology-enabled enterprises unlocks regional expansion and long-term talent retention."
            )
        )
    )
}
