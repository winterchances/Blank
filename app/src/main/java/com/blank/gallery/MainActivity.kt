package com.blank.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
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

private enum class OrganizeSubPage(val title: String) {
    DUPLICATES("重复照片"),
    SIMILAR("相似照片"),
    BURSTS("连拍"),
    SCREENSHOTS("截图"),
    LARGE_FILES("大文件"),
    SELF_ORDERED("按顺序整理"),
    SELF_RANDOM("随机整理")
}

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
    var organizeSubPage by remember { mutableStateOf<OrganizeSubPage?>(null) }

    BackHandler(enabled = organizeSubPage != null) {
        organizeSubPage = null
    }

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
                            bottom = (if (organizeSubPage == null) 104.dp else 24.dp) +
                                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        )
                ) {
                    PageHeader(
                        title = organizeSubPage?.title ?: currentPage.title,
                        onBack = if (organizeSubPage != null) {
                            { organizeSubPage = null }
                        } else null
                    )
                    if (organizeSubPage != null) {
                        OrganizeSubPageContent(page = organizeSubPage!!)
                    } else {
                        when (currentPage) {
                            MainPage.HOME -> HomeContent(
                                onOpenSmart = { currentPage = MainPage.ORGANIZE },
                                onOpenSelf = { currentPage = MainPage.ORGANIZE }
                            )
                            MainPage.ALBUMS -> AlbumsContent()
                            MainPage.ORGANIZE -> OrganizeContent(
                                onOpenCategory = { organizeSubPage = it },
                                onOpenSelfMode = { organizeSubPage = it }
                            )
                            MainPage.FAVORITES -> FavoritesContent()
                        }
                    }
                }

                if (organizeSubPage == null) {
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
}

@Composable
private fun PageHeader(title: String, onBack: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Text(
                text = "‹",
                modifier = Modifier
                    .padding(end = 10.dp)
                    .clickable(onClick = onBack),
                fontSize = 34.sp,
                lineHeight = 34.sp,
                color = MainText
            )
        }
        Text(
            text = title,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            color = MainText
        )
    }
}

@Composable
private fun HomeContent(
    onOpenSmart: () -> Unit,
    onOpenSelf: () -> Unit
) {
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
        SectionCard(title = "快捷整理") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAction(
                    title = "智能整理",
                    onClick = onOpenSmart,
                    modifier = Modifier.weight(1f)
                )
                QuickAction(
                    title = "自己整理",
                    onClick = onOpenSelf,
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
private fun OrganizeContent(
    onOpenCategory: (OrganizeSubPage) -> Unit,
    onOpenSelfMode: (OrganizeSubPage) -> Unit
) {
    val smartItems = listOf(
        Triple(OrganizeSubPage.DUPLICATES, "重复照片", "查找内容完全相同的照片"),
        Triple(OrganizeSubPage.SIMILAR, "相似照片", "找出画面相近的照片"),
        Triple(OrganizeSubPage.BURSTS, "连拍照片", "集中整理连续拍摄的照片"),
        Triple(OrganizeSubPage.SCREENSHOTS, "截图", "集中查看保存的截图"),
        Triple(OrganizeSubPage.LARGE_FILES, "大文件", "找出占用空间较大的照片")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "智能整理",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MainText
            )
            smartItems.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { (page, title, description) ->
                        OrganizeCategoryTile(
                            title = title,
                            description = description,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenCategory(page) }
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "自己整理",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MainText
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OrganizeCategoryTile(
                    title = "按顺序整理",
                    description = "按照照片时间顺序，逐张浏览",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSelfMode(OrganizeSubPage.SELF_ORDERED) }
                )
                OrganizeCategoryTile(
                    title = "随机整理",
                    description = "随机选择一张照片作为起点",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSelfMode(OrganizeSubPage.SELF_RANDOM) }
                )
            }
        }
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
private fun QuickAction(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ThemeMint.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MainText
        )
        Spacer(Modifier.width(5.dp))
        Text("›", fontSize = 19.sp, color = ThemeMint)
    }
}

@Composable
private fun OrganizeSubPageContent(page: OrganizeSubPage) {
    val message = when (page) {
        OrganizeSubPage.SELF_ORDERED ->
            "扫描完成后，将按照照片时间顺序，从尚未整理且未受保护的照片开始"
        OrganizeSubPage.SELF_RANDOM ->
            "扫描完成后，从尚未整理且未受保护的照片中随机选择起点，之后也会继续随机抽取"
        else ->
            "尚未扫描，扫描完成后会显示待整理内容"
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        EmptyState(
            message = message,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun OrganizeCategoryTile(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.02f),
                spotColor = Color.Black.copy(alpha = 0.02f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(GlassWhite)
            .border(1.dp, Color.Black.copy(alpha = 0.045f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MainText
            )
            Text("›", fontSize = 20.sp, color = MutedText)
        }
        Text(
            text = description,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = MutedText
        )
    }
}

@Composable
private fun FeatureTile(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    tall: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
