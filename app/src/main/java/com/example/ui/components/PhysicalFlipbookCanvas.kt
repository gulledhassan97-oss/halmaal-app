package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.BookEntity
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun PhysicalFlipbookCanvas(
    book: BookEntity,
    isSpreadMode: Boolean,
    currentIndex: Int,
    flipAnimatable: Animatable<Float, *>,
    buyerEmail: String?,
    isOwned: Boolean,
    getPageFile: (Long, Int) -> File,
    onCheckAccess: suspend (Long, Int) -> Boolean,
    onUnlockClick: () -> Unit,
    onIndexCommitted: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val totalPages = book.totalPages.coerceAtLeast(1)
    val spreads = remember(totalPages) { buildBookSpreads(totalPages) }
    val maxIndex = if (isSpreadMode) spreads.lastIndex else (totalPages - 1)
    val safeIndex = currentIndex.coerceIn(0, maxIndex)

    val containerWidthPx = remember { mutableFloatStateOf(900f) }

    LaunchedEffect(maxIndex) {
        if (currentIndex > maxIndex) {
            onIndexCommitted(maxIndex)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .testTag("physical_flipbook_canvas"),
        contentAlignment = Alignment.Center
    ) {
        val pageAspect = 0.715f
        val targetRatio = if (isSpreadMode) pageAspect * 2f else pageAspect
        val targetMaxWidth = if (isSpreadMode) 960.dp else 540.dp
        val availW = maxWidth.coerceAtMost(targetMaxWidth).coerceAtLeast(120.dp)
        val availH = maxHeight.coerceAtLeast(120.dp)
        val (fittedW, fittedH) = if (availW / availH > targetRatio) {
            (availH * targetRatio) to availH
        } else {
            availW to (availW / targetRatio)
        }
        val dragSensitivity = if (isSpreadMode) 0.52f else 0.7f

        Box(
            modifier = Modifier
                .size(width = fittedW, height = fittedH)
                .pointerInput(safeIndex, maxIndex, isSpreadMode) {
                    containerWidthPx.floatValue = size.width.toFloat().coerceAtLeast(300f)
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val delta = -dragAmount / (containerWidthPx.floatValue * dragSensitivity)
                            val minBound = if (safeIndex > 0) -1f else 0f
                            val maxBound = if (safeIndex < maxIndex) 1f else 0f
                            val nextVal = (flipAnimatable.value + delta).coerceIn(minBound, maxBound)
                            scope.launch { flipAnimatable.snapTo(nextVal) }
                        },
                        onDragEnd = {
                            scope.launch {
                                val current = flipAnimatable.value
                                when {
                                    current > 0.30f && safeIndex < maxIndex -> {
                                        flipAnimatable.animateTo(
                                            1f,
                                            animationSpec = tween(260, easing = FastOutSlowInEasing)
                                        )
                                        onIndexCommitted(safeIndex + 1)
                                        flipAnimatable.snapTo(0f)
                                    }
                                    current < -0.30f && safeIndex > 0 -> {
                                        flipAnimatable.animateTo(
                                            -1f,
                                            animationSpec = tween(260, easing = FastOutSlowInEasing)
                                        )
                                        onIndexCommitted(safeIndex - 1)
                                        flipAnimatable.snapTo(0f)
                                    }
                                    else -> {
                                        flipAnimatable.animateTo(
                                            0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                flipAnimatable.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                            }
                        }
                    )
                }
                .pointerInput(safeIndex, maxIndex, isSpreadMode) {
                    detectTapGestures { offset ->
                        if (flipAnimatable.isRunning) return@detectTapGestures
                        val tapZone = offset.x / size.width.toFloat()
                        if (tapZone > 0.72f && safeIndex < maxIndex) {
                            scope.launch {
                                flipAnimatable.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
                                onIndexCommitted(safeIndex + 1)
                                flipAnimatable.snapTo(0f)
                            }
                        } else if (tapZone < 0.28f && safeIndex > 0) {
                            scope.launch {
                                flipAnimatable.animateTo(-1f, tween(420, easing = FastOutSlowInEasing))
                                onIndexCommitted(safeIndex - 1)
                                flipAnimatable.snapTo(0f)
                            }
                        }
                    }
                }
        ) {
            val progress = flipAnimatable.value
            if (isSpreadMode) {
                SpreadFlipbookContent(
                    book = book,
                    spreads = spreads,
                    currentSpreadIndex = safeIndex,
                    flipProgress = progress,
                    buyerEmail = buyerEmail,
                    isOwned = isOwned,
                    getPageFile = getPageFile,
                    onCheckAccess = onCheckAccess,
                    onUnlockClick = onUnlockClick
                )
            } else {
                SinglePageFlipbookContent(
                    book = book,
                    currentPageIndex = safeIndex,
                    totalPages = totalPages,
                    flipProgress = progress,
                    buyerEmail = buyerEmail,
                    isOwned = isOwned,
                    getPageFile = getPageFile,
                    onCheckAccess = onCheckAccess,
                    onUnlockClick = onUnlockClick
                )
            }
        }
    }
}

@Composable
private fun SpreadFlipbookContent(
    book: BookEntity,
    spreads: List<BookSpread>,
    currentSpreadIndex: Int,
    flipProgress: Float,
    buyerEmail: String?,
    isOwned: Boolean,
    getPageFile: (Long, Int) -> File,
    onCheckAccess: suspend (Long, Int) -> Boolean,
    onUnlockClick: () -> Unit
) {
    val currentSpread = spreads[currentSpreadIndex]
    val nextSpread = spreads.getOrNull(currentSpreadIndex + 1)
    val prevSpread = spreads.getOrNull(currentSpreadIndex - 1)

    // Physical hardcover binding board behind the paper spread
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("spread_flipbook_board"),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1F2937),
        border = BorderStroke(2.dp, Color(0xFF334155)),
        shadowElevation = 18.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            // 1. Base Layer: Left & Right static halves revealed underneath the turning leaf
            Row(modifier = Modifier.fillMaxSize()) {
                val baseLeftPage = when {
                    flipProgress < 0f && prevSpread != null -> prevSpread.leftPage
                    else -> currentSpread.leftPage
                }
                val baseRightPage = when {
                    flipProgress > 0f && nextSpread != null -> nextSpread.rightPage
                    else -> currentSpread.rightPage
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (baseLeftPage != null) {
                        BookLeafPage(
                            book = book,
                            pageNum = baseLeftPage,
                            isLeftPage = true,
                            buyerEmail = buyerEmail,
                            isOwned = isOwned,
                            getPageFile = getPageFile,
                            onCheckAccess = onCheckAccess,
                            onUnlockClick = onUnlockClick
                        )
                        // Shadow cast onto left base page while landing forward or uncovering backward
                        if (flipProgress > 0.5f) {
                            val dim = ((flipProgress - 0.5f) * 2f).coerceIn(0f, 1f) * 0.28f
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
                        } else if (flipProgress < 0f && flipProgress >= -0.5f) {
                            val dim = (1f - abs(flipProgress) * 2f).coerceIn(0f, 1f) * 0.34f
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
                        }
                    } else {
                        InsideCoverEndpaper(isLeft = true)
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (baseRightPage != null) {
                        BookLeafPage(
                            book = book,
                            pageNum = baseRightPage,
                            isLeftPage = false,
                            buyerEmail = buyerEmail,
                            isOwned = isOwned,
                            getPageFile = getPageFile,
                            onCheckAccess = onCheckAccess,
                            onUnlockClick = onUnlockClick
                        )
                        // Shadow cast onto revealed right page while turning leaf lifts
                        if (flipProgress > 0f && flipProgress <= 0.5f) {
                            val dim = (1f - flipProgress * 2f).coerceIn(0f, 1f) * 0.34f
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
                        } else if (flipProgress < -0.5f) {
                            val dim = ((abs(flipProgress) - 0.5f) * 2f).coerceIn(0f, 1f) * 0.28f
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
                        }
                    } else {
                        InsideCoverEndpaper(isLeft = false)
                    }
                }
            }

            // Center book-spine shadow seam
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(24.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.24f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // 2. Active Turning Leaf Layer (3D GraphicsLayer pivoted on the center spine)
            if (flipProgress > 0.001f && nextSpread != null) {
                val curlShade = (sin(flipProgress * PI).toFloat() * 0.24f).coerceIn(0f, 0.28f)
                Row(modifier = Modifier.fillMaxSize()) {
                    if (flipProgress <= 0.5f) {
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    cameraDistance = 24f * density
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                    rotationY = -180f * flipProgress
                                }
                        ) {
                            if (currentSpread.rightPage != null) {
                                BookLeafPage(
                                    book = book,
                                    pageNum = currentSpread.rightPage,
                                    isLeftPage = false,
                                    buyerEmail = buyerEmail,
                                    isOwned = isOwned,
                                    getPageFile = getPageFile,
                                    onCheckAccess = onCheckAccess,
                                    onUnlockClick = onUnlockClick
                                )
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = curlShade)))
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    cameraDistance = 24f * density
                                    transformOrigin = TransformOrigin(1f, 0.5f)
                                    rotationY = 180f * (1f - flipProgress)
                                }
                        ) {
                            if (nextSpread.leftPage != null) {
                                BookLeafPage(
                                    book = book,
                                    pageNum = nextSpread.leftPage,
                                    isLeftPage = true,
                                    buyerEmail = buyerEmail,
                                    isOwned = isOwned,
                                    getPageFile = getPageFile,
                                    onCheckAccess = onCheckAccess,
                                    onUnlockClick = onUnlockClick
                                )
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = curlShade)))
                            }
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }
            } else if (flipProgress < -0.001f && prevSpread != null) {
                val p = abs(flipProgress)
                val curlShade = (sin(p * PI).toFloat() * 0.24f).coerceIn(0f, 0.28f)
                Row(modifier = Modifier.fillMaxSize()) {
                    if (p <= 0.5f) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    cameraDistance = 24f * density
                                    transformOrigin = TransformOrigin(1f, 0.5f)
                                    rotationY = 180f * p
                                }
                        ) {
                            if (currentSpread.leftPage != null) {
                                BookLeafPage(
                                    book = book,
                                    pageNum = currentSpread.leftPage,
                                    isLeftPage = true,
                                    buyerEmail = buyerEmail,
                                    isOwned = isOwned,
                                    getPageFile = getPageFile,
                                    onCheckAccess = onCheckAccess,
                                    onUnlockClick = onUnlockClick
                                )
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = curlShade)))
                            }
                        }
                        Spacer(Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    cameraDistance = 24f * density
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                    rotationY = -180f * (1f - p)
                                }
                        ) {
                            if (prevSpread.rightPage != null) {
                                BookLeafPage(
                                    book = book,
                                    pageNum = prevSpread.rightPage,
                                    isLeftPage = false,
                                    buyerEmail = buyerEmail,
                                    isOwned = isOwned,
                                    getPageFile = getPageFile,
                                    onCheckAccess = onCheckAccess,
                                    onUnlockClick = onUnlockClick
                                )
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = curlShade)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SinglePageFlipbookContent(
    book: BookEntity,
    currentPageIndex: Int,
    totalPages: Int,
    flipProgress: Float,
    buyerEmail: String?,
    isOwned: Boolean,
    getPageFile: (Long, Int) -> File,
    onCheckAccess: suspend (Long, Int) -> Boolean,
    onUnlockClick: () -> Unit
) {
    val currentPage = currentPageIndex + 1
    val nextPage = (currentPage + 1).takeIf { it <= totalPages }
    val prevPage = (currentPage - 1).takeIf { it >= 1 }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("single_flipbook_board"),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1F2937),
        shadowElevation = 16.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            when {
                flipProgress > 0.001f && nextPage != null -> {
                    BookLeafPage(
                        book = book,
                        pageNum = nextPage,
                        isLeftPage = false,
                        buyerEmail = buyerEmail,
                        isOwned = isOwned,
                        getPageFile = getPageFile,
                        onCheckAccess = onCheckAccess,
                        onUnlockClick = onUnlockClick
                    )
                    val underShadow = ((1f - flipProgress) * 0.35f).coerceIn(0f, 0.35f)
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = underShadow)))

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                cameraDistance = 20f * density
                                transformOrigin = TransformOrigin(0f, 0.5f)
                                rotationY = -105f * flipProgress
                                alpha = (1f - (flipProgress - 0.65f).coerceAtLeast(0f) * 2.8f).coerceIn(0f, 1f)
                            }
                    ) {
                        BookLeafPage(
                            book = book,
                            pageNum = currentPage,
                            isLeftPage = false,
                            buyerEmail = buyerEmail,
                            isOwned = isOwned,
                            getPageFile = getPageFile,
                            onCheckAccess = onCheckAccess,
                            onUnlockClick = onUnlockClick
                        )
                    }
                }

                flipProgress < -0.001f && prevPage != null -> {
                    val p = abs(flipProgress)
                    BookLeafPage(
                        book = book,
                        pageNum = currentPage,
                        isLeftPage = false,
                        buyerEmail = buyerEmail,
                        isOwned = isOwned,
                        getPageFile = getPageFile,
                        onCheckAccess = onCheckAccess,
                        onUnlockClick = onUnlockClick
                    )
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = (p * 0.3f).coerceIn(0f, 0.3f))))

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                cameraDistance = 20f * density
                                transformOrigin = TransformOrigin(0f, 0.5f)
                                rotationY = -105f * (1f - p)
                                alpha = (p * 2.2f).coerceIn(0f, 1f)
                            }
                    ) {
                        BookLeafPage(
                            book = book,
                            pageNum = prevPage,
                            isLeftPage = false,
                            buyerEmail = buyerEmail,
                            isOwned = isOwned,
                            getPageFile = getPageFile,
                            onCheckAccess = onCheckAccess,
                            onUnlockClick = onUnlockClick
                        )
                    }
                }

                else -> {
                    BookLeafPage(
                        book = book,
                        pageNum = currentPage,
                        isLeftPage = false,
                        buyerEmail = buyerEmail,
                        isOwned = isOwned,
                        getPageFile = getPageFile,
                        onCheckAccess = onCheckAccess,
                        onUnlockClick = onUnlockClick
                    )
                }
            }
        }
    }
}
