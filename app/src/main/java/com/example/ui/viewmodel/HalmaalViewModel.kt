package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BookEntity
import com.example.data.local.HalmaalDatabase
import com.example.data.local.HomeScreenSettingsEntity
import com.example.data.local.UserEntity
import com.example.data.remote.GatewayCheckoutResult
import com.example.data.remote.GatewayVerificationResult
import com.example.data.remote.PaymentGateway
import com.example.data.remote.SifaloPayGateway
import com.example.data.repository.AuthResult
import com.example.data.repository.HalmaalRepository
import com.example.data.storage.BookPageStorage
import com.example.ui.state.AdminAnalyticsState
import com.example.ui.state.AuthModalState
import com.example.ui.state.BookEditorModalState
import com.example.ui.state.CatalogFilter
import com.example.ui.state.CheckoutModalState
import com.example.ui.state.HalmaalUiState
import com.example.ui.state.SandboxOtpBanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class HalmaalViewModel(application: Application) : AndroidViewModel(application) {

    private val database = HalmaalDatabase.getInstance(application)
    private val pageStorage = BookPageStorage(application)
    private val sifaloPayGateway = SifaloPayGateway()
    private val repository = HalmaalRepository(database, pageStorage, sifaloPayGateway)

    private val currentUserFlow = MutableStateFlow<UserEntity?>(null)
    private val searchQueryFlow = MutableStateFlow("")
    private val selectedFilterFlow = MutableStateFlow(CatalogFilter.ALL)
    private val selectedCategoryFlow = MutableStateFlow("All")
    private val welcomeDismissedFlow = MutableStateFlow(false)
    private val selectedDetailBookFlow = MutableStateFlow<BookEntity?>(null)
    private val authModalFlow = MutableStateFlow<AuthModalState?>(null)
    private val checkoutModalFlow = MutableStateFlow<CheckoutModalState?>(null)
    private val bookEditorModalFlow = MutableStateFlow<BookEditorModalState?>(null)
    private val uploadProgressFlow = MutableStateFlow<UploadProgressUi>(UploadProgressUi())
    private val snackbarFlow = MutableStateFlow<String?>(null)

    private data class UploadProgressUi(
        val isUploading: Boolean = false,
        val progressText: String? = null,
        val error: String? = null,
        val success: String? = null
    )

    private val ownedBookIdsFlow = currentUserFlow.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else repository.observeOwnedBookIds(user.id)
    }

    private val userOrdersFlow = currentUserFlow.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else repository.observeOrdersForUser(user.id)
    }

    private val adminAnalyticsFlow = combine(
        repository.userCount,
        repository.paidOrdersCount,
        repository.totalRevenue,
        repository.bookSalesStats,
        uploadProgressFlow
    ) { users, paidOrders, revenue, bookSales, uploadUi ->
        AdminAnalyticsState(
            totalUsers = users,
            paidOrders = paidOrders,
            totalRevenue = revenue,
            totalBooks = bookSales.size,
            salesPerBook = bookSales,
            isUploading = uploadUi.isUploading,
            uploadProgressText = uploadUi.progressText,
            uploadError = uploadUi.error,
            uploadSuccess = uploadUi.success
        )
    }

    private val modalBundleFlow = combine(
        authModalFlow,
        checkoutModalFlow,
        bookEditorModalFlow
    ) { auth, checkout, editor ->
        Triple(auth, checkout, editor)
    }

    private val storefrontPrefsFlow = combine(
        searchQueryFlow,
        selectedFilterFlow,
        selectedCategoryFlow,
        welcomeDismissedFlow,
        repository.homeSettings
    ) { query, filter, category, welcomeDismissed, homeSettings ->
        StorefrontPrefsBundle(query, filter, category, welcomeDismissed, homeSettings)
    }

    private data class StorefrontPrefsBundle(
        val query: String,
        val filter: CatalogFilter,
        val category: String,
        val welcomeDismissed: Boolean,
        val homeSettings: HomeScreenSettingsEntity
    )

    val uiState: StateFlow<HalmaalUiState> = combine(
        currentUserFlow,
        repository.allBooks,
        ownedBookIdsFlow,
        userOrdersFlow,
        storefrontPrefsFlow,
        selectedDetailBookFlow,
        modalBundleFlow,
        adminAnalyticsFlow,
        snackbarFlow
    ) { values ->
        val user = values[0] as UserEntity?
        @Suppress("UNCHECKED_CAST")
        val books = values[1] as List<BookEntity>
        @Suppress("UNCHECKED_CAST")
        val ownedIds = values[2] as List<Long>
        @Suppress("UNCHECKED_CAST")
        val orders = values[3] as List<com.example.data.local.OrderEntity>
        val prefs = values[4] as StorefrontPrefsBundle
        val detailBook = values[5] as BookEntity?
        @Suppress("UNCHECKED_CAST")
        val modals = values[6] as Triple<AuthModalState?, CheckoutModalState?, BookEditorModalState?>
        val adminState = values[7] as AdminAnalyticsState
        val snackbar = values[8] as String?

        HalmaalUiState(
            currentUser = user,
            books = books,
            ownedBookIds = ownedIds.toSet(),
            userOrders = orders,
            searchQuery = prefs.query,
            selectedFilter = prefs.filter,
            selectedCategory = prefs.category,
            welcomeDismissed = prefs.welcomeDismissed,
            homeSettings = prefs.homeSettings,
            selectedDetailBook = detailBook,
            authModal = modals.first,
            checkoutModal = modals.second,
            bookEditorModal = modals.third,
            adminState = adminState.copy(totalBooks = books.size),
            isLivePaymentConfigured = repository.isLivePaymentKeyConfigured,
            snackbarMessage = snackbar
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HalmaalUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    fun getPageFile(bookId: Long, pageNum: Int): File = repository.getPageFile(bookId, pageNum)

    suspend fun canAccessPage(bookId: Long, pageNum: Int): Boolean {
        return repository.canAccessPage(currentUserFlow.value, bookId, pageNum)
    }

    fun updateSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun selectFilter(filter: CatalogFilter) {
        selectedFilterFlow.value = filter
    }

    fun selectCategory(category: String) {
        selectedCategoryFlow.value = category
    }

    fun dismissWelcomeScreen() {
        welcomeDismissedFlow.value = true
    }

    fun saveHomeSettings(settings: HomeScreenSettingsEntity) {
        viewModelScope.launch {
            repository.saveHomeSettings(settings)
            snackbarFlow.value = "Home screen settings saved & applied"
        }
    }

    fun resetHomeSettings() {
        viewModelScope.launch {
            repository.resetHomeSettings()
            snackbarFlow.value = "Home screen settings reset to default design"
        }
    }

    fun openBookDetail(book: BookEntity) {
        selectedDetailBookFlow.value = book
    }

    fun closeBookDetail() {
        selectedDetailBookFlow.value = null
    }

    fun dismissSnackbar() {
        snackbarFlow.value = null
    }

    // ==================== AUTHENTICATION ====================

    fun openLoginModal() {
        authModalFlow.value = AuthModalState(isLoginMode = true)
    }

    fun openRegisterModal() {
        authModalFlow.value = AuthModalState(isLoginMode = false)
    }

    fun closeAuthModal() {
        authModalFlow.value = null
    }

    fun switchAuthMode(loginMode: Boolean) {
        authModalFlow.update { state ->
            (state ?: AuthModalState()).copy(
                isLoginMode = loginMode,
                errorMessage = null,
                successMessage = null,
                awaitingOtpVerification = false
            )
        }
    }

    fun updateAuthEmail(email: String) {
        authModalFlow.update { it?.copy(email = email, errorMessage = null) }
    }

    fun updateAuthPassword(password: String) {
        authModalFlow.update { it?.copy(password = password, errorMessage = null) }
    }

    fun updateAuthOtp(otp: String) {
        authModalFlow.update { it?.copy(otpCode = otp.filter { ch -> ch.isDigit() }.take(6), errorMessage = null) }
    }

    fun prefillAdminCredentials() {
        authModalFlow.update {
            (it ?: AuthModalState(isLoginMode = true)).copy(
                isLoginMode = true,
                email = HalmaalRepository.ADMIN_EMAIL,
                password = HalmaalRepository.ADMIN_DEFAULT_PASS,
                errorMessage = null
            )
        }
    }

    fun submitLogin() {
        val state = authModalFlow.value ?: return
        viewModelScope.launch {
            authModalFlow.update { it?.copy(isLoading = true, errorMessage = null) }
            when (val res = repository.login(state.email, state.password)) {
                is AuthResult.LoggedIn -> {
                    currentUserFlow.value = res.user
                    authModalFlow.value = null
                    snackbarFlow.value = "Signed in as ${res.user.email}"
                }
                is AuthResult.Error -> {
                    authModalFlow.update { it?.copy(isLoading = false, errorMessage = res.message) }
                }
                else -> Unit
            }
        }
    }

    fun submitRegister() {
        val state = authModalFlow.value ?: return
        viewModelScope.launch {
            authModalFlow.update { it?.copy(isLoading = true, errorMessage = null, successMessage = null) }
            when (val res = repository.registerUser(state.email, state.password)) {
                is AuthResult.RegisteredPendingOtp -> {
                    authModalFlow.update {
                        it?.copy(
                            isLoading = false,
                            awaitingOtpVerification = true,
                            sandboxOtpBanner = SandboxOtpBanner(res.email, res.sandboxOtpCode),
                            successMessage = "6-digit verification code sent (valid for 10 minutes)."
                        )
                    }
                }
                is AuthResult.Error -> {
                    authModalFlow.update { it?.copy(isLoading = false, errorMessage = res.message) }
                }
                else -> Unit
            }
        }
    }

    fun submitVerifyOtp() {
        val state = authModalFlow.value ?: return
        viewModelScope.launch {
            authModalFlow.update { it?.copy(isLoading = true, errorMessage = null) }
            when (val res = repository.verifyOtp(state.email, state.otpCode)) {
                is AuthResult.Verified -> {
                    authModalFlow.update {
                        it?.copy(
                            isLoading = false,
                            isLoginMode = true,
                            awaitingOtpVerification = false,
                            sandboxOtpBanner = null,
                            otpCode = "",
                            successMessage = res.message
                        )
                    }
                }
                is AuthResult.Error -> {
                    authModalFlow.update { it?.copy(isLoading = false, errorMessage = res.message) }
                }
                else -> Unit
            }
        }
    }

    fun logout() {
        currentUserFlow.value = null
        selectedFilterFlow.value = CatalogFilter.ALL
        snackbarFlow.value = "Signed out of Hal-maal"
    }

    // ==================== SIFALOPAY CHECKOUT ====================

    fun openCheckout(book: BookEntity) {
        if (currentUserFlow.value == null) {
            snackbarFlow.value = "Please sign in to purchase ${book.title}"
            openLoginModal()
            return
        }
        selectedDetailBookFlow.value = null
        checkoutModalFlow.value = CheckoutModalState(book = book)
    }

    fun closeCheckout() {
        checkoutModalFlow.value = null
    }

    fun selectCheckoutGateway(gateway: PaymentGateway) {
        checkoutModalFlow.update { it?.copy(selectedGateway = gateway, errorMessage = null) }
    }

    fun selectCheckoutCurrency(currency: String) {
        checkoutModalFlow.update { it?.copy(selectedCurrency = currency, errorMessage = null) }
    }

    fun updateCheckoutPhone(phone: String) {
        checkoutModalFlow.update { it?.copy(phone = phone, errorMessage = null) }
    }

    fun initiateSifaloPayment() {
        val checkout = checkoutModalFlow.value ?: return
        if (checkout.isSubmitting) return
        val user = currentUserFlow.value
        if (user == null) {
            checkoutModalFlow.update {
                it?.copy(
                    isSubmitting = false,
                    errorMessage = "Please sign in first to complete your ${checkout.selectedGateway.displayName} payment."
                )
            }
            openLoginModal()
            return
        }

        viewModelScope.launch {
            checkoutModalFlow.update { it?.copy(isSubmitting = true, errorMessage = null) }
            try {
                val result = repository.initiateBookCheckout(
                    userId = user.id,
                    book = checkout.book,
                    gateway = checkout.selectedGateway,
                    phone = checkout.phone,
                    currency = checkout.selectedCurrency
                )
                result.fold(
                    onSuccess = { (order, gatewayResult) ->
                        when (gatewayResult) {
                            is GatewayCheckoutResult.ImmediatePaid -> {
                                checkoutModalFlow.update {
                                    it?.copy(
                                        isSubmitting = false,
                                        pendingOrderRef = order.sifalopayRef,
                                        pendingSid = gatewayResult.sid,
                                        pendingIsSandbox = gatewayResult.isSandboxMode,
                                        completed = true,
                                        statusMessage = "${gatewayResult.message} Unlocked for read-only Flipbook viewing."
                                    )
                                }
                                snackbarFlow.value = "${checkout.book.title} unlocked in Flipbook Reader"
                            }
                            is GatewayCheckoutResult.RedirectRequired -> {
                                checkoutModalFlow.update {
                                    it?.copy(
                                        isSubmitting = false,
                                        pendingOrderRef = order.sifalopayRef,
                                        pendingSid = gatewayResult.sid,
                                        paymentUrl = gatewayResult.paymentUrl,
                                        pendingIsSandbox = false,
                                        statusMessage = gatewayResult.message
                                    )
                                }
                            }
                            is GatewayCheckoutResult.PendingUssd -> {
                                checkoutModalFlow.update {
                                    it?.copy(
                                        isSubmitting = false,
                                        pendingOrderRef = order.sifalopayRef,
                                        pendingSid = gatewayResult.sid,
                                        pendingIsSandbox = gatewayResult.isSandboxMode,
                                        statusMessage = gatewayResult.message
                                    )
                                }
                            }
                            is GatewayCheckoutResult.Failed -> {
                                checkoutModalFlow.update {
                                    it?.copy(isSubmitting = false, errorMessage = gatewayResult.error)
                                }
                            }
                        }
                    },
                    onFailure = { error ->
                        checkoutModalFlow.update {
                            it?.copy(
                                isSubmitting = false,
                                errorMessage = error.message ?: "Unable to start payment."
                            )
                        }
                    }
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                checkoutModalFlow.update {
                    it?.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Unexpected error starting ${checkout.selectedGateway.displayName} payment."
                    )
                }
            }
        }
    }

    fun verifyAndUnlockPayment() {
        val checkout = checkoutModalFlow.value ?: return
        if (checkout.isVerifying) return
        val orderId = checkout.pendingOrderRef ?: return
        viewModelScope.launch {
            checkoutModalFlow.update { it?.copy(isVerifying = true, errorMessage = null) }
            try {
                when (val verification = repository.verifyAndCompleteOrder(
                    orderId = orderId,
                    sid = checkout.pendingSid,
                    isSandboxMode = checkout.pendingIsSandbox
                )) {
                    is GatewayVerificationResult.VerifiedPaid -> {
                        checkoutModalFlow.update {
                            it?.copy(
                                isVerifying = false,
                                completed = true,
                                pendingSid = verification.sid,
                                statusMessage = "Order ${verification.orderId} confirmed (${verification.sid}). Read-only Flipbook access unlocked (PDF download disabled)."
                            )
                        }
                        snackbarFlow.value = "${checkout.book.title} unlocked in Flipbook Reader"
                    }
                    is GatewayVerificationResult.Pending -> {
                        checkoutModalFlow.update {
                            it?.copy(isVerifying = false, statusMessage = verification.message)
                        }
                    }
                    is GatewayVerificationResult.Failed -> {
                        checkoutModalFlow.update {
                            it?.copy(isVerifying = false, errorMessage = verification.error)
                        }
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                checkoutModalFlow.update {
                    it?.copy(
                        isVerifying = false,
                        errorMessage = e.message ?: "Unexpected error verifying payment."
                    )
                }
            }
        }
    }

    // ==================== SUPER ADMIN BOOK UPLOAD & UPDATE ====================

    fun openCreateBookEditor() {
        uploadProgressFlow.update { it.copy(error = null, success = null) }
        bookEditorModalFlow.value = BookEditorModalState()
    }

    fun openEditBookEditor(book: BookEntity) {
        uploadProgressFlow.update { it.copy(error = null, success = null) }
        bookEditorModalFlow.value = BookEditorModalState(
            editingBook = book,
            title = book.title,
            author = book.author,
            priceText = String.format(java.util.Locale.US, "%.2f", book.price),
            category = book.category,
            description = book.description,
            accentColorHex = book.accentColorHex
        )
    }

    fun closeBookEditor() {
        if (bookEditorModalFlow.value?.isSaving == true) return
        bookEditorModalFlow.value = null
    }

    fun updateEditorTitle(value: String) {
        bookEditorModalFlow.update { it?.copy(title = value, errorMessage = null) }
    }

    fun updateEditorAuthor(value: String) {
        bookEditorModalFlow.update { it?.copy(author = value, errorMessage = null) }
    }

    fun updateEditorPrice(value: String) {
        bookEditorModalFlow.update { it?.copy(priceText = value, errorMessage = null) }
    }

    fun updateEditorCategory(value: String) {
        bookEditorModalFlow.update { it?.copy(category = value, errorMessage = null) }
    }

    fun updateEditorDescription(value: String) {
        bookEditorModalFlow.update { it?.copy(description = value, errorMessage = null) }
    }

    fun updateEditorAccentColor(colorHex: Long) {
        bookEditorModalFlow.update { it?.copy(accentColorHex = colorHex, errorMessage = null) }
    }

    fun toggleEditorManuscriptMode(useManuscript: Boolean) {
        bookEditorModalFlow.update {
            it?.copy(useManuscriptMode = useManuscript, errorMessage = null)
        }
    }

    fun updateEditorManuscriptText(value: String) {
        bookEditorModalFlow.update { it?.copy(manuscriptText = value, errorMessage = null) }
    }

    fun selectEditorPdf(uri: Uri?) {
        if (uri == null) return
        val displayName = pageStorage.resolveDisplayName(uri)
        bookEditorModalFlow.update {
            it?.copy(
                useManuscriptMode = false,
                selectedPdfUriString = uri.toString(),
                selectedPdfLabel = displayName,
                errorMessage = null
            )
        }
    }

    fun clearEditorPdf() {
        bookEditorModalFlow.update {
            it?.copy(selectedPdfUriString = null, selectedPdfLabel = null)
        }
    }

    fun submitBookEditor() {
        val state = bookEditorModalFlow.value ?: return
        val price = state.priceText.toDoubleOrNull()
        if (state.title.isBlank()) {
            bookEditorModalFlow.update { it?.copy(errorMessage = "Book title is required.") }
            return
        }
        if (price == null || price <= 0.0) {
            bookEditorModalFlow.update { it?.copy(errorMessage = "Enter a valid USD price greater than 0.") }
            return
        }

        val pdfUri = state.selectedPdfUriString?.let { Uri.parse(it) }
        val manuscriptText = if (state.useManuscriptMode) state.manuscriptText.trim() else null

        if (!state.isEditMode && pdfUri == null && manuscriptText.isNullOrBlank()) {
            bookEditorModalFlow.update {
                it?.copy(errorMessage = "Select a PDF file or switch to Compose Edition and add manuscript text.")
            }
            return
        }

        viewModelScope.launch {
            bookEditorModalFlow.update {
                it?.copy(
                    isSaving = true,
                    progressText = if (pdfUri != null) "Preparing PDF rasterizer..." else "Saving publication...",
                    errorMessage = null
                )
            }

            val result = if (state.editingBook == null) {
                repository.uploadAndRasterizeBook(
                    title = state.title,
                    author = state.author,
                    description = state.description,
                    price = price,
                    category = state.category,
                    accentColorHex = state.accentColorHex,
                    pdfUri = pdfUri,
                    manuscriptText = manuscriptText
                ) { current, total ->
                    bookEditorModalFlow.update {
                        it?.copy(isSaving = true, progressText = "Rasterizing page $current of $total...")
                    }
                }
            } else {
                repository.updateBook(
                    bookId = state.editingBook.id,
                    title = state.title,
                    author = state.author,
                    description = state.description,
                    price = price,
                    category = state.category,
                    accentColorHex = state.accentColorHex,
                    newPdfUri = pdfUri,
                    newManuscriptText = manuscriptText
                ) { current, total ->
                    bookEditorModalFlow.update {
                        it?.copy(isSaving = true, progressText = "Re-rasterizing page $current of $total...")
                    }
                }
            }

            result.fold(
                onSuccess = { savedBook ->
                    if (selectedDetailBookFlow.value?.id == savedBook.id) {
                        selectedDetailBookFlow.value = savedBook
                    }
                    bookEditorModalFlow.value = null
                    val verb = if (state.isEditMode) "Updated" else "Published"
                    val summary = "$verb \"${savedBook.title}\" (${savedBook.totalPages} pages)."
                    uploadProgressFlow.value = UploadProgressUi(isUploading = false, success = summary)
                    snackbarFlow.value = summary
                },
                onFailure = { err ->
                    bookEditorModalFlow.update {
                        it?.copy(
                            isSaving = false,
                            progressText = null,
                            errorMessage = err.message ?: "Failed to save publication."
                        )
                    }
                }
            )
        }
    }

    fun uploadAndRasterizeBook(
        title: String,
        author: String,
        description: String,
        priceText: String,
        category: String,
        pdfUri: Uri?
    ) {
        val price = priceText.toDoubleOrNull()
        if (title.isBlank() || price == null || price <= 0.0 || pdfUri == null) {
            uploadProgressFlow.value = UploadProgressUi(
                error = "Title, valid price (> 0), and PDF file are required."
            )
            return
        }

        viewModelScope.launch {
            uploadProgressFlow.value = UploadProgressUi(
                isUploading = true,
                progressText = "Preparing PDF rasterizer..."
            )
            val result = repository.uploadAndRasterizeBook(
                title = title,
                author = author,
                description = description,
                price = price,
                category = category,
                pdfUri = pdfUri
            ) { current, total ->
                uploadProgressFlow.value = UploadProgressUi(
                    isUploading = true,
                    progressText = "Rasterizing page $current of $total..."
                )
            }

            result.fold(
                onSuccess = { book ->
                    uploadProgressFlow.value = UploadProgressUi(
                        isUploading = false,
                        success = "Published \"${book.title}\" (${book.totalPages} pages rasterized)."
                    )
                },
                onFailure = { err ->
                    uploadProgressFlow.value = UploadProgressUi(
                        isUploading = false,
                        error = err.message ?: "Failed to convert PDF."
                    )
                }
            )
        }
    }

    fun clearUploadFeedback() {
        uploadProgressFlow.update { it.copy(error = null, success = null) }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            repository.deleteBook(book.id)
            snackbarFlow.value = "Deleted \"${book.title}\""
        }
    }
}
