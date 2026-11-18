package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.remote.PaymentGateway
import com.example.ui.state.CheckoutModalState
import com.example.ui.theme.EdahabIndigo
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ZaadGreen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SifaloPayCheckoutSheet(
    state: CheckoutModalState,
    isLiveKeyConfigured: Boolean,
    onDismiss: () -> Unit,
    onSelectGateway: (PaymentGateway) -> Unit,
    onSelectCurrency: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onInitiatePayment: () -> Unit,
    onVerifyAndUnlock: () -> Unit,
    onOpenReaderNow: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uriHandler = LocalUriHandler.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    val submitPayment: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onInitiatePayment()
    }

    LaunchedEffect(state.pendingOrderRef, state.completed) {
        if (state.pendingOrderRef != null || state.completed) {
            scrollState.animateScrollTo(0)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            val isNarrow = maxWidth < 360.dp
            val stackWalletCards = maxWidth < 290.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .verticalScroll(scrollState)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = if (isNarrow) 16.dp else 24.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp)
            ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Checkout with Sifalo Pay",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isLiveKeyConfigured) {
                            "Basic Auth Live Gateway • ZAAD & eDahab"
                        } else {
                            "Sandbox Verification Mode • ZAAD & eDahab"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Book Summary Banner + Read-Only Restriction Note
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.book.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "By ${state.book.author} • ${state.book.totalPages} pages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = String.format(Locale.US, "$%.2f %s", state.book.price, state.selectedCurrency),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Protected read-only Flipbook edition (PDF download disabled)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (state.pendingOrderRef != null || state.completed) {
                state.errorMessage?.let { error ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("checkout_error_banner")
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            when {
                state.completed -> {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Payment Confirmed • Flipbook Unlocked",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = state.statusMessage ?: "Verified via Sifalo Pay. Read inside the protected Flipbook reader (no PDF download).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onOpenReaderNow(state.book.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("checkout_open_reader_button"),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Read Book in Flipbook")
                    }
                }

                state.pendingOrderRef != null -> {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Awaiting ${state.selectedGateway.displayName} Confirmation",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = state.statusMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Order ID: ${state.pendingOrderRef}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            if (state.pendingSid.isNotBlank()) {
                                Text(
                                    text = "Sifalo SID: ${state.pendingSid}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    state.paymentUrl?.let { url ->
                        OutlinedButton(
                            onClick = { uriHandler.openUri(url) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .padding(bottom = 10.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open Hosted ${state.selectedGateway.displayName} Checkout")
                        }
                    }

                    Button(
                        onClick = onVerifyAndUnlock,
                        enabled = !state.isVerifying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("verify_payment_button"),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        if (state.isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Filled.Verified, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Verify Payment & Unlock Flipbook")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Close")
                    }
                }

                else -> {
                    Text(
                        text = "Select Mobile Wallet",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(10.dp))
                    if (stackWalletCards) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            GatewayOptionCard(
                                gateway = PaymentGateway.ZAAD,
                                selected = state.selectedGateway == PaymentGateway.ZAAD,
                                accentColor = ZaadGreen,
                                onClick = { onSelectGateway(PaymentGateway.ZAAD) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gateway_zaad_card")
                            )
                            GatewayOptionCard(
                                gateway = PaymentGateway.EDAHAB,
                                selected = state.selectedGateway == PaymentGateway.EDAHAB,
                                accentColor = EdahabIndigo,
                                onClick = { onSelectGateway(PaymentGateway.EDAHAB) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gateway_edahab_card")
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GatewayOptionCard(
                                gateway = PaymentGateway.ZAAD,
                                selected = state.selectedGateway == PaymentGateway.ZAAD,
                                accentColor = ZaadGreen,
                                onClick = { onSelectGateway(PaymentGateway.ZAAD) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("gateway_zaad_card")
                            )
                            GatewayOptionCard(
                                gateway = PaymentGateway.EDAHAB,
                                selected = state.selectedGateway == PaymentGateway.EDAHAB,
                                accentColor = EdahabIndigo,
                                onClick = { onSelectGateway(PaymentGateway.EDAHAB) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("gateway_edahab_card")
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Currency:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        listOf("USD", "SLSH").forEach { curr ->
                            FilterChip(
                                selected = state.selectedCurrency == curr,
                                onClick = { onSelectCurrency(curr) },
                                label = { Text(curr) }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.phone,
                        onValueChange = onPhoneChange,
                        label = { Text("Wallet Phone Number") },
                        placeholder = {
                            Text(
                                if (state.selectedGateway == PaymentGateway.ZAAD) "e.g., 0634445566 or 252634445566"
                                else "e.g., 0654445566 or 252654445566"
                            )
                        },
                        supportingText = {
                            Text(
                                if (state.selectedGateway == PaymentGateway.ZAAD) {
                                    "Enter your Telesom ZAAD number (9–12 digits)"
                                } else {
                                    "Enter your Somtel eDahab number (9–12 digits)"
                                }
                            )
                        },
                        isError = state.errorMessage != null,
                        leadingIcon = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submitPayment() }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_phone_input")
                    )

                    state.errorMessage?.let { error ->
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_error_banner")
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = submitPayment,
                        enabled = !state.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("checkout_pay_now_button"),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Sending ${state.selectedGateway.displayName} prompt...")
                        } else {
                            Text(
                                String.format(
                                    Locale.US,
                                    "Pay $%.2f via %s",
                                    state.book.price,
                                    state.selectedGateway.displayName
                                )
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun GatewayOptionCard(
    gateway: PaymentGateway,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) accentColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) accentColor else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.width(2.dp))
            Column {
                Text(
                    text = gateway.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = gateway.providerLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}
