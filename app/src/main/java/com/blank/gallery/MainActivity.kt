package com.blank.gallery

import com.blank.gallery.data.MediaAsset
import com.blank.gallery.data.MediaLibraryRepository
import com.blank.gallery.data.MediaPermission
import com.blank.gallery.ui.navigation.BottomNavigation
import com.blank.gallery.ui.navigation.MainPage
import com.blank.gallery.ui.screens.AlbumBatchActionBar
import com.blank.gallery.ui.screens.AlbumsContent
import com.blank.gallery.ui.screens.FavoritesContent
import com.blank.gallery.ui.screens.HomeContent
import com.blank.gallery.ui.screens.OrganizeContent
import com.blank.gallery.ui.screens.OrganizeSubPage
import com.blank.gallery.ui.screens.OrganizeSubPageContent
import com.blank.gallery.ui.theme.PageBackground
import com.blank.gallery.ui.theme.MainText
import com.blank.gallery.ui.theme.ThemeMint

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    var permissionDenied by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var scannedItems by remember { mutableStateOf(0) }
    var scanMessage by remember { mutableStateOf<String?>(null) }

    val photoCount by repository.photoCount.collectAsState(initial = 0)
    val videoCount by repository.videoCount.collectAsState(initial = 0)
    val totalBytes by repository.totalBytes.collectAsState(initial = 0L)
    val lastScanAt by repository.lastScanAt.collectAsState()
    val albumFolders by repository.folders.collectAsState(initial = emptyList())
    val albumMonths by repository.months.collectAsState(initial = emptyList())
    var selectedMonthKey by remember { mutableStateOf<String?>(null) }
    var selectedFolderPath by remember { mutableStateOf<String?>(null) }
    var albumMediaType by remember { mutableStateOf<Int?>(null) }
    var albumSearchQuery by remember { mutableStateOf("") }
    var albumGridColumns by remember { mutableStateOf(4) }
    var albumSortBySize by remember { mutableStateOf(false) }
    var selectedMedia by remember { mutableStateOf<Map<Long, MediaAsset>>(emptyMap()) }
    val mediaFlow = remember(repository, albumMediaType, albumSearchQuery, selectedFolderPath, selectedMonthKey, albumSortBySize) {
        repository.pagedMedia(albumMediaType, albumSearchQuery, selectedFolderPath, selectedMonthKey, albumSortBySize)
    }
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
                        includeVideos = MediaPermission.canReadVideos(context),
                        permissionScope = MediaPermission.accessScope(context)
                    ) { count ->
                        scannedItems = count
                    }
                    scanMessage = if (total == 0) "相册已是最新" else "扫描完成 · 更新 $total 项"
                    pagedMedia.refresh()
                } catch (error: SecurityException) {
                    hasMediaAccess = MediaPermission.hasAnyMediaAccess(context)
                    selectedMedia = emptyMap()
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
            permissionDenied = false
            runScan()
        } else {
            permissionDenied = true
            scanMessage = "尚未获得照片访问权限"
        }
    }

    val requestAccessOrScan: () -> Unit = {
        if (hasMediaAccess) {
            runScan()
        } else if (permissionDenied) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        } else {
            permissionLauncher.launch(MediaPermission.requiredPermissions())
        }
    }

    LaunchedEffect(Unit) {
        if (hasMediaAccess) {
            runScan()
        } else {
            repository.hideAllFromCurrentLibrary()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val accessNow = MediaPermission.hasAnyMediaAccess(context)
                if (accessNow != hasMediaAccess) {
                    hasMediaAccess = accessNow
                    if (accessNow) {
                        permissionDenied = false
                        runScan()
                    } else {
                        selectedMedia = emptyMap()
                        scope.launch { repository.hideAllFromCurrentLibrary() }
                    }
                } else if (accessNow && !isScanning) {
                    runScan()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = selectedMedia.isNotEmpty()) {
        selectedMedia = emptyMap()
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
                    val selectionMode = currentPage == MainPage.ALBUMS && selectedMedia.isNotEmpty()
                    PageHeader(
                        title = if (selectionMode) "已选 ${selectedMedia.size} 项" else organizeSubPage?.title ?: currentPage.title,
                        onBack = when {
                            selectionMode -> { { selectedMedia = emptyMap() } }
                            organizeSubPage != null -> { { organizeSubPage = null } }
                            else -> null
                        },
                        backLabel = if (selectionMode) "×" else "‹"
                    )
                    if (organizeSubPage != null) {
                        OrganizeSubPageContent(page = organizeSubPage!!)
                    } else {
                        when (currentPage) {
                            MainPage.HOME -> HomeContent(
                                photoCount = photoCount,
                                videoCount = videoCount,
                                totalBytes = totalBytes,
                                lastScanAt = lastScanAt,
                                hasMediaAccess = hasMediaAccess,
                                permissionDenied = permissionDenied,
                                isScanning = isScanning,
                                scannedItems = scannedItems,
                                scanMessage = scanMessage,
                                onScanClick = requestAccessOrScan,
                                onOpenSmart = { currentPage = MainPage.ORGANIZE },
                                onOpenSelf = { currentPage = MainPage.ORGANIZE }
                            )
                            MainPage.ALBUMS -> AlbumsContent(
                                media = pagedMedia,
                                hasMediaAccess = hasMediaAccess,
                                mediaType = albumMediaType,
                                onMediaTypeChange = { selectedMedia = emptyMap(); albumMediaType = it },
                                searchQuery = albumSearchQuery,
                                onSearchQueryChange = { selectedMedia = emptyMap(); albumSearchQuery = it },
                                gridColumns = albumGridColumns,
                                onGridColumnsChange = { albumGridColumns = it },
                                sortBySize = albumSortBySize,
                                onSortBySizeChange = { albumSortBySize = it },
                                folders = albumFolders,
                                selectedFolderPath = selectedFolderPath,
                                onFolderSelect = { selectedMedia = emptyMap(); selectedFolderPath = it },
                                months = albumMonths,
                                selectedMonthKey = selectedMonthKey,
                                onMonthSelect = { selectedMedia = emptyMap(); selectedMonthKey = it },
                                selectedIds = selectedMedia.keys,
                                onToggleSelection = { asset ->
                                    selectedMedia = if (asset.id in selectedMedia) {
                                        selectedMedia - asset.id
                                    } else {
                                        selectedMedia + (asset.id to asset)
                                    }
                                }
                            )
                            MainPage.ORGANIZE -> OrganizeContent(
                                onOpenCategory = { organizeSubPage = it },
                                onOpenSelfMode = { organizeSubPage = it }
                            )
                            MainPage.FAVORITES -> FavoritesContent()
                        }
                    }
                }

                if (organizeSubPage == null && currentPage == MainPage.ALBUMS && selectedMedia.isNotEmpty()) {
                    val allFavorite = selectedMedia.values.all { it.isFavorite }
                    val allProtected = selectedMedia.values.all { it.isProtected }
                    AlbumBatchActionBar(
                        favoriteLabel = if (allFavorite) "取消收藏" else "收藏",
                        protectLabel = if (allProtected) "取消保护" else "保护",
                        onFavorite = {
                            val nextValue = !allFavorite
                            val ids = selectedMedia.keys.toList()
                            scope.launch {
                                repository.setFavoriteForIds(ids, nextValue)
                                selectedMedia = selectedMedia.mapValues { (_, asset) -> asset.copy(isFavorite = nextValue) }
                                pagedMedia.refresh()
                            }
                        },
                        onProtect = {
                            val nextValue = !allProtected
                            val ids = selectedMedia.keys.toList()
                            scope.launch {
                                repository.setProtectedForIds(ids, nextValue)
                                selectedMedia = selectedMedia.mapValues { (_, asset) -> asset.copy(isProtected = nextValue) }
                                pagedMedia.refresh()
                            }
                        },
                        onCancel = { selectedMedia = emptyMap() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                            )
                    )
                } else if (organizeSubPage == null) {
                    BottomNavigation(
                        currentPage = currentPage,
                        onSelect = { selectedMedia = emptyMap(); currentPage = it },
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
private fun PageHeader(title: String, onBack: (() -> Unit)? = null, backLabel: String = "‹") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Text(
                text = backLabel,
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
