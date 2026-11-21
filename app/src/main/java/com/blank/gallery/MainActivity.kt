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
internal val MainText = Color(0xFF1A1A1A)
internal val ThemeMint = Color(0xFF7FB5B1)
internal val MutedText = Color(0xFF888888)
internal val GlassWhite = Color.White.copy(alpha = 0.85f)

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
internal fun EmptyState(
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

