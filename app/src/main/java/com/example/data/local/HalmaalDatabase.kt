package com.example.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class HalmaalDatabase(
    context: Context,
    dbName: String? = DB_NAME
) : SQLiteOpenHelper(context.applicationContext, dbName, null, DB_VERSION) {

    private val _usersTick = MutableStateFlow(0L)
    val usersTick: StateFlow<Long> = _usersTick

    private val _booksTick = MutableStateFlow(0L)
    val booksTick: StateFlow<Long> = _booksTick

    private val _ordersTick = MutableStateFlow(0L)
    val ordersTick: StateFlow<Long> = _ordersTick

    private val _libraryTick = MutableStateFlow(0L)
    val libraryTick: StateFlow<Long> = _libraryTick

    private val _settingsTick = MutableStateFlow(0L)
    val settingsTick: StateFlow<Long> = _settingsTick

    @Volatile
    private var suppressNotifications = false

    private val userDaoInstance by lazy { UserDao(this) }
    private val bookDaoInstance by lazy { BookDao(this) }
    private val orderDaoInstance by lazy { OrderDao(this) }
    private val libraryDaoInstance by lazy { LibraryDao(this) }
    private val homeSettingsDaoInstance by lazy { HomeSettingsDao(this) }

    fun userDao(): UserDao = userDaoInstance
    fun bookDao(): BookDao = bookDaoInstance
    fun orderDao(): OrderDao = orderDaoInstance
    fun libraryDao(): LibraryDao = libraryDaoInstance
    fun homeSettingsDao(): HomeSettingsDao = homeSettingsDaoInstance

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT NOT NULL UNIQUE,
                passwordHash TEXT NOT NULL,
                isActive INTEGER NOT NULL DEFAULT 0,
                verificationCodeHash TEXT,
                codeExpiresAt INTEGER,
                role TEXT NOT NULL DEFAULT 'user',
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS books (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                author TEXT NOT NULL,
                description TEXT NOT NULL,
                price REAL NOT NULL,
                pdfFilename TEXT NOT NULL,
                totalPages INTEGER NOT NULL DEFAULT 0,
                category TEXT NOT NULL DEFAULT 'Literature',
                accentColorHex INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS orders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                userId INTEGER NOT NULL,
                bookId INTEGER NOT NULL,
                bookTitle TEXT NOT NULL,
                amount REAL NOT NULL,
                currency TEXT NOT NULL DEFAULT 'USD',
                gateway TEXT NOT NULL,
                phone TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'pending',
                sifalopayRef TEXT NOT NULL,
                sid TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_library (
                userId INTEGER NOT NULL,
                bookId INTEGER NOT NULL,
                unlockedAt INTEGER NOT NULL,
                PRIMARY KEY (userId, bookId)
            )
            """.trimIndent()
        )
        createHomeSettingsTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE orders ADD COLUMN currency TEXT NOT NULL DEFAULT 'USD'")
            db.execSQL("ALTER TABLE orders ADD COLUMN sid TEXT NOT NULL DEFAULT ''")
        }
        if (oldVersion < 3) {
            createHomeSettingsTable(db)
        }
        if (oldVersion < 4) {
            db.execSQL(
                """
                UPDATE home_settings
                SET primaryColorHex = ${0xFF0B2B5CL},
                    darkAccentColorHex = ${0xFF001027L},
                    ctaHighlightHex = ${0xFFD99B1EL},
                    canvasBackgroundHex = ${0xFFFAF7F0L}
                WHERE id = 1
                """.trimIndent()
            )
        }
        if (oldVersion < 5) {
            db.execSQL(
                """
                UPDATE home_settings
                SET welcomeTitle = 'Buugaagta dhijitaalka ah ee kor u qaada maskaxda, dhisa nolosha.'
                WHERE id = 1
                """.trimIndent()
            )
        }
    }

    private fun createHomeSettingsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS home_settings (
                id INTEGER PRIMARY KEY,
                brandTitle TEXT NOT NULL,
                brandSubtitle TEXT NOT NULL,
                showWelcomeScreen INTEGER NOT NULL DEFAULT 1,
                welcomeTitle TEXT NOT NULL,
                welcomeSubtitle TEXT NOT NULL,
                welcomeButtonText TEXT NOT NULL,
                showSearchBar INTEGER NOT NULL DEFAULT 1,
                showCategoriesSection INTEGER NOT NULL DEFAULT 1,
                showTrendingSection INTEGER NOT NULL DEFAULT 1,
                showRecommendedPanel INTEGER NOT NULL DEFAULT 1,
                categoriesHeading TEXT NOT NULL,
                trendingHeading TEXT NOT NULL,
                recommendedHeading TEXT NOT NULL,
                trendingLayoutStyle TEXT NOT NULL DEFAULT 'CAROUSEL',
                featuredBookId INTEGER,
                primaryColorHex INTEGER NOT NULL,
                darkAccentColorHex INTEGER NOT NULL,
                ctaHighlightHex INTEGER NOT NULL,
                canvasBackgroundHex INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    suspend fun <T> runInTransaction(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        val db = writableDatabase
        suppressNotifications = true
        db.beginTransaction()
        try {
            val result = block()
            db.setTransactionSuccessful()
            result
        } finally {
            db.endTransaction()
            suppressNotifications = false
            notifyAllChanged()
        }
    }

    internal fun notifyUsersChanged() {
        if (!suppressNotifications) _usersTick.value += 1
    }

    internal fun notifyBooksChanged() {
        if (!suppressNotifications) _booksTick.value += 1
    }

    internal fun notifyOrdersChanged() {
        if (!suppressNotifications) _ordersTick.value += 1
    }

    internal fun notifyLibraryChanged() {
        if (!suppressNotifications) _libraryTick.value += 1
    }

    internal fun notifySettingsChanged() {
        if (!suppressNotifications) _settingsTick.value += 1
    }

    private fun notifyAllChanged() {
        _usersTick.value += 1
        _booksTick.value += 1
        _ordersTick.value += 1
        _libraryTick.value += 1
        _settingsTick.value += 1
    }

    companion object {
        private const val DB_NAME = "halmaal_publishing.db"
        private const val DB_VERSION = 5

        @Volatile
        private var INSTANCE: HalmaalDatabase? = null

        fun getInstance(context: Context): HalmaalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = HalmaalDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        fun createInMemory(context: Context): HalmaalDatabase {
            return HalmaalDatabase(context.applicationContext, dbName = null)
        }
    }
}
