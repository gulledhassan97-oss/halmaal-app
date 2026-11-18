package com.example.data.local

data class UserEntity(
    val id: Long = 0,
    val email: String,
    val passwordHash: String,
    val isActive: Boolean = false,
    val verificationCodeHash: String? = null,
    val codeExpiresAt: Long? = null,
    val role: String = "user",
    val createdAt: Long = System.currentTimeMillis()
)

data class BookEntity(
    val id: Long = 0,
    val title: String,
    val author: String,
    val description: String,
    val price: Double,
    val pdfFilename: String,
    val totalPages: Int = 0,
    val category: String = "Literature",
    val accentColorHex: Long = 0xFF0B2B5C,
    val createdAt: Long = System.currentTimeMillis()
)

data class OrderEntity(
    val id: Long = 0,
    val userId: Long,
    val bookId: Long,
    val bookTitle: String,
    val amount: Double,
    val currency: String = "USD",
    val gateway: String,
    val phone: String,
    val status: String = "pending",
    val sifalopayRef: String,
    val sid: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class UserLibraryEntity(
    val userId: Long,
    val bookId: Long,
    val unlockedAt: Long = System.currentTimeMillis()
)

const val DEFAULT_WELCOME_TITLE =
    "Buugaagta dhijitaalka ah ee kor u qaada maskaxda, dhisa nolosha."

data class HomeScreenSettingsEntity(
    val id: Int = 1,
    val brandTitle: String = "Hal-maal",
    val brandSubtitle: String = "Digital Publishing",
    val showWelcomeScreen: Boolean = true,
    val welcomeTitle: String = DEFAULT_WELCOME_TITLE,
    val welcomeSubtitle: String = "World best writers, works and write entertaining literature for you",
    val welcomeButtonText: String = "Lets start",
    val showSearchBar: Boolean = true,
    val showCategoriesSection: Boolean = true,
    val showTrendingSection: Boolean = true,
    val showRecommendedPanel: Boolean = true,
    val categoriesHeading: String = "Categories",
    val trendingHeading: String = "Trending Books",
    val recommendedHeading: String = "More Recommended",
    val trendingLayoutStyle: String = "CAROUSEL",
    val featuredBookId: Long? = null,
    val primaryColorHex: Long = 0xFF0B2B5CL,
    val darkAccentColorHex: Long = 0xFF001027L,
    val ctaHighlightHex: Long = 0xFFD99B1EL,
    val canvasBackgroundHex: Long = 0xFFFAF7F0L,
    val updatedAt: Long = System.currentTimeMillis()
)
