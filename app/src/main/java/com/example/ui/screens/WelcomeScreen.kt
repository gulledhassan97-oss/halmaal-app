package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.DEFAULT_WELCOME_TITLE
import com.example.data.local.HomeScreenSettingsEntity
import com.example.ui.adaptive.AdaptiveSpec
import com.example.ui.adaptive.rememberAdaptiveSpec
import com.example.ui.theme.PlayfairDisplayFontFamily

@Composable
fun WelcomeScreen(
    settings: HomeScreenSettingsEntity,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = Color(settings.primaryColorHex)
    val darkAccent = Color(settings.darkAccentColorHex)
    val highlightGold = Color(settings.ctaHighlightHex)
    val canvasBg = Color(settings.canvasBackgroundHex)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBg)
            .testTag("welcome_screen")
    ) {
        val spec = rememberAdaptiveSpec()
        val waveHeight = (spec.maxHeight * 0.46f).coerceIn(170.dp, 420.dp)

        // Organic curved top waves scaled dynamically to screen height
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(waveHeight)
                .align(Alignment.TopCenter)
        ) {
            val w = size.width
            val h = size.height

            val baseWave = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.72f)
                cubicTo(w * 0.75f, h * 0.92f, w * 0.28f, h * 0.68f, 0f, h * 0.84f)
                close()
            }
            drawPath(
                path = baseWave,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.78f), primaryColor.copy(alpha = 0.35f))
                )
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.16f),
                radius = w.coerceAtMost(h * 2f) * 0.28f,
                center = Offset(w * 0.82f, h * 0.24f)
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.48f),
                radius = w.coerceAtMost(h * 2f) * 0.24f,
                center = Offset(w * 0.16f, h * 0.20f)
            )
        }

        val innerScroll = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(innerScroll),
            contentAlignment = Alignment.Center
        ) {
            if (spec.useTwoPaneLayout) {
                // Landscape phone, foldable, or tablet: balanced 2-column Welcome layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 980.dp)
                        .heightIn(min = (spec.maxHeight - 48.dp).coerceAtLeast(240.dp))
                        .padding(horizontal = spec.horizontalPadding, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        WelcomeBrandPill(settings = settings, primaryColor = primaryColor, darkAccent = darkAccent)
                        Spacer(Modifier.height(14.dp))
                        WelcomeLogoHeroCard(
                            settings = settings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 420.dp)
                                .heightIn(max = (spec.maxHeight * 0.56f).coerceIn(140.dp, 280.dp))
                                .aspectRatio(1.75f, matchHeightConstraintsFirst = true)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        WelcomeCopyAndCta(
                            settings = settings,
                            spec = spec,
                            primaryColor = primaryColor,
                            darkAccent = darkAccent,
                            highlightGold = highlightGold,
                            onStartClick = onStartClick
                        )
                    }
                }
            } else {
                // Portrait phone (narrow, standard, or tall)
                val horizontalPad = if (spec.isNarrowScreen) 18.dp else 28.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .heightIn(min = (spec.maxHeight - 64.dp).coerceAtLeast(320.dp))
                        .padding(horizontal = horizontalPad, vertical = if (spec.isShortScreen) 12.dp else 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    WelcomeBrandPill(settings = settings, primaryColor = primaryColor, darkAccent = darkAccent)

                    Spacer(Modifier.height(12.dp))

                    WelcomeLogoHeroCard(
                        settings = settings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 400.dp)
                            .heightIn(max = (spec.maxHeight * 0.34f).coerceIn(130.dp, 250.dp))
                            .aspectRatio(1.75f)
                    )

                    Spacer(Modifier.height(16.dp))

                    WelcomeCopyAndCta(
                        settings = settings,
                        spec = spec,
                        primaryColor = primaryColor,
                        darkAccent = darkAccent,
                        highlightGold = highlightGold,
                        onStartClick = onStartClick
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeBrandPill(
    settings: HomeScreenSettingsEntity,
    primaryColor: Color,
    darkAccent: Color
) {
    Surface(
        color = Color.White.copy(alpha = 0.88f),
        shape = RoundedCornerShape(50),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "${settings.brandTitle} • ${settings.brandSubtitle}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = darkAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WelcomeLogoHeroCard(
    settings: HomeScreenSettingsEntity,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF001027),
        border = BorderStroke(2.dp, Color(settings.ctaHighlightHex).copy(alpha = 0.75f)),
        shadowElevation = 14.dp,
        modifier = modifier
    ) {
        Image(
            painter = painterResource(id = R.drawable.halmaal_brand_logo),
            contentDescription = "Hal-maal Digital Publishing Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun WelcomeCopyAndCta(
    settings: HomeScreenSettingsEntity,
    spec: AdaptiveSpec,
    primaryColor: Color,
    darkAccent: Color,
    highlightGold: Color,
    onStartClick: () -> Unit
) {
    val compact = spec.isNarrowScreen || spec.isShortScreen
    val rawTitle = settings.welcomeTitle.ifBlank { DEFAULT_WELCOME_TITLE }.trim()

    val headlineStyle = remember(spec.isNarrowScreen, spec.isShortScreen, spec.useTwoPaneLayout) {
        val (fontSize, lineHeight, tracking) = when {
            spec.isNarrowScreen -> Triple(19.sp, 27.sp, (-0.15).sp)
            spec.isShortScreen -> Triple(20.sp, 28.sp, (-0.15).sp)
            spec.useTwoPaneLayout && spec.maxWidth >= 840.dp -> Triple(27.sp, 37.sp, (-0.3).sp)
            spec.useTwoPaneLayout -> Triple(22.sp, 31.sp, (-0.2).sp)
            else -> Triple(23.sp, 32.sp, (-0.25).sp)
        }
        TextStyle(
            fontFamily = PlayfairDisplayFontFamily,
            fontSize = fontSize,
            lineHeight = lineHeight,
            letterSpacing = tracking,
            lineBreak = LineBreak.Heading
        )
    }

    val styledHeadline = remember(rawTitle, primaryColor, darkAccent, highlightGold) {
        buildEditorialHeadline(
            title = rawTitle,
            primaryColor = primaryColor,
            darkAccent = darkAccent,
            highlightGold = highlightGold
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Editorial colophon ornament separating visual hero from literary statement
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .widthIn(max = 180.dp)
                .padding(bottom = if (compact) 8.dp else 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(highlightGold.copy(alpha = 0.55f))
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .size(6.dp)
                    .rotate(45f)
                    .background(highlightGold)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(highlightGold.copy(alpha = 0.55f))
            )
        }

        Text(
            text = styledHeadline,
            style = headlineStyle,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = 460.dp)
                .padding(horizontal = 4.dp)
                .semantics { contentDescription = rawTitle }
                .testTag("welcome_headline_text")
        )

        Box(
            modifier = Modifier
                .padding(top = if (compact) 10.dp else 14.dp, bottom = if (compact) 8.dp else 12.dp)
                .width(44.dp)
                .height(2.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(highlightGold.copy(alpha = 0.25f), highlightGold, highlightGold.copy(alpha = 0.25f))
                    ),
                    shape = CircleShape
                )
        )

        Text(
            text = settings.welcomeSubtitle.ifBlank {
                "World best writers, works and write entertaining literature for you"
            },
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 20.sp,
                letterSpacing = 0.15.sp
            ),
            color = darkAccent.copy(alpha = 0.74f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = 380.dp)
                .padding(horizontal = 8.dp)
        )

        Spacer(Modifier.height(if (compact) 16.dp else 24.dp))

        Text(
            text = settings.welcomeButtonText.ifBlank { "Lets start" }.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            ),
            color = darkAccent.copy(alpha = 0.85f)
        )
        Spacer(Modifier.height(10.dp))

        IconButton(
            onClick = onStartClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = darkAccent,
                contentColor = Color.White
            ),
            modifier = Modifier
                .size(if (compact) 56.dp else 62.dp)
                .shadow(8.dp, CircleShape)
                .border(1.5.dp, highlightGold.copy(alpha = 0.72f), CircleShape)
                .testTag("welcome_start_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = settings.welcomeButtonText,
                modifier = Modifier.size(if (compact) 24.dp else 26.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

private fun buildEditorialHeadline(
    title: String,
    primaryColor: Color,
    darkAccent: Color,
    highlightGold: Color
): AnnotatedString {
    val normalized = title.replace(Regex("\\s+"), " ").trim()
    val defaultNormalized = DEFAULT_WELCOME_TITLE.replace(Regex("\\s+"), " ").trim()

    if (normalized.equals(defaultNormalized, ignoreCase = true)) {
        // Balanced 3-stanza literary cadence preserving every character of the Somali tagline:
        // Lead-in -> Core aspiration -> Closing resolution
        return buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = darkAccent.copy(alpha = 0.82f),
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.1.sp
                )
            ) {
                append("Buugaagta dhijitaalka ah ee\n")
            }
            withStyle(
                SpanStyle(
                    color = darkAccent,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append("kor u qaada maskaxda,\n")
            }
            withStyle(
                SpanStyle(
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                )
            ) {
                append("dhisa nolosha.")
            }
        }
    }

    // For custom admin headlines: emphasize the concluding clause after a comma if present
    val commaIdx = title.lastIndexOf(',')
    return buildAnnotatedString {
        if (commaIdx in 1 until title.lastIndex) {
            withStyle(SpanStyle(color = darkAccent, fontWeight = FontWeight.SemiBold)) {
                append(title.substring(0, commaIdx + 1))
            }
            withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) {
                append(title.substring(commaIdx + 1))
            }
        } else {
            withStyle(SpanStyle(color = darkAccent, fontWeight = FontWeight.Bold)) {
                append(title)
            }
        }
    }
}
