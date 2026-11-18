package com.example.ui.state

import com.example.data.local.BookEntity
import com.example.data.local.BookSalesStat
import com.example.data.local.HomeScreenSettingsEntity
import com.example.data.local.OrderEntity
import com.example.data.local.UserEntity
import com.example.data.remote.PaymentGateway

enum class CatalogFilter(val label: String) {
    ALL("All Publications"),
    MY_LIBRARY("My Library"),
    FREE_PREVIEW("Free Previews")
}

data class SandboxOtpBanner(
    val email: String,
    val code: String
)

data class CheckoutModalState(
    val book: BookEntity,
    val selectedGateway: PaymentGateway = PaymentGateway.ZAAD,
    val selectedCurrency: String = "USD",
    val phone: String = "",
    val isSubmitting: Boolean = false,
    val isVerifying: Boolean = false,
    val pendingOrderRef: String? = null,
    val pendingSid: String = "",
    val paymentUrl: String? = null,
    val pendingIsSandbox: Boolean = true,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val completed: Boolean = false
)

data class AuthModalState(
    val isLoginMode: Boolean = true,
    val email: String = "",
    val password: String = "",
    val otpCode: String = "",
    val awaitingOtpVerification: Boolean = false,
    val sandboxOtpBanner: SandboxOtpBanner? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class AdminAnalyticsState(
    val totalUsers: Int = 0,
    val paidOrders: Int = 0,
    val totalRevenue: Double = 0.0,
    val totalBooks: Int = 0,
    val salesPerBook: List<BookSalesStat> = emptyList(),
    val isUploading: Boolean = false,
    val uploadProgressText: String? = null,
    val uploadError: String? = null,
    val uploadSuccess: String? = null
)

data class BookEditorModalState(
    val editingBook: BookEntity? = null,
    val title: String = "",
    val author: String = "",
    val priceText: String = "",
    val category: String = "Digital Edition",
    val description: String = "",
    val accentColorHex: Long = 0xFF0F766E,
    val useManuscriptMode: Boolean = false,
    val manuscriptText: String = "",
    val selectedPdfLabel: String? = null,
    val selectedPdfUriString: String? = null,
    val isSaving: Boolean = false,
    val progressText: String? = null,
    val errorMessage: String? = null
) {
    val isEditMode: Boolean get() = editingBook != null
}

data class HalmaalUiState(
    val currentUser: UserEntity? = null,
    val books: List<BookEntity> = emptyList(),
    val ownedBookIds: Set<Long> = emptySet(),
    val userOrders: List<OrderEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: CatalogFilter = CatalogFilter.ALL,
    val selectedCategory: String = "All",
    val welcomeDismissed: Boolean = false,
    val homeSettings: HomeScreenSettingsEntity = HomeScreenSettingsEntity(),
    val selectedDetailBook: BookEntity? = null,
    val authModal: AuthModalState? = null,
    val checkoutModal: CheckoutModalState? = null,
    val bookEditorModal: BookEditorModalState? = null,
    val adminState: AdminAnalyticsState = AdminAnalyticsState(),
    val isLivePaymentConfigured: Boolean = false,
    val snackbarMessage: String? = null
) {
    val isSuperAdmin: Boolean
        get() = currentUser?.role == "super_admin" || currentUser?.role == "admin"

    val isAdmin: Boolean
        get() = isSuperAdmin

    fun isBookOwned(bookId: Long): Boolean {
        return isAdmin || ownedBookIds.contains(bookId)
    }

    val availableCategories: List<String>
        get() = listOf("All") + books.map { it.category }.distinct().sorted()

    val featuredBook: BookEntity?
        get() = homeSettings.featuredBookId?.let { id -> books.find { it.id == id } } ?: books.firstOrNull()

    val filteredBooks: List<BookEntity>
        get() = books.filter { book ->
            val matchesQuery = searchQuery.isBlank() ||
                book.title.contains(searchQuery, ignoreCase = true) ||
                book.author.contains(searchQuery, ignoreCase = true) ||
                book.category.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategory.equals("All", ignoreCase = true) ||
                book.category.equals(selectedCategory, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                CatalogFilter.ALL -> true
                CatalogFilter.MY_LIBRARY -> isBookOwned(book.id)
                CatalogFilter.FREE_PREVIEW -> book.totalPages > 0
            }
            matchesQuery && matchesCategory && matchesFilter
        }
}
