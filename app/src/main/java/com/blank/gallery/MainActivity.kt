package com.blank.gallery

import com.blank.gallery.data.MediaAsset
import com.blank.gallery.data.HomeOrganizeSummary
import com.blank.gallery.data.MediaDeletionManager
import com.blank.gallery.data.MediaLibraryRepository
import com.blank.gallery.data.MediaPermission
import com.blank.gallery.data.SelfOrganizeSummary
import com.blank.gallery.domain.SelfOrganizeDecision
import com.blank.gallery.domain.SelfOrganizeManager
import com.blank.gallery.domain.SelfOrganizeMode
import com.blank.gallery.domain.SmartAnalysisType
import com.blank.gallery.domain.SmartAnalysisTaskState
import com.blank.gallery.ui.navigation.BottomNavigation
import com.blank.gallery.ui.navigation.MainPage
import com.blank.gallery.ui.screens.AlbumBatchActionBar
import com.blank.gallery.ui.screens.AlbumDeleteConfirmationSheet
import com.blank.gallery.ui.screens.AlbumFolderDrawer
import com.blank.gallery.ui.screens.AlbumHeaderActionButtons
import com.blank.gallery.ui.screens.AlbumOptionsSheet
import com.blank.gallery.ui.screens.AlbumSearchSheet
import com.blank.gallery.ui.screens.AlbumsContent
import com.blank.gallery.ui.screens.FavoritesContent
import com.blank.gallery.ui.screens.SmartAnalysisScreen
import com.blank.gallery.ui.screens.HomeContent
import com.blank.gallery.ui.screens.OrganizeContent
import com.blank.gallery.ui.screens.OrganizeSubPage
import com.blank.gallery.ui.screens.OrganizeSubPageContent
import com.blank.gallery.ui.screens.SelfOrganizeScreen
import com.blank.gallery.ui.screens.SelfOrganizePendingDeleteScreen
import com.blank.gallery.ui.theme.PageBackground
import com.blank.gallery.ui.theme.MainText
import com.blank.gallery.ui.theme.ThemeMint
import com.blank.gallery.ui.theme.blankGlassSurface
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlankApp() {
    val context = LocalContext.current
    val hazeState = remember { HazeState() }
    val albumDrawerState = androidx.compose.material3.rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    var isAlbumSearchVisible by remember { mutableStateOf(false) }
    var albumOptionsSheet by remember { mutableStateOf<AlbumOptionsSheetType?>(null) }
    val repository = remember(context) { MediaLibraryRepository(context.applicationContext) }
    val deletionManager = remember(context) { MediaDeletionManager(context.applicationContext) }
    val selfOrganizeManager = remember(repository) { SelfOrganizeManager(repository) }
    val selfOrganizeSummary by selfOrganizeManager.summary.collectAsState(initial = SelfOrganizeSummary())
    val canUndoSelfOrganize by selfOrganizeManager.canUndo.collectAsState(initial = false)
    val selfOrganizePendingDelete by selfOrganizeManager.pendingDelete.collectAsState(initial = emptyList())
    val favorites by repository.favorites.collectAsState(initial = emptyList())
    val protectedMedia by repository.protectedMedia.collectAsState(initial = emptyList())
    val smartAnalysisStates by repository.smartAnalysisStates.collectAsState()
    var smartResults by remember { mutableStateOf<Map<OrganizeSubPage, List<List<MediaAsset>>>>(emptyMap()) }
    var smartAnalyzedPages by remember { mutableStateOf<Set<OrganizeSubPage>>(emptySet()) }
    var smartErrors by remember { mutableStateOf<Map<OrganizeSubPage, String>>(emptyMap()) }


    var currentPage by remember { mutableStateOf(MainPage.HOME) }
    var organizeSubPage by remember { mutableStateOf<OrganizeSubPage?>(null) }
    var hasMediaAccess by remember {
        mutableStateOf(MediaPermission.hasAnyMediaAccess(context))
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var scannedItems by remember { mutableStateOf(0) }
    var scanMessage by remember { mutableStateOf<String?>(null) }
    var selfOrganizePhoto by remember { mutableStateOf<MediaAsset?>(null) }
    var selfOrganizePreviewPhotos by remember { mutableStateOf<List<MediaAsset>>(emptyList()) }
    var isSelfOrganizeLoading by remember { mutableStateOf(false) }
    var selfOrganizeMessage by remember { mutableStateOf<String?>(null) }
    var isShowingSelfPendingDelete by remember { mutableStateOf(false) }
    var selfOrganizeDeleteConfirmationVisible by remember { mutableStateOf(false) }

    val photoCount by repository.photoCount.collectAsState(initial = 0)
    val videoCount by repository.videoCount.collectAsState(initial = 0)
    val photoBytes by repository.photoBytes.collectAsState(initial = 0L)
    val videoBytes by repository.videoBytes.collectAsState(initial = 0L)
    val homeOrganizeSummary by repository.homeOrganizeSummary.collectAsState(initial = HomeOrganizeSummary())
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
    var pendingTrashQueue by remember { mutableStateOf<List<List<MediaAsset>>>(emptyList()) }
    var launchNextTrashBatch by remember { mutableStateOf<(() -> Unit)?>(null) }
    var legacyDeleteQueue by remember { mutableStateOf<List<MediaAsset>>(emptyList()) }
    var legacyDeletedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var continueLegacyDelete by remember { mutableStateOf<(() -> Unit)?>(null) }
    var finishLegacyDelete by remember { mutableStateOf<(() -> Unit)?>(null) }
    val mediaFlow = remember(repository, albumMediaType, albumSearchQuery, selectedFolderPath, selectedMonthKey, albumSortBySize) {
        repository.pagedMedia(albumMediaType, albumSearchQuery, selectedFolderPath, selectedMonthKey, albumSortBySize)
    }
    val pagedMedia = mediaFlow.collectAsLazyPagingItems()
    val scope = rememberCoroutineScope()
    val hasPhotoAccess = hasMediaAccess
    val activeSelfOrganizeMode = when (organizeSubPage) {
        OrganizeSubPage.SELF_RANDOM -> SelfOrganizeMode.RANDOM
        OrganizeSubPage.SELF_ORDERED -> SelfOrganizeMode.ORDERED
        else -> null
    }


    val runScan: (forceFullScan: Boolean) -> Unit = { forceFullScan ->
        if (!isScanning) {
            isScanning = true
            scannedItems = 0
            scanMessage = null
            scope.launch {
                try {
                    val total = repository.scanVisibleMedia(
                        includeImages = MediaPermission.canReadImages(context),
                        includeVideos = MediaPermission.canReadVideos(context),
                        permissionScope = MediaPermission.accessScope(context),
                        forceFullScan = forceFullScan
                    ) { count ->
                        scannedItems = count
                    }
                    scanMessage = when {
                        !MediaPermission.hasFullLibraryAccess(context) ->
                            "当前为部分访问 · 已扫描授权范围内的 $scannedItems 项"
                        total == 0 -> "相册已是最新"
                        else -> "扫描完成 · 更新 $total 项"
                    }
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
        if (result.resultCode == Activity.RESULT_OK && ids.isNotEmpty()) {
            scope.launch {
                repository.hideFromLibraryForIds(ids.toList())
                selectedMedia = selectedMedia - ids
                pagedMedia.refresh()
                pendingTrashQueue = pendingTrashQueue.drop(1)
                launchNextTrashBatch?.invoke()
            }
        } else {
            pendingTrashQueue = emptyList()
            isDeletionInProgress = false
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

    fun smartTypeFor(page: OrganizeSubPage): SmartAnalysisType? = when (page) {
        OrganizeSubPage.DUPLICATES -> SmartAnalysisType.DUPLICATES
        OrganizeSubPage.SIMILAR -> SmartAnalysisType.SIMILAR_PHOTOS
        OrganizeSubPage.BURSTS -> SmartAnalysisType.BURST_PHOTOS
        OrganizeSubPage.SCREENSHOTS -> SmartAnalysisType.SCREENSHOTS
        OrganizeSubPage.LARGE_FILES -> SmartAnalysisType.LARGE_MEDIA
        else -> null
    }

    fun runSmartAnalysis(page: OrganizeSubPage) {
        scope.launch {
            try {
                val groups = when (page) {
                    OrganizeSubPage.DUPLICATES -> repository.analyzeDuplicateMedia().groups.map { it.items }
                    OrganizeSubPage.SIMILAR -> repository.analyzeSimilarPhotos().groups.map { it.items }
                    OrganizeSubPage.BURSTS -> repository.analyzeBurstPhotos().groups.map { it.items }
                    OrganizeSubPage.SCREENSHOTS -> repository.analyzeScreenshots().matches.map { listOf(it.asset) }
                    OrganizeSubPage.LARGE_FILES -> repository.analyzeLargeMedia().items.map { listOf(it) }
                    else -> emptyList()
                }.map { group -> group.distinctBy { it.id } }.filter { it.isNotEmpty() }
                smartResults = smartResults + (page to groups)
                smartAnalyzedPages = smartAnalyzedPages + page
                smartErrors = smartErrors - page
            } catch (error: Exception) {
                smartErrors = smartErrors + (page to (error.message ?: "分析失败，请稍后重试"))
            }
        }
    }

    fun decideSmartAsset(asset: MediaAsset, status: Int) {
        scope.launch {
            try {
                repository.setSelfOrganizeStatus(asset.id, status)
                smartResults = smartResults.mapValues { (_, groups) ->
                    groups.mapNotNull { group ->
                        group.filterNot { it.id == asset.id }.takeIf { it.isNotEmpty() }
                    }
                }
            } catch (_: Exception) {
                organizeSubPage?.let { page -> smartErrors = smartErrors + (page to "保存整理决定失败，请重试") }
            }
        }
    }

    LaunchedEffect(organizeSubPage, lastScanAt) {
        val page = organizeSubPage
        if (lastScanAt != null && page != null && smartTypeFor(page) != null && page !in smartAnalyzedPages) runSmartAnalysis(page)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasMediaAccess = MediaPermission.hasAnyMediaAccess(context)
        if (hasMediaAccess) {
            permissionDenied = false
            runScan(true)
        } else {
            permissionDenied = true
            scanMessage = "尚未获得照片访问权限"
        }
    }

    fun finishLegacyDeleteFlow(message: String? = null) {
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
            if (message != null) {
                scanMessage = message
                selfOrganizeMessage = message
            }
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
                    // The file was already removed outside the app; reconcile its stale local row.
                    legacyDeletedIds = legacyDeletedIds + asset.id
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
                                    // The file was already removed outside the app; reconcile its stale local row.
                                    legacyDeletedIds = legacyDeletedIds + asset.id
                                    legacyDeleteQueue = legacyDeleteQueue.drop(1)
                                    processNextLegacyDelete()
                                }
                                else -> finishLegacyDeleteFlow(
                                    "部分项目未能移至回收站，未处理的项目仍保留在列表中"
                                )
                            }
                        }
                    }
                    legacyDeleteLauncher.launch(
                        IntentSenderRequest.Builder(attempt.intentSender).build()
                    )
                }
                MediaDeletionManager.LegacyDeleteResult.Failed ->
                    finishLegacyDeleteFlow("部分项目无法移至回收站，未处理的项目仍保留在列表中")
            }
        }
    }

    fun startDeleteItems(items: List<MediaAsset>) {
        val eligible = items.filter {
            it.isVisible && it.mediaType in 1..2 && !it.isProtected
        }
        if (eligible.isEmpty() || isDeletionInProgress) return
        isDeletionInProgress = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 16+ limits each MediaStore group request to 2,000 items.
            pendingTrashQueue = eligible.chunked(2000)
            launchNextTrashBatch = {
                val batch = pendingTrashQueue.firstOrNull()
                if (batch == null) {
                    isDeletionInProgress = false
                } else {
                    try {
                        val request = deletionManager.createTrashRequest(batch.map { it.contentUri })
                        pendingTrashIds = batch.map { it.id }.toSet()
                        trashRequestLauncher.launch(
                            IntentSenderRequest.Builder(request.intentSender).build()
                        )
                    } catch (_: Exception) {
                        pendingTrashQueue = emptyList()
                        pendingTrashIds = emptySet()
                        isDeletionInProgress = false
                        scanMessage = "无法发起系统回收站请求，请稍后重试"
                        selfOrganizeMessage = "无法发起系统回收站请求，请稍后重试"
                    }
                }
            }
            launchNextTrashBatch?.invoke()
        } else {
            // Android 10 has no shared MediaStore trash API; each non-owned item
            // may require its own RecoverableSecurityException consent prompt.
            legacyDeleteQueue = eligible
            legacyDeletedIds = emptySet()
            finishLegacyDelete = {
                finishLegacyDeleteFlow("系统未完成删除，未处理的项目仍保留在列表中")
            }
            processNextLegacyDelete()
        }
    }

    fun confirmBatchDelete() {
        deleteConfirmationVisible = false
        startDeleteItems(selectedMedia.values.toList())
    }

    fun confirmSelfOrganizePendingDelete() {
        selfOrganizeDeleteConfirmationVisible = false
        if (isSelfOrganizeLoading || isDeletionInProgress) return
        val requestedIds = selfOrganizePendingDelete.map { it.id }
        isSelfOrganizeLoading = true
        scope.launch {
            try {
                // The pending list may have changed while the confirmation
                // sheet was open. Re-check visibility, protection and status.
                val currentItems = repository.currentPendingSelfOrganizePhotos(requestedIds)
                if (currentItems.isEmpty()) {
                    selfOrganizeMessage = "待删除照片已发生变化，请检查列表后重试"
                } else {
                    startDeleteItems(currentItems)
                }
            } catch (_: Exception) {
                selfOrganizeMessage = "无法读取最新的待删除照片，请稍后重试"
            } finally {
                isSelfOrganizeLoading = false
            }
        }
    }

    val requestAccessOrScan: () -> Unit = {
        if (MediaPermission.hasFullLibraryAccess(context)) {
            runScan(true)
        } else {
            val activity = context as? Activity
            val canRequestAgain = activity != null &&
                MediaPermission.requiredPermissions().any { permission ->
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
                }
            if (permissionDenied && !canRequestAgain) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            } else {
                permissionLauncher.launch(MediaPermission.requiredPermissions())
            }
        }
    }

    LaunchedEffect(Unit) {
        val permissionPreferences = context.getSharedPreferences(
            "blank_permission_state",
            android.content.Context.MODE_PRIVATE
        )
        val hasAskedForInitialAccess = permissionPreferences.getBoolean(
            "initial_media_permission_requested",
            false
        )
        if (!MediaPermission.hasFullLibraryAccess(context) && !hasAskedForInitialAccess) {
            // Ask on the first app launch so Android can show the full-library
            // choice (or its selected-photos alternative) before scanning.
            permissionPreferences.edit()
                .putBoolean("initial_media_permission_requested", true)
                .apply()
            permissionLauncher.launch(MediaPermission.requiredPermissions())
        } else if (hasMediaAccess) {
            runScan(false)
        } else {
            repository.hideAllFromCurrentLibrary()
        }
    }

    LaunchedEffect(organizeSubPage, hasPhotoAccess, isScanning) {
        if (activeSelfOrganizeMode != null) {
            selfOrganizeMessage = null
            if (!hasPhotoAccess || isScanning) {
                selfOrganizePhoto = null
                selfOrganizePreviewPhotos = emptyList()
                isSelfOrganizeLoading = false
            } else {
                isSelfOrganizeLoading = true
                val next = selfOrganizeManager.nextPhoto(activeSelfOrganizeMode)
                selfOrganizePhoto = next
                selfOrganizePreviewPhotos = next?.let {
                    selfOrganizeManager.peekNextPhotos(activeSelfOrganizeMode, it.id)
                }.orEmpty()
                isSelfOrganizeLoading = false
            }
        } else {
            selfOrganizePhoto = null
            selfOrganizePreviewPhotos = emptyList()
        }
    }

    fun decideSelfOrganize(decision: SelfOrganizeDecision) {
        val current = selfOrganizePhoto ?: return
        val mode = activeSelfOrganizeMode ?: return
        if (isSelfOrganizeLoading) return
        selfOrganizeMessage = null
        isSelfOrganizeLoading = true
        scope.launch {
            try {
                selfOrganizeManager.applyDecision(current.id, decision)
                val next = selfOrganizeManager.nextPhoto(mode)
                selfOrganizePhoto = next
                selfOrganizePreviewPhotos = next?.let {
                    selfOrganizeManager.peekNextPhotos(mode, it.id)
                }.orEmpty()
            } catch (_: Exception) {
                selfOrganizeMessage = "整理状态保存失败，请重试"
            } finally {
                isSelfOrganizeLoading = false
            }
        }
    }

    fun undoSelfOrganize() {
        val mode = activeSelfOrganizeMode ?: return
        if (!canUndoSelfOrganize || isSelfOrganizeLoading) return
        selfOrganizeMessage = null
        isSelfOrganizeLoading = true
        scope.launch {
            try {
                val restored = selfOrganizeManager.undo()
                val next = restored ?: selfOrganizeManager.nextPhoto(mode)
                selfOrganizePhoto = next
                selfOrganizePreviewPhotos = next?.let {
                    selfOrganizeManager.peekNextPhotos(mode, it.id)
                }.orEmpty()
            } catch (_: Exception) {
                selfOrganizeMessage = "撤销失败，请稍后重试"
            } finally {
                isSelfOrganizeLoading = false
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val currentHasMediaAccess by androidx.compose.runtime.rememberUpdatedState(hasMediaAccess)
    val currentIsScanning by androidx.compose.runtime.rememberUpdatedState(isScanning)
    val currentRunScan by androidx.compose.runtime.rememberUpdatedState(runScan)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val accessNow = MediaPermission.hasAnyMediaAccess(context)
                if (accessNow != currentHasMediaAccess) {
                    hasMediaAccess = accessNow
                    if (accessNow) {
                        permissionDenied = false
                        currentRunScan(false)
                    } else {
                        selectedMedia = emptyMap()
                        scope.launch { repository.hideAllFromCurrentLibrary() }
                    }
                } else if (accessNow && !currentIsScanning) {
                    currentRunScan(false)
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

    BackHandler(enabled = isShowingSelfPendingDelete) {
        isShowingSelfPendingDelete = false
    }

    BackHandler(enabled = selfOrganizeDeleteConfirmationVisible) {
        selfOrganizeDeleteConfirmationVisible = false
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
        androidx.compose.runtime.CompositionLocalProvider(LocalRippleConfiguration provides null) {
        androidx.compose.material3.ModalNavigationDrawer(
            drawerState = albumDrawerState,
            gesturesEnabled = currentPage == MainPage.ALBUMS && albumFolders.isNotEmpty(),
            drawerContent = {
                AlbumFolderDrawer(
                    folders = albumFolders,
                    selectedFolderPath = selectedFolderPath,
                    onFolderSelect = { path ->
                        selectedMedia = emptyMap()
                        selectedFolderPath = path
                        scope.launch { albumDrawerState.close() }
                    },
                    hazeState = hazeState
                )
            }
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
                        .blur(if (deleteConfirmationVisible || selfOrganizeDeleteConfirmationVisible) 5.dp else 0.dp)
                        .hazeSource(hazeState)
                        .mainPageSwipe(
                            currentPage = currentPage,
                            enabled = organizeSubPage == null && selectedMedia.isEmpty() &&
                                !albumDrawerState.isOpen && !deleteConfirmationVisible &&
                                !selfOrganizeDeleteConfirmationVisible && !isAlbumSearchVisible,
                            onPageChange = { currentPage = it }
                        )
                ) {
                    val selectionMode = currentPage == MainPage.ALBUMS && selectedMedia.isNotEmpty()
                    PageHeader(
                        title = when {
                            isShowingSelfPendingDelete -> "待删除"
                            selectionMode -> "已选 ${selectedMedia.size} 项"
                            else -> organizeSubPage?.title ?: currentPage.title
                        },
                        onBack = when {
                            isShowingSelfPendingDelete -> { { isShowingSelfPendingDelete = false } }
                            selectionMode -> { { selectedMedia = emptyMap() } }
                            organizeSubPage != null -> { { organizeSubPage = null } }
                            else -> null
                        },
                        backLabel = if (selectionMode) "×" else "‹",
                        compactCentered = organizeSubPage != null,
                        showAlbumActions = currentPage == MainPage.ALBUMS && !selectionMode,
                        onViewSortClick = { albumOptionsSheet = AlbumOptionsSheetType.VIEW_SORT },
                        hazeState = hazeState
                    )
                    if (organizeSubPage != null) {
                        if (isShowingSelfPendingDelete) {
                            SelfOrganizePendingDeleteScreen(
                                assets = selfOrganizePendingDelete,
                                isLoading = isSelfOrganizeLoading || isDeletionInProgress,
                                feedbackMessage = selfOrganizeMessage,
                                onCancel = { asset ->
                                    if (!isSelfOrganizeLoading && !isDeletionInProgress) {
                                        isSelfOrganizeLoading = true
                                        scope.launch {
                                            try {
                                                selfOrganizeManager.cancelPendingDelete(asset.id)
                                            } catch (_: Exception) {
                                                selfOrganizeMessage = "撤回失败，请稍后重试"
                                            } finally {
                                                isSelfOrganizeLoading = false
                                            }
                                        }
                                    }
                                },
                                onConfirmDelete = { selfOrganizeDeleteConfirmationVisible = true }
                            )
                        } else if (activeSelfOrganizeMode != null) {
                            SelfOrganizeScreen(
                                asset = selfOrganizePhoto,
                                previewAssets = selfOrganizePreviewPhotos,
                                summary = selfOrganizeSummary,
                                canUndo = canUndoSelfOrganize,
                                isLoading = isSelfOrganizeLoading,
                                hasPhotoAccess = hasPhotoAccess,
                                isScanning = isScanning,
                                feedbackMessage = selfOrganizeMessage,
                                onRequestAccess = requestAccessOrScan,
                                onKeep = { decideSelfOrganize(SelfOrganizeDecision.KEEP) },
                                onPendingDelete = { decideSelfOrganize(SelfOrganizeDecision.PENDING_DELETE) },
                                onSkip = { decideSelfOrganize(SelfOrganizeDecision.SKIP) },
                                onUndo = { undoSelfOrganize() },
                                onOpenPendingDelete = { isShowingSelfPendingDelete = true }
                            )
                        } else {
                            val page = organizeSubPage!!
                            val type = smartTypeFor(page)
                            val state = type?.let { smartAnalysisStates[it] }
                            SmartAnalysisScreen(
                                page = page,
                                groups = smartResults[page].orEmpty(),
                                hasAnalyzed = page in smartAnalyzedPages,
                                hasScanned = lastScanAt != null,
                                isRunning = state is SmartAnalysisTaskState.Running || state is SmartAnalysisTaskState.Cancelling,
                                progressText = when (state) {
                                    is SmartAnalysisTaskState.Running -> "已分析 ${state.completed} / ${state.total}"
                                    is SmartAnalysisTaskState.Completed -> "分析完成 · 共检查 ${state.completed} 项"
                                    else -> null
                                },
                                errorText = smartErrors[page] ?: (state as? SmartAnalysisTaskState.Failed)?.message,
                                onStart = { runSmartAnalysis(page) },
                                onCancel = {
                                    type?.let { analysisType -> scope.launch { repository.cancelSmartAnalysis(analysisType) } }
                                },
                                onKeep = { asset -> decideSmartAsset(asset, 1) },
                                onPendingDelete = { asset -> decideSmartAsset(asset, 2) }
                            )
                        }
                    } else {
                        AnimatedContent(
                            targetState = currentPage,
                            modifier = Modifier.weight(1f),
                            transitionSpec = {
                                androidx.compose.animation.slideInHorizontally(
                                    animationSpec = tween(260)
                                ) { it / 5 } + fadeIn(tween(220)) togetherWith
                                    androidx.compose.animation.slideOutHorizontally(
                                        animationSpec = tween(220)
                                    ) { -it / 5 } + fadeOut(tween(180))
                            },
                            label = "blank-page"
                        ) { page ->
                            when (page) {
                            MainPage.HOME -> HomeContent(
                                photoCount = photoCount,
                                videoCount = videoCount,
                                photoBytes = photoBytes,
                                videoBytes = videoBytes,
                                organizeSummary = homeOrganizeSummary,
                                analysisCounts = mapOf(
                                    OrganizeSubPage.DUPLICATES to if (OrganizeSubPage.DUPLICATES in smartAnalyzedPages) smartResults[OrganizeSubPage.DUPLICATES].orEmpty().sumOf { it.size } else null,
                                    OrganizeSubPage.SIMILAR to if (OrganizeSubPage.SIMILAR in smartAnalyzedPages) smartResults[OrganizeSubPage.SIMILAR].orEmpty().sumOf { it.size } else null,
                                    OrganizeSubPage.BURSTS to if (OrganizeSubPage.BURSTS in smartAnalyzedPages) smartResults[OrganizeSubPage.BURSTS].orEmpty().sumOf { it.size } else null,
                                    OrganizeSubPage.SCREENSHOTS to if (OrganizeSubPage.SCREENSHOTS in smartAnalyzedPages) smartResults[OrganizeSubPage.SCREENSHOTS].orEmpty().sumOf { it.size } else null,
                                    OrganizeSubPage.LARGE_FILES to if (OrganizeSubPage.LARGE_FILES in smartAnalyzedPages) smartResults[OrganizeSubPage.LARGE_FILES].orEmpty().sumOf { it.size } else null
                                ),
                                lastScanAt = lastScanAt,
                                hasMediaAccess = hasMediaAccess,
                                hasFullMediaAccess = MediaPermission.hasFullLibraryAccess(context),
                                permissionDenied = permissionDenied,
                                isScanning = isScanning,
                                scannedItems = scannedItems,
                                scanMessage = scanMessage,
                                hazeState = hazeState,
                                onOpenAnalysis = { page ->
                                    selectedMedia = emptyMap()
                                    currentPage = MainPage.ORGANIZE
                                    organizeSubPage = page
                                }
                            )
                            MainPage.ALBUMS -> AlbumsContent(
                                media = pagedMedia,
                                gridColumns = albumGridColumns,
                                hasMediaAccess = hasMediaAccess,
                                mediaType = albumMediaType,
                                onMediaTypeChange = { selectedMedia = emptyMap(); albumMediaType = it },
                                searchQuery = albumSearchQuery,
                                months = albumMonths,
                                selectedMonthKey = selectedMonthKey,
                                selectedFolderPath = selectedFolderPath,
                                onOpenFolders = { scope.launch { albumDrawerState.open() } },
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
                            MainPage.FAVORITES -> FavoritesContent(
                                favorites = favorites,
                                protectedMedia = protectedMedia,
                                onToggleFavorite = { asset -> scope.launch { repository.setFavorite(asset.id, false) } },
                                onToggleProtected = { asset -> scope.launch { repository.setProtected(asset.id, false) } }
                            )
                            }
                        }
                    }
                }

                AnimatedContent(
                    targetState = organizeSubPage == null && currentPage == MainPage.ALBUMS && selectedMedia.isNotEmpty(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .blur(if (deleteConfirmationVisible || selfOrganizeDeleteConfirmationVisible) 5.dp else 0.dp)
                        .padding(
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
                        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
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
                            onCancel = { selectedMedia = emptyMap() },
                            hazeState = hazeState
                        )
                        }
                    } else if (organizeSubPage == null) {
                        BottomNavigation(
                            currentPage = currentPage,
                            onSelect = { selectedMedia = emptyMap(); currentPage = it },
                            onScanClick = requestAccessOrScan,
                            hazeState = hazeState,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (albumOptionsSheet != null) {
                    val sheetType = albumOptionsSheet!!
                    val options = when (sheetType) {
                        AlbumOptionsSheetType.VIEW_SORT -> listOf("四宫格", "三宫格", "按时间排序", "按文件大小排序")
                    }
                    AlbumOptionsSheet(
                        title = "视图与排序",
                        options = options,
                        onSelect = { selected ->
                            selectedMedia = emptyMap()
                            when (selected) {
                                "四宫格" -> albumGridColumns = 4
                                "三宫格" -> albumGridColumns = 3
                                "按时间排序" -> albumSortBySize = false
                                "按文件大小排序" -> albumSortBySize = true
                            }
                            albumOptionsSheet = null
                        },
                        onDismiss = { albumOptionsSheet = null }
                    )
                }

                if (isAlbumSearchVisible) {
                    AlbumSearchSheet(
                        query = albumSearchQuery,
                        onQueryChange = { selectedMedia = emptyMap(); albumSearchQuery = it },
                        onDismiss = { isAlbumSearchVisible = false }
                    )
                }

                AlbumDeleteConfirmationSheet(
                    visible = deleteConfirmationVisible,
                    deletableCount = selectedMedia.values.count { !it.isProtected },
                    protectedCount = selectedMedia.values.count { it.isProtected },
                    isAndroid11OrNewer = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
                    onDismiss = { deleteConfirmationVisible = false },
                    onConfirm = { confirmBatchDelete() }
                )

                AlbumDeleteConfirmationSheet(
                    visible = selfOrganizeDeleteConfirmationVisible,
                    deletableCount = selfOrganizePendingDelete.size,
                    protectedCount = 0,
                    isAndroid11OrNewer = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
                    onDismiss = { selfOrganizeDeleteConfirmationVisible = false },
                    onConfirm = { confirmSelfOrganizePendingDelete() }
                )
            }
        }
        }
        }
    }
}

private enum class AlbumOptionsSheetType { VIEW_SORT }

private fun Modifier.mainPageSwipe(
    currentPage: MainPage,
    enabled: Boolean,
    onPageChange: (MainPage) -> Unit
): Modifier = pointerInput(currentPage, enabled) {
    if (!enabled) return@pointerInput
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var total = Offset.Zero
        val touchSlop = viewConfiguration.touchSlop
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            total += change.positionChange()
            if (abs(total.x) > touchSlop && abs(total.x) > abs(total.y) * 1.2f) {
                val pages = MainPage.values()
                val index = pages.indexOf(currentPage)
                val nextIndex = if (total.x < 0f) index + 1 else index - 1
                if (nextIndex in pages.indices) onPageChange(pages[nextIndex])
                change.consume()
                break
            }
        }
    }
}



@Composable
private fun PageHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    backLabel: String = "‹",
    compactCentered: Boolean = false,
    showAlbumActions: Boolean = false,
    onViewSortClick: () -> Unit = {},
    hazeState: HazeState
) {
    if (compactCentered) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 15.dp)
                .height(40.dp)
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(38.dp)
                        .blankGlassSurface(hazeState, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = backLabel,
                        fontSize = 25.sp,
                        lineHeight = 25.sp,
                        color = MainText
                    )
                }
            }
            Text(
                text = title,
                modifier = Modifier.align(Alignment.Center),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MainText
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 15.dp),
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
                modifier = Modifier.weight(1f),
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                color = MainText
            )
            if (showAlbumActions) {
                AlbumHeaderActionButtons(
                    onViewSortClick = onViewSortClick,
                    hazeState = hazeState
                )
            }
        }
    }
}
