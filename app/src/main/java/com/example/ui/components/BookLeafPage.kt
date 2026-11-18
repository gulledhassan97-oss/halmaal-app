package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.R
import com.example.data.local.BookEntity
import java.io.File

/**
 * Represents a physical book spread.
 * Spread 0 has leftPage = null (front cover on right), matching StPageFlip showCover = true.
 * Subsequent spreads pair even/odd pages: [2, 3], [4, 5], etc.
 */
data class BookSpread(
    val leftPage: Int?,
    val rightPage: Int?
)

fun buildBookSpreads(totalPages: Int): List<BookSpread> {
    if (totalPages <= 1) return listOf(BookSpread(leftPage = null, rightPage = 1))
    val spreads = mutableListOf<BookSpread>()
    spreads.add(BookSpread(leftPage = null, rightPage = 1))
    var page = 2
    while (page <= totalPages) {
        val left = page
        val right = if (page + 1 <= totalPages) page + 1 else null
        spreads.add(BookSpread(leftPage = left, rightPage = right))
        page += 2
    }
    return spreads
}

@Composable
internal fun InsideCoverEndpaper(isLeft: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    if (isLeft) {
                        listOf(Color(0xFF0B2B5C), Color(0xFF001027))
                    } else {
                        listOf(Color(0xFF001027), Color(0xFF0B2B5C))
                    }
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.halmaal_brand_logo),
            contentDescription = "Hal-maal Digital Publishing",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize(0.75f)
                .clip(RoundedCornerShape(10.dp))
        )
    }
}

@Composable
internal fun BookLeafPage(
    book: BookEntity,
    pageNum: Int,
    isLeftPage: Boolean,
    buyerEmail: String?,
    isOwned: Boolean,
    getPageFile: (Long, Int) -> File,
    onCheckAccess: suspend (Long, Int) -> Boolean,
    onUnlockClick: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val hasAccess by produceState(
        initialValue = pageNum == 1 || isOwned,
        key1 = book.id,
        key2 = pageNum,
        key3 = isOwned
    ) {
        value = onCheckAccess(book.id, pageNum)
    }
    val pageFile = remember(book.id, pageNum) { getPageFile(book.id, pageNum) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(
                if (isLeftPage) {
                    RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)
                } else {
                    RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)
                }
            )
            .background(Color(0xFFFDFBF7))
    ) {
        if (hasAccess) {
            if (pageFile.exists()) {
                Image(
                    painter = rememberAsyncImagePainter(model = pageFile),
                    contentDescription = "Page $pageNum of ${book.title}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Page $pageNum unavailable", color = Color.DarkGray)
                }
            }

            // Realistic gutter gradient along the binding edge
            Box(
                modifier = Modifier
                    .align(if (isLeftPage) Alignment.CenterEnd else Alignment.CenterStart)
                    .width(24.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            if (isLeftPage) {
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.16f))
                            } else {
                                listOf(Color.Black.copy(alpha = 0.16f), Color.Transparent)
                            }
                        )
                    )
            )

            // Visible buyer email watermark across protected pages
            if (pageNum > 1 && !buyerEmail.isNullOrBlank()) {
                BuyerWatermarkOverlay(
                    watermarkText = "Licensed to $buyerEmail • Hal-maal",
                    textMeasurer = textMeasurer
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF18181B))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(34.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Page $pageNum Locked",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Page 1 is a free cover preview. Unlock all ${book.totalPages} pages with ZAAD or eDahab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD4D4D8),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onUnlockClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("reader_unlock_button")
                ) {
                    Text("Unlock Book", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun BuyerWatermarkOverlay(
    watermarkText: String,
    textMeasurer: TextMeasurer
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val style = TextStyle(
            color = Color(0xFF0F766E).copy(alpha = 0.10f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        val measured = textMeasurer.measure(watermarkText, style)
        rotate(degrees = -28f) {
            val stepY = 175f
            val stepX = (measured.size.width + 72).toFloat()
            var y = -size.height
            while (y < size.height * 2f) {
                var x = -size.width
                while (x < size.width * 2f) {
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(x, y)
                    )
                    x += stepX
                }
                y += stepY
            }
        }
    }
}
