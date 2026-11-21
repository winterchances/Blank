package com.blank.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PageBackground = Color(0xFFF5F5F7)
private val MainText = Color(0xFF1A1A1A)
private val ThemeMint = Color(0xFF7FB5B1)
private val MutedText = Color(0xFF888888)
private val GlassWhite = Color.White.copy(alpha = 0.85f)

private enum class MainPage(val title: String, val icon: NavIconType) {
    HOME("首页", NavIconType.HOME),
    ALBUMS("相册", NavIconType.ALBUMS),
    ORGANIZE("整理", NavIconType.ORGANIZE),
    FAVORITES("收藏", NavIconType.FAVORITES)
}

private enum class NavIconType { HOME, ALBUMS, ORGANIZE, FAVORITES }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { BlankApp() }
    }
}

@Composable
private fun BlankApp() {
    var currentPage by remember { mutableStateOf(MainPage.HOME) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = ThemeMint,
            background = PageBackground,
            surface = PageBackground,
            onBackground = MainText,
            onSurface = MainText
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = PageBackground) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                            bottom = 104.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        )
                ) {
                    PageHeader(currentPage.title)
                    when (currentPage) {
                        MainPage.HOME -> HomeContent()
                        MainPage.ALBUMS -> AlbumsContent()
                        MainPage.ORGANIZE -> OrganizeContent()
                        MainPage.FAVORITES -> FavoritesContent()
                    }
                }

                BottomNavigation(
                    currentPage = currentPage,
                    onSelect = { currentPage = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        )
                )
            }
        }
    }
}

@Composable
private fun PageHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 20.dp),
        fontSize = 30.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.5.sp,
        color = MainText
    )
}

@Composable
private fun HomeContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SectionCard(title = "相册概览") {
            EmptyState(
                message = "照片整理完成后，这里会显示你的相册概况",
                compact = true
            )
        }
        SectionCard(title = "整理") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureTile(
                    title = "智能整理",
                    description = "查找重复、相似和其他待整理照片",
                    modifier = Modifier.weight(1f)
                )
                FeatureTile(
                    title = "自己整理",
                    description = "一张一张决定照片的去留",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AlbumsContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        EmptyState(
            message = "相册中的照片会显示在这里",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun OrganizeContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        FeatureTile(
            title = "智能整理",
            description = "查找重复、相似、连拍、截图和占用空间较大的照片",
            modifier = Modifier.fillMaxWidth(),
            tall = true
        )
        FeatureTile(
            title = "自己整理",
            description = "一次查看一张照片，由你决定保留、待删除或跳过",
            modifier = Modifier.fillMaxWidth(),
            tall = true
        )
    }
}

@Composable
private fun FavoritesContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        FeatureTile(
            title = "我的收藏",
            description = "你喜欢的照片会集中保存在这里",
            modifier = Modifier.fillMaxWidth(),
            tall = true
        )
        FeatureTile(
            title = "保护清单",
            description = "受保护的照片不会进入清理候选",
            modifier = Modifier.fillMaxWidth(),
            tall = true
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.03f),
                spotColor = Color.Black.copy(alpha = 0.03f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(GlassWhite)
            .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MainText)
        content()
    }
}

@Composable
private fun FeatureTile(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    tall: Boolean = false
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.025f),
                spotColor = Color.Black.copy(alpha = 0.025f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(GlassWhite)
            .border(1.dp, Color.Black.copy(alpha = 0.045f), RoundedCornerShape(18.dp))
            .padding(horizontal = 15.dp, vertical = if (tall) 20.dp else 15.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MainText)
        Text(description, fontSize = 13.sp, lineHeight = 19.sp, color = MutedText)
    }
}

@Composable
private fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 8.dp else 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 42.dp else 54.dp)
                .clip(CircleShape)
                .background(ThemeMint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text("⌁", color = ThemeMint, fontSize = if (compact) 24.sp else 30.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            color = MutedText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun BottomNavigation(
    currentPage: MainPage,
    onSelect: (MainPage) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(GlassWhite)
            .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MainPage.values().forEach { page ->
            val selected = page == currentPage
            val indicatorColor by animateColorAsState(
                targetValue = if (selected) Color.Black.copy(alpha = 0.06f) else Color.Transparent,
                animationSpec = tween(250),
                label = "nav-indicator-color"
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
                    .clickable { onSelect(page) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                NavGlyph(page.icon, tint = MainText)
                Spacer(Modifier.height(1.dp))
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

@Composable
private fun NavGlyph(type: NavIconType, tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 1.8.dp.toPx()
        val w = size.width
        val h = size.height
        val line = Stroke(width = stroke, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)

        when (type) {
            NavIconType.HOME -> {
                val path = Path().apply {
                    moveTo(w * 0.12f, h * 0.45f)
                    lineTo(w * 0.5f, h * 0.13f)
                    lineTo(w * 0.88f, h * 0.45f)
                    lineTo(w * 0.82f, h * 0.45f)
                    lineTo(w * 0.82f, h * 0.88f)
                    lineTo(w * 0.18f, h * 0.88f)
                    lineTo(w * 0.18f, h * 0.45f)
                    close()
                }
                drawPath(path, tint, style = line)
                drawLine(tint, Offset(w * 0.42f, h * 0.88f), Offset(w * 0.42f, h * 0.61f), stroke)
                drawLine(tint, Offset(w * 0.58f, h * 0.88f), Offset(w * 0.58f, h * 0.61f), stroke)
            }
            NavIconType.ALBUMS -> {
                val path = Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(androidx.compose.ui.geometry.Rect(Offset(w * 0.12f, h * 0.12f), androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.76f)), w * 0.08f, h * 0.08f))
                    moveTo(w * 0.5f, h * 0.12f); lineTo(w * 0.5f, h * 0.88f)
                    moveTo(w * 0.12f, h * 0.5f); lineTo(w * 0.88f, h * 0.5f)
                }
                drawPath(path, tint, style = line)
            }
            NavIconType.ORGANIZE -> {
                drawLine(tint, Offset(w * 0.18f, h * 0.28f), Offset(w * 0.82f, h * 0.28f), stroke)
                drawLine(tint, Offset(w * 0.18f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), stroke)
                drawLine(tint, Offset(w * 0.18f, h * 0.72f), Offset(w * 0.82f, h * 0.72f), stroke)
                drawCircle(tint, radius = stroke * 1.4f, center = Offset(w * 0.38f, h * 0.28f))
                drawCircle(tint, radius = stroke * 1.4f, center = Offset(w * 0.65f, h * 0.5f))
                drawCircle(tint, radius = stroke * 1.4f, center = Offset(w * 0.45f, h * 0.72f))
            }
            NavIconType.FAVORITES -> {
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.86f)
                    cubicTo(w * 0.4f, h * 0.76f, w * 0.13f, h * 0.56f, w * 0.13f, h * 0.34f)
                    cubicTo(w * 0.13f, h * 0.12f, w * 0.42f, h * 0.08f, w * 0.5f, h * 0.3f)
                    cubicTo(w * 0.58f, h * 0.08f, w * 0.87f, h * 0.12f, w * 0.87f, h * 0.34f)
                    cubicTo(w * 0.87f, h * 0.56f, w * 0.6f, h * 0.76f, w * 0.5f, h * 0.86f)
                    close()
                }
                drawPath(path, tint, style = line)
            }
        }
    }
}
