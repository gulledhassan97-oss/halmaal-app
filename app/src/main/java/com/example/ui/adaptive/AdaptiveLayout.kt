package com.example.ui.adaptive

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WidthSizeTier {
    NARROW,    // < 360.dp (compact/small phones, narrow split-screen)
    COMPACT,   // 360.dp .. 599.dp (standard portrait phones)
    MEDIUM,    // 600.dp .. 839.dp (foldables, small tablets, wide landscape phones)
    EXPANDED   // >= 840.dp (tablets, desktops, wide landscape)
}

enum class HeightSizeTier {
    COMPACT_SHORT, // < 480.dp (landscape phones, split-screen top/bottom)
    STANDARD,      // 480.dp .. 799.dp
    TALL           // >= 800.dp
}

@Immutable
data class AdaptiveSpec(
    val maxWidth: Dp,
    val maxHeight: Dp,
    val widthTier: WidthSizeTier,
    val heightTier: HeightSizeTier
) {
    val isLandscape: Boolean
        get() = maxWidth > maxHeight

    val isShortScreen: Boolean
        get() = heightTier == HeightSizeTier.COMPACT_SHORT

    val isNarrowScreen: Boolean
        get() = widthTier == WidthSizeTier.NARROW

    val useNavigationRail: Boolean
        get() = maxWidth >= 600.dp

    val useTwoPaneLayout: Boolean
        get() = maxWidth >= 700.dp || (isLandscape && maxWidth >= 540.dp)

    val horizontalPadding: Dp
        get() = when (widthTier) {
            WidthSizeTier.NARROW -> 12.dp
            WidthSizeTier.COMPACT -> 16.dp
            WidthSizeTier.MEDIUM -> 24.dp
            WidthSizeTier.EXPANDED -> 32.dp
        }

    val maxContentWidth: Dp
        get() = when (widthTier) {
            WidthSizeTier.NARROW, WidthSizeTier.COMPACT -> maxWidth
            WidthSizeTier.MEDIUM -> 920.dp
            WidthSizeTier.EXPANDED -> 1180.dp
        }

    val catalogGridColumns: Int
        get() = when {
            maxWidth >= 960.dp -> 4
            maxWidth >= 680.dp -> 3
            else -> 2
        }

    val recommendedColumns: Int
        get() = when {
            maxWidth >= 920.dp -> 3
            maxWidth >= 600.dp -> 2
            else -> 1
        }
}

fun BoxWithConstraintsScope.rememberAdaptiveSpec(): AdaptiveSpec {
    val widthTier = when {
        maxWidth < 360.dp -> WidthSizeTier.NARROW
        maxWidth < 600.dp -> WidthSizeTier.COMPACT
        maxWidth < 840.dp -> WidthSizeTier.MEDIUM
        else -> WidthSizeTier.EXPANDED
    }
    val heightTier = when {
        maxHeight < 480.dp -> HeightSizeTier.COMPACT_SHORT
        maxHeight < 800.dp -> HeightSizeTier.STANDARD
        else -> HeightSizeTier.TALL
    }
    return AdaptiveSpec(
        maxWidth = maxWidth,
        maxHeight = maxHeight,
        widthTier = widthTier,
        heightTier = heightTier
    )
}
