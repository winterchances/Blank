package com.blank.gallery

import com.blank.gallery.data.MediaLibraryRepository
import com.blank.gallery.data.MediaPermission
import com.blank.gallery.ui.navigation.BottomNavigation
import com.blank.gallery.ui.navigation.MainPage
import com.blank.gallery.ui.screens.AlbumsContent
import com.blank.gallery.ui.screens.FavoritesContent
import com.blank.gallery.ui.screens.HomeContent
import com.blank.gallery.ui.screens.OrganizeContent
import com.blank.gallery.ui.screens.OrganizeSubPage
import com.blank.gallery.ui.screens.OrganizeSubPageContent
import com.blank.gallery.ui.theme.PageBackground
import com.blank.gallery.ui.theme.MainText
import com.blank.gallery.ui.theme.ThemeMint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { BlankApp() }
    }
}

@Composable
private fun BlankApp() {
    val context = LocalContext.current
    val repository = remember(context) { MediaLibraryRepository(context.applicationContext) }

    var currentPage by remember { mutableStateOf(MainPage.HOME) }
    var organizeSubPage by remember { mutableStateOf<OrganizeSubPage?>(null) }
    var hasMediaAccess by remember {
        mutableStateOf(MediaPermission.hasAnyMediaAccess(context))
    }
    var isScanning by remember { mutableStateOf(false) }
    var scannedItems by remember { mutableStateOf(0) }
    var scanMessage by remember { mutableStateOf<String?>(null) }

    val photoCount by repository.photoCount.collectAsState(initial = 0)
    val videoCount by repository.videoCount.collectAsState(initial = 0)
    val totalBytes by repository.totalBytes.collectAsState(initial = 0L)
    val mediaFlow = remember(repository) { repository.pagedMedia }
    val pagedMedia = mediaFlow.collectAsLazyPagingItems()
    val scope = rememberCoroutineScope()

    val runScan: () -> Unit = {
        if (!isScanning) {
            isScanning = true
            scannedItems = 0
            scanMessage = null
            scope.launch {
                try {
                    val total = repository.scanVisibleMedia(
                        includeImages = MediaPermission.canReadImages(context),
                        includeVideos = MediaPermission.canReadVideos(context)
                    ) { count ->
                        scannedItems = count
                    }
                    scanMessage = "扫描完成 · 共检查 $total 项"
                    pagedMedia.refresh()
                } catch (error: SecurityException) {
                    hasMediaAccess = MediaPermission.hasAnyMediaAccess(context)
                    scanMessage = "照片访问权限已变化，请重新授权后扫描"
                } catch (error: Exception) {
                    scanMessage = "扫描未完成，请稍后重试"
                } finally {
                    isScanning = false
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasMediaAccess = MediaPermission.hasAnyMediaAccess(context)
        if (hasMediaAccess) {
            runScan()
        } else {
            scanMessage = "尚未获得照片访问权限"
        }
    }

    val requestAccessOrScan: () -> Unit = {
        if (hasMediaAccess) {
            runScan()
        } else {
            permissionLauncher.launch(MediaPermission.requiredPermissions())
        }
    }

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
                                photoCount = photoCount,
                                videoCount = videoCount,
                                totalBytes = totalBytes,
                                hasMediaAccess = hasMediaAccess,
                                isScanning = isScanning,
                                scannedItems = scannedItems,
                                scanMessage = scanMessage,
                                onScanClick = requestAccessOrScan,
                                onOpenSmart = { currentPage = MainPage.ORGANIZE },
                                onOpenSelf = { currentPage = MainPage.ORGANIZE }
                            )
                            MainPage.ALBUMS -> AlbumsContent(
                                media = pagedMedia,
                                hasMediaAccess = hasMediaAccess
                            )
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
