package com.example.data.local

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class BookSalesStat(
    val bookId: Long,
    val title: String,
    val author: String,
    val price: Double,
    val sales: Int,
    val bookRevenue: Double
)

class UserDao(private val dbHelper: HalmaalDatabase) {

    suspend fun getByEmail(email: String): UserEntity? = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.rawQuery(
            "SELECT * FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1",
            arrayOf(email)
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toUserEntity() else null
        }
    }

    suspend fun getById(id: Long): UserEntity? = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.rawQuery(
            "SELECT * FROM users WHERE id = ? LIMIT 1",
            arrayOf(id.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toUserEntity() else null
        }
    }

    suspend fun insert(user: UserEntity): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("email", user.email)
            put("passwordHash", user.passwordHash)
            put("isActive", if (user.isActive) 1 else 0)
            put("verificationCodeHash", user.verificationCodeHash)
            put("codeExpiresAt", user.codeExpiresAt)
            put("role", user.role)
            put("createdAt", user.createdAt)
        }
        val id = dbHelper.writableDatabase.insertOrThrow("users", null, values)
        dbHelper.notifyUsersChanged()
        id
    }

    suspend fun activateUser(userId: Long) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("isActive", 1)
            putNull("verificationCodeHash")
            putNull("codeExpiresAt")
        }
        dbHelper.writableDatabase.update("users", values, "id = ?", arrayOf(userId.toString()))
        dbHelper.notifyUsersChanged()
    }

    suspend fun updateVerificationCode(userId: Long, codeHash: String, expiresAt: Long) =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put("verificationCodeHash", codeHash)
                put("codeExpiresAt", expiresAt)
            }
            dbHelper.writableDatabase.update("users", values, "id = ?", arrayOf(userId.toString()))
            dbHelper.notifyUsersChanged()
        }

    fun observeUserCount(): Flow<Int> = dbHelper.usersTick.map {
        withContext(Dispatchers.IO) {
            dbHelper.readableDatabase.rawQuery("SELECT COUNT(*) FROM users", null).use { c ->
                if (c.moveToFirst()) c.getInt(0) else 0
            }
        }
    }

    private fun Cursor.toUserEntity(): UserEntity = UserEntity(
        id = getLong(getColumnIndexOrThrow("id")),
        email = getString(getColumnIndexOrThrow("email")),
        passwordHash = getString(getColumnIndexOrThrow("passwordHash")),
        isActive = getInt(getColumnIndexOrThrow("isActive")) == 1,
        verificationCodeHash = getStringOrNull("verificationCodeHash"),
        codeExpiresAt = getLongOrNull("codeExpiresAt"),
        role = getString(getColumnIndexOrThrow("role")),
        createdAt = getLong(getColumnIndexOrThrow("createdAt"))
    )
}

class BookDao(private val dbHelper: HalmaalDatabase) {

    fun observeAllBooks(): Flow<List<BookEntity>> = dbHelper.booksTick.map {
        withContext(Dispatchers.IO) {
            val list = mutableListOf<BookEntity>()
            dbHelper.readableDatabase.rawQuery(
                "SELECT * FROM books ORDER BY createdAt DESC",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(cursor.toBookEntity())
                }
            }
            list
        }
    }

    suspend fun getBookCount(): Int = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.rawQuery("SELECT COUNT(*) FROM books", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    suspend fun getBookById(bookId: Long): BookEntity? = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.rawQuery(
            "SELECT * FROM books WHERE id = ? LIMIT 1",
            arrayOf(bookId.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toBookEntity() else null
        }
    }

    suspend fun insertBook(book: BookEntity): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("title", book.title)
            put("author", book.author)
            put("description", book.description)
            put("price", book.price)
            put("pdfFilename", book.pdfFilename)
            put("totalPages", book.totalPages)
            put("category", book.category)
            put("accentColorHex", book.accentColorHex)
            put("createdAt", book.createdAt)
        }
        val id = dbHelper.writableDatabase.insertWithOnConflict(
            "books",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        dbHelper.notifyBooksChanged()
        id
    }

    suspend fun updateTotalPages(bookId: Long, totalPages: Int) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply { put("totalPages", totalPages) }
        dbHelper.writableDatabase.update("books", values, "id = ?", arrayOf(bookId.toString()))
        dbHelper.notifyBooksChanged()
    }

    suspend fun updateBook(book: BookEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("title", book.title)
            put("author", book.author)
            put("description", book.description)
            put("price", book.price)
            put("pdfFilename", book.pdfFilename)
            put("totalPages", book.totalPages)
            put("category", book.category)
            put("accentColorHex", book.accentColorHex)
        }
        dbHelper.writableDatabase.update("books", values, "id = ?", arrayOf(book.id.toString()))
        dbHelper.notifyBooksChanged()
    }

    suspend fun deleteBookById(bookId: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete("books", "id = ?", arrayOf(bookId.toString()))
        dbHelper.notifyBooksChanged()
    }

    private fun Cursor.toBookEntity(): BookEntity = BookEntity(
        id = getLong(getColumnIndexOrThrow("id")),
        title = getString(getColumnIndexOrThrow("title")),
        author = getString(getColumnIndexOrThrow("author")),
        description = getString(getColumnIndexOrThrow("description")),
        price = getDouble(getColumnIndexOrThrow("price")),
        pdfFilename = getString(getColumnIndexOrThrow("pdfFilename")),
        totalPages = getInt(getColumnIndexOrThrow("totalPages")),
        category = getString(getColumnIndexOrThrow("category")),
        accentColorHex = getLong(getColumnIndexOrThrow("accentColorHex")),
        createdAt = getLong(getColumnIndexOrThrow("createdAt"))
    )
}

class OrderDao(private val dbHelper: HalmaalDatabase) {

    fun observeOrdersForUser(userId: Long): Flow<List<OrderEntity>> = dbHelper.ordersTick.map {
        withContext(Dispatchers.IO) {
            val list = mutableListOf<OrderEntity>()
            dbHelper.readableDatabase.rawQuery(
                "SELECT * FROM orders WHERE userId = ? ORDER BY createdAt DESC",
                arrayOf(userId.toString())
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(cursor.toOrderEntity())
                }
            }
            list
        }
    }

    suspend fun getOrderByRef(ref: String): OrderEntity? = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.rawQuery(
            "SELECT * FROM orders WHERE sifalopayRef = ? LIMIT 1",
            arrayOf(ref)
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toOrderEntity() else null
        }
    }

    suspend fun insertOrder(order: OrderEntity): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("userId", order.userId)
            put("bookId", order.bookId)
            put("bookTitle", order.bookTitle)
            put("amount", order.amount)
            put("currency", order.currency)
            put("gateway", order.gateway)
            put("phone", order.phone)
            put("status", order.status)
            put("sifalopayRef", order.sifalopayRef)
            put("sid", order.sid)
            put("createdAt", order.createdAt)
        }
        val id = dbHelper.writableDatabase.insertWithOnConflict(
            "orders",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        dbHelper.notifyOrdersChanged()
        id
    }

    suspend fun updateOrderStatus(orderId: Long, status: String, sid: String? = null) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("status", status)
            if (!sid.isNullOrBlank()) put("sid", sid)
        }
        dbHelper.writableDatabase.update("orders", values, "id = ?", arrayOf(orderId.toString()))
        dbHelper.notifyOrdersChanged()
    }

    fun observePaidOrdersCount(): Flow<Int> = dbHelper.ordersTick.map {
        withContext(Dispatchers.IO) {
            dbHelper.readableDatabase.rawQuery(
                "SELECT COUNT(*) FROM orders WHERE status = 'completed'",
                null
            ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
        }
    }

    fun observeTotalRevenue(): Flow<Double> = dbHelper.ordersTick.map {
        withContext(Dispatchers.IO) {
            dbHelper.readableDatabase.rawQuery(
                "SELECT COALESCE(SUM(amount), 0.0) FROM orders WHERE status = 'completed'",
                null
            ).use { c -> if (c.moveToFirst()) c.getDouble(0) else 0.0 }
        }
    }

    fun observeBookSalesStats(): Flow<List<BookSalesStat>> =
        kotlinx.coroutines.flow.combine(dbHelper.booksTick, dbHelper.ordersTick) { _, _ ->
            withContext(Dispatchers.IO) {
                val stats = mutableListOf<BookSalesStat>()
                dbHelper.readableDatabase.rawQuery(
                    """
                    SELECT b.id AS bookId,
                           b.title AS title,
                           b.author AS author,
                           b.price AS price,
                           COUNT(o.id) AS sales,
                           COALESCE(SUM(o.amount), 0.0) AS bookRevenue
                    FROM books b
                    LEFT JOIN orders o ON b.id = o.bookId AND o.status = 'completed'
                    GROUP BY b.id
                    ORDER BY bookRevenue DESC, b.createdAt DESC
                    """.trimIndent(),
                    null
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        stats.add(
                            BookSalesStat(
                                bookId = cursor.getLong(cursor.getColumnIndexOrThrow("bookId")),
                                title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                                author = cursor.getString(cursor.getColumnIndexOrThrow("author")),
                                price = cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                                sales = cursor.getInt(cursor.getColumnIndexOrThrow("sales")),
                                bookRevenue = cursor.getDouble(cursor.getColumnIndexOrThrow("bookRevenue"))
                            )
                        )
                    }
                }
                stats
            }
        }

    private fun Cursor.toOrderEntity(): OrderEntity = OrderEntity(
        id = getLong(getColumnIndexOrThrow("id")),
        userId = getLong(getColumnIndexOrThrow("userId")),
        bookId = getLong(getColumnIndexOrThrow("bookId")),
        bookTitle = getString(getColumnIndexOrThrow("bookTitle")),
        amount = getDouble(getColumnIndexOrThrow("amount")),
        currency = getStringOrNull("currency") ?: "USD",
        gateway = getString(getColumnIndexOrThrow("gateway")),
        phone = getString(getColumnIndexOrThrow("phone")),
        status = getString(getColumnIndexOrThrow("status")),
        sifalopayRef = getString(getColumnIndexOrThrow("sifalopayRef")),
        sid = getStringOrNull("sid") ?: "",
        createdAt = getLong(getColumnIndexOrThrow("createdAt"))
    )
}

class LibraryDao(private val dbHelper: HalmaalDatabase) {

    fun observeOwnedBookIds(userId: Long): Flow<List<Long>> = dbHelper.libraryTick.map {
        withContext(Dispatchers.IO) {
            val ids = mutableListOf<Long>()
            dbHelper.readableDatabase.rawQuery(
                "SELECT bookId FROM user_library WHERE userId = ?",
                arrayOf(userId.toString())
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    ids.add(cursor.getLong(0))
                }
            }
            ids
        }
    }

    suspend fun isBookOwned(userId: Long, bookId: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.rawQuery(
            "SELECT 1 FROM user_library WHERE userId = ? AND bookId = ? LIMIT 1",
            arrayOf(userId.toString(), bookId.toString())
        ).use { cursor ->
            cursor.moveToFirst()
        }
    }

    suspend fun insertLibraryEntry(entry: UserLibraryEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("userId", entry.userId)
            put("bookId", entry.bookId)
            put("unlockedAt", entry.unlockedAt)
        }
        dbHelper.writableDatabase.insertWithOnConflict(
            "user_library",
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        )
        dbHelper.notifyLibraryChanged()
    }

    suspend fun deleteEntriesForBook(bookId: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete("user_library", "bookId = ?", arrayOf(bookId.toString()))
        dbHelper.notifyLibraryChanged()
    }
}

class HomeSettingsDao(private val dbHelper: HalmaalDatabase) {

    fun observeSettings(): Flow<HomeScreenSettingsEntity> = dbHelper.settingsTick.map {
        getSettingsOrDefault()
    }

    suspend fun getSettingsOrDefault(): HomeScreenSettingsEntity = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.rawQuery(
            "SELECT * FROM home_settings WHERE id = 1 LIMIT 1",
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toHomeSettingsEntity() else HomeScreenSettingsEntity()
        }
    }

    suspend fun saveSettings(settings: HomeScreenSettingsEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("id", 1)
            put("brandTitle", settings.brandTitle)
            put("brandSubtitle", settings.brandSubtitle)
            put("showWelcomeScreen", if (settings.showWelcomeScreen) 1 else 0)
            put("welcomeTitle", settings.welcomeTitle)
            put("welcomeSubtitle", settings.welcomeSubtitle)
            put("welcomeButtonText", settings.welcomeButtonText)
            put("showSearchBar", if (settings.showSearchBar) 1 else 0)
            put("showCategoriesSection", if (settings.showCategoriesSection) 1 else 0)
            put("showTrendingSection", if (settings.showTrendingSection) 1 else 0)
            put("showRecommendedPanel", if (settings.showRecommendedPanel) 1 else 0)
            put("categoriesHeading", settings.categoriesHeading)
            put("trendingHeading", settings.trendingHeading)
            put("recommendedHeading", settings.recommendedHeading)
            put("trendingLayoutStyle", settings.trendingLayoutStyle)
            if (settings.featuredBookId != null) {
                put("featuredBookId", settings.featuredBookId)
            } else {
                putNull("featuredBookId")
            }
            put("primaryColorHex", settings.primaryColorHex)
            put("darkAccentColorHex", settings.darkAccentColorHex)
            put("ctaHighlightHex", settings.ctaHighlightHex)
            put("canvasBackgroundHex", settings.canvasBackgroundHex)
            put("updatedAt", System.currentTimeMillis())
        }
        dbHelper.writableDatabase.insertWithOnConflict(
            "home_settings",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        dbHelper.notifySettingsChanged()
    }

    private fun Cursor.toHomeSettingsEntity(): HomeScreenSettingsEntity = HomeScreenSettingsEntity(
        id = getInt(getColumnIndexOrThrow("id")),
        brandTitle = getString(getColumnIndexOrThrow("brandTitle")),
        brandSubtitle = getString(getColumnIndexOrThrow("brandSubtitle")),
        showWelcomeScreen = getInt(getColumnIndexOrThrow("showWelcomeScreen")) == 1,
        welcomeTitle = getString(getColumnIndexOrThrow("welcomeTitle")),
        welcomeSubtitle = getString(getColumnIndexOrThrow("welcomeSubtitle")),
        welcomeButtonText = getString(getColumnIndexOrThrow("welcomeButtonText")),
        showSearchBar = getInt(getColumnIndexOrThrow("showSearchBar")) == 1,
        showCategoriesSection = getInt(getColumnIndexOrThrow("showCategoriesSection")) == 1,
        showTrendingSection = getInt(getColumnIndexOrThrow("showTrendingSection")) == 1,
        showRecommendedPanel = getInt(getColumnIndexOrThrow("showRecommendedPanel")) == 1,
        categoriesHeading = getString(getColumnIndexOrThrow("categoriesHeading")),
        trendingHeading = getString(getColumnIndexOrThrow("trendingHeading")),
        recommendedHeading = getString(getColumnIndexOrThrow("recommendedHeading")),
        trendingLayoutStyle = getString(getColumnIndexOrThrow("trendingLayoutStyle")),
        featuredBookId = getLongOrNull("featuredBookId"),
        primaryColorHex = getLong(getColumnIndexOrThrow("primaryColorHex")),
        darkAccentColorHex = getLong(getColumnIndexOrThrow("darkAccentColorHex")),
        ctaHighlightHex = getLong(getColumnIndexOrThrow("ctaHighlightHex")),
        canvasBackgroundHex = getLong(getColumnIndexOrThrow("canvasBackgroundHex")),
        updatedAt = getLong(getColumnIndexOrThrow("updatedAt"))
    )
}

private fun Cursor.getStringOrNull(columnName: String): String? {
    val idx = getColumnIndexOrThrow(columnName)
    return if (isNull(idx)) null else getString(idx)
}

private fun Cursor.getLongOrNull(columnName: String): Long? {
    val idx = getColumnIndexOrThrow(columnName)
    return if (isNull(idx)) null else getLong(idx)
}
