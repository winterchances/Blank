package com.blank.gallery.ui.navigation

import com.blank.gallery.ui.theme.MainText
import com.blank.gallery.ui.theme.GlassWhite

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class MainPage(val title: String, val icon: NavIconType) {
    HOME("首页", NavIconType.HOME),
    ALBUMS("相册", NavIconType.ALBUMS),
    ORGANIZE("整理", NavIconType.ORGANIZE),
    FAVORITES("收藏", NavIconType.FAVORITES)
}

internal enum class NavIconType { HOME, ALBUMS, ORGANIZE, FAVORITES }

private val BlankHomeIcon: ImageVector = ImageVector.Builder("BlankHome", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(3f, 9f); lineTo(12f, 2f); lineTo(21f, 9f); lineTo(21f, 20f)
        quadTo(21f, 22f, 19f, 22f); lineTo(5f, 22f); quadTo(3f, 22f, 3f, 20f)
        close()
        moveTo(9f, 22f); lineTo(9f, 12f); lineTo(15f, 12f); lineTo(15f, 22f)
    }
}.build()

private val BlankAlbumsIcon: ImageVector = ImageVector.Builder("BlankAlbums", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(4.5f, 2f); lineTo(19.5f, 2f); quadTo(22f, 2f, 22f, 4.5f)
        lineTo(22f, 19.5f); quadTo(22f, 22f, 19.5f, 22f); lineTo(4.5f, 22f)
        quadTo(2f, 22f, 2f, 19.5f); lineTo(2f, 4.5f); quadTo(2f, 2f, 4.5f, 2f); close()
        moveTo(7f, 2f); lineTo(7f, 22f); moveTo(17f, 2f); lineTo(17f, 22f)
        moveTo(2f, 12f); lineTo(22f, 12f)
        moveTo(2f, 7f); lineTo(7f, 7f); moveTo(2f, 17f); lineTo(7f, 17f)
        moveTo(17f, 7f); lineTo(22f, 7f); moveTo(17f, 17f); lineTo(22f, 17f)
    }
}.build()

private val BlankOrganizeIcon: ImageVector = ImageVector.Builder("BlankOrganize", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(3.5f, 6f); lineTo(5.5f, 8f); lineTo(9f, 4f); moveTo(12f, 6f); lineTo(20f, 6f)
        moveTo(3.5f, 12f); lineTo(5.5f, 14f); lineTo(9f, 10f); moveTo(12f, 12f); lineTo(20f, 12f)
        moveTo(3.5f, 18f); lineTo(5.5f, 20f); lineTo(9f, 16f); moveTo(12f, 18f); lineTo(20f, 18f)
    }
}.build()

private val BlankFavoritesIcon: ImageVector = ImageVector.Builder("BlankFavorites", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(12f, 21f)
        curveTo(10.8f, 19.9f, 4f, 14.1f, 4f, 9.2f)
        curveTo(4f, 6.4f, 5.9f, 4.5f, 8.3f, 4.5f)
        curveTo(10f, 4.5f, 11.2f, 5.5f, 12f, 6.7f)
        curveTo(12.8f, 5.5f, 14f, 4.5f, 15.7f, 4.5f)
        curveTo(18.1f, 4.5f, 20f, 6.4f, 20f, 9.2f)
        curveTo(20f, 14.1f, 13.2f, 19.9f, 12f, 21f)
        close()
    }
}.build()

private val BlankSearchIcon: ImageVector = ImageVector.Builder("BlankSearch", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(11f, 3f)
        curveTo(15.42f, 3f, 19f, 6.58f, 19f, 11f)
        curveTo(19f, 15.42f, 15.42f, 19f, 11f, 19f)
        curveTo(6.58f, 19f, 3f, 15.42f, 3f, 11f)
        curveTo(3f, 6.58f, 6.58f, 3f, 11f, 3f)
        close()
        moveTo(21f, 21f); lineTo(16.65f, 16.65f)
    }
}.build()

@Composable
internal fun BottomNavigation(
    currentPage: MainPage,
    onSelect: (MainPage) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 62.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(GlassWhite)
            .padding(4.dp)
    ) {
        val slot = maxWidth / 4
        val target = slot * MainPage.values().indexOf(currentPage)
        val offset by animateDpAsState(
            target,
            tween(300, easing = androidx.compose.animation.core.CubicBezierEasing(.25f, 1f, .5f, 1f)),
            label = "nav-indicator"
        )
        Box(
            Modifier.offset(x = offset)
                .width(slot)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.06f))
        )
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            MainPage.values().forEach { page ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .clickable { onSelect(page) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    NavGlyph(page.icon, tint = MainText)
                    Spacer(Modifier.height(0.dp))
                    Text(
                        text = page.title,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MainText
                    )
                }
            }
        }
    }
    androidx.compose.material3.Surface(
        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).size(56.dp).clickable(onClick = onSearchClick),
        shape = CircleShape,
        color = GlassWhite,
        shadowElevation = 8.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(BlankSearchIcon, "搜索", Modifier.size(21.dp), tint = MainText)
        }
    }
    }
}

@Composable
private fun NavGlyph(type: NavIconType, tint: Color) {
    val icon = when (type) {
        NavIconType.HOME -> BlankHomeIcon
        NavIconType.ALBUMS -> BlankAlbumsIcon
        NavIconType.ORGANIZE -> BlankOrganizeIcon
        NavIconType.FAVORITES -> BlankFavoritesIcon
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = tint
    )
}
