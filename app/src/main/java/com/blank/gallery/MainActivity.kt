package com.blank.gallery

import com.blank.gallery.data.MediaAsset
import com.blank.gallery.data.MediaDeletionManager
import com.blank.gallery.data.MediaLibraryRepository
import com.blank.gallery.data.MediaPermission
import com.blank.gallery.ui.navigation.BottomNavigation
import com.blank.gallery.ui.navigation.MainPage
import com.blank.gallery.ui.screens.AlbumBatchActionBar
import com.blank.gallery.ui.screens.AlbumDeleteConfirmationSheet
import com.blank.gallery.ui.screens.AlbumsContent
import com.blank.gallery.ui.screens.FavoritesContent
import com.blank.gallery.ui.screens.HomeContent
import com.blank.gallery.ui.screens.OrganizeContent
import com.blank.gallery.ui.screens.OrganizeSubPage
import com.blank.gallery.ui.screens.OrganizeSubPageContent
import com.blank.gallery.ui.theme.PageBackground
import com.blank.gallery.ui.theme.MainText
import com.blank.gallery.ui.theme.ThemeMint

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.blur
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
    val deletionManager = remember(context) { MediaDeletionManager(context.applicationContext) }

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
    var deleteConfirmationVisible by remember { mutableStateOf(false) }
    var isDeletionInProgress by remember { mutableStateOf(false) }
    var pendingTrashIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var legacyDeleteQueue by remember { mutableStateOf<List<MediaAsset>>(emptyList()) }
    var legacyDeletedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var continueLegacyDelete by remember { mutableStateOf<(() -> Unit)?>(null) }
    var finishLegacyDelete by remember { mutableStateOf<(() -> Unit)?>(null) }
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

    val trashRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val ids = pendingTrashIds
        pendingTrashIds = emptySet()
        isDeletionInProgress = false
        if (result.resultCode == Activity.RESULT_OK && ids.isNotEmpty()) {
            scope.launch {
                repository.hideFromLibraryForIds(ids.toList())
                selectedMedia = selectedMedia - ids
                pagedMedia.refresh()
            }
        }
    }

    val legacyDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val continuation = continueLegacyDelete
        continueLegacyDelete = null
        if (result.resultCode == Activity.RESULT_OK) continuation?.invoke()
        else finishLegacyDelete?.invoke()
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

    fun finishLegacyDeleteFlow() {
        val deletedIds = legacyDeletedIds
        scope.launch {
            repository.hideFromLibraryForIds(deletedIds.toList())
            selectedMedia = selectedMedia - deletedIds
            pagedMedia.refresh()
            legacyDeleteQueue = emptyList()
            legacyDeletedIds = emptySet()
            finishLegacyDelete = null
            continueLegacyDelete = null
            isDeletionInProgress = false
        }
    }

    fun processNextLegacyDelete() {
        val asset = legacyDeleteQueue.firstOrNull()
        if (asset == null) {
            finishLegacyDeleteFlow()
            return
        }
        scope.launch {
            when (val attempt = deletionManager.deleteLegacyItem(asset.contentUri)) {
                MediaDeletionManager.LegacyDeleteResult.Deleted -> {
                    legacyDeletedIds = legacyDeletedIds + asset.id
                    legacyDeleteQueue = legacyDeleteQueue.drop(1)
                    processNextLegacyDelete()
                }
                MediaDeletionManager.LegacyDeleteResult.Missing -> {
                    legacyDeleteQueue = legacyDeleteQueue.drop(1)
                    processNextLegacyDelete()
                }
                is MediaDeletionManager.LegacyDeleteResult.RequiresConsent -> {
                    continueLegacyDelete = {
                        scope.launch {
                            when (deletionManager.deleteLegacyItem(asset.contentUri)) {
                                MediaDeletionManager.LegacyDeleteResult.Deleted -> {
                                    legacyDeletedIds = legacyDeletedIds + asset.id
                                    legacyDeleteQueue = legacyDeleteQueue.drop(1)
                                    processNextLegacyDelete()
                                }
                                MediaDeletionManager.LegacyDeleteResult.Missing -> {
                                    legacyDeleteQueue = legacyDeleteQueue.drop(1)
                                    processNextLegacyDelete()
                                }
                                else -> finishLegacyDeleteFlow()
                            }
                        }
                    }
                    legacyDeleteLauncher.launch(
                        IntentSenderRequest.Builder(attempt.intentSender).build()
                    )
                }
                MediaDeletionManager.LegacyDeleteResult.Failed -> finishLegacyDeleteFlow()
            }
        }
    }

    fun confirmBatchDelete() {
        deleteConfirmationVisible = false
        val eligible = selectedMedia.values.filterNot { it.isProtected }
        if (eligible.isEmpty() || isDeletionInProgress) return
        isDeletionInProgress = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val request = deletionManager.createTrashRequest(eligible.map { it.contentUri })
                pendingTrashIds = eligible.map { it.id }.toSet()
                trashRequestLauncher.launch(
                    IntentSenderRequest.Builder(request.intentSender).build()
                )
            } catch (_: Exception) {
                isDeletionInProgress = false
                scanMessage = "无法发起系统回收站请求，请稍后重试"
            }
        } else {
            // Android 10 has no shared MediaStore trash API; each non-owned item
            // may require its own RecoverableSecurityException consent prompt.
            legacyDeleteQueue = eligible
            legacyDeletedIds = emptySet()
            finishLegacyDelete = { finishLegacyDeleteFlow() }
            processNextLegacyDelete()
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

    BackHandler(enabled = deleteConfirmationVisible) {
        deleteConfirmationVisible = false
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
                        .blur(if (deleteConfirmationVisible) 5.dp else 0.dp)
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

                AnimatedContent(
                    targetState = organizeSubPage == null && currentPage == MainPage.ALBUMS && selectedMedia.isNotEmpty(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .blur(if (deleteConfirmationVisible) 5.dp else 0.dp)
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        ),
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith
                            fadeOut(animationSpec = tween(200))
                    },
                    label = "album-selection-navigation"
                ) { selectionMode ->
                    if (selectionMode) {
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
                            onDelete = { deleteConfirmationVisible = true },
                            deleteEnabled = selectedMedia.values.any { !it.isProtected } && !isDeletionInProgress,
                            onCancel = { selectedMedia = emptyMap() }
                        )
                    } else if (organizeSubPage == null) {
                        BottomNavigation(
                            currentPage = currentPage,
                            onSelect = { selectedMedia = emptyMap(); currentPage = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                AlbumDeleteConfirmationSheet(
                    visible = deleteConfirmationVisible,
                    deletableCount = selectedMedia.values.count { !it.isProtected },
                    protectedCount = selectedMedia.values.count { it.isProtected },
                    isAndroid11OrNewer = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
                    onDismiss = { deleteConfirmationVisible = false },
                    onConfirm = { confirmBatchDelete() }
                )
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
