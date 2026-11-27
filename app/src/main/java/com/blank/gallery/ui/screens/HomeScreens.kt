package com.blank.gallery.ui.screens

import com.blank.gallery.data.MediaAsset
import com.blank.gallery.data.MediaMonth
import com.blank.gallery.data.HomeOrganizeSummary
import com.blank.gallery.ui.components.EmptyState
import com.blank.gallery.ui.components.FeatureTile
import com.blank.gallery.ui.theme.MainText
import com.blank.gallery.ui.theme.MutedText
import com.blank.gallery.ui.theme.ThemeMint
import com.blank.gallery.ui.theme.GlassWhite
import com.blank.gallery.ui.theme.PageBackground

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.os.StatFs
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun HomeContent(
    photoCount: Int,
    videoCount: Int,
    photoBytes: Long,
    videoBytes: Long,
    organizeSummary: HomeOrganizeSummary,
    analysisCounts: Map<OrganizeSubPage, Int?>,
    pendingDeleteCount: Int,
    onOpenPendingDelete: () -> Unit,
    lastScanAt: Long?,
    hasMediaAccess: Boolean,
    hasFullMediaAccess: Boolean,
    permissionDenied: Boolean,
    isScanning: Boolean,
    scannedItems: Int,
    scanMessage: String?,
    hazeState: HazeState,
    onOpenAnalysis: (OrganizeSubPage) -> Unit
) {
    val context = LocalContext.current
    val storage = runCatching {
        val storagePath = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
        val stat = StatFs(storagePath)
        stat.blockSizeLong * stat.blockCountLong to stat.availableBytes
    }.getOrDefault(0L to 0L)
    val deviceTotalBytes = storage.first.coerceAtLeast(0L)
    val deviceAvailableBytes = storage.second.coerceIn(0L, deviceTotalBytes)
    val deviceUsedBytes = (deviceTotalBytes - deviceAvailableBytes).coerceAtLeast(0L)
    val photoSegment = photoBytes.coerceAtLeast(0L).coerceAtMost(deviceUsedBytes)
    val videoSegment = videoBytes.coerceAtLeast(0L).coerceAtMost((deviceUsedBytes - photoSegment).coerceAtLeast(0L))
    val otherSegment = (deviceUsedBytes - photoSegment - videoSegment).coerceAtLeast(0L)
    val progress = if (organizeSummary.totalCount > 0) {
        (organizeSummary.completedCount.toFloat() / organizeSummary.totalCount).coerceIn(0f, 1f)
    } else 0f
    val progressPercent = (progress * 100f).toInt()
    val analysisItems = listOf(
        OrganizeSubPage.DUPLICATES to "重复照片",
        OrganizeSubPage.SIMILAR to "相似照片",
        OrganizeSubPage.BURSTS to "连拍照片",
        OrganizeSubPage.SCREENSHOTS to "截图",
        OrganizeSubPage.LARGE_FILES to "大文件"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HomeGlassCard(hazeState) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("整理进度", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MainText)
                Text("$progressPercent%", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MainText)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(ThemeMint.copy(alpha = 0.14f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(ThemeMint)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("已整理 ${organizeSummary.completedCount} 项", fontSize = 11.sp, color = MutedText)
                Text("待整理 ${organizeSummary.remainingCount} 项", fontSize = 11.sp, color = MutedText)
            }
        }

        HomeGlassCard(hazeState) {
            Text("智能整理", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MainText)
            analysisItems.forEach { (page, title) ->
                HomeAnalysisItem(
                    title = title,
                    count = analysisCounts[page],
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenAnalysis(page) }
                )
            }
        }

        HomeGlassCard(hazeState) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenPendingDelete)
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("待删除", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MainText)
                    Text("确认后才会真正删除", fontSize = 10.sp, color = MutedText)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$pendingDeleteCount 项", fontSize = 12.sp, color = MutedText)
                    Text("›", fontSize = 20.sp, color = MutedText)
                }
            }
        }

        HomeGlassCard(hazeState) {
            Text("存储情况", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MainText)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(formatStorage(deviceUsedBytes), fontSize = 23.sp, fontWeight = FontWeight.Bold, color = MainText)
                    Text("已使用 / ${formatStorage(deviceTotalBytes)}", fontSize = 10.sp, color = MutedText)
                }
                Text("剩余 ${formatStorage(deviceAvailableBytes)}", fontSize = 11.sp, color = MutedText)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.045f))
            ) {
                Row(Modifier.fillMaxSize()) {
                    if (photoSegment > 0L) {
                        Box(Modifier.weight(photoSegment.toFloat()).fillMaxHeight().background(ThemeMint))
                    }
                    if (videoSegment > 0L) {
                        Box(Modifier.weight(videoSegment.toFloat()).fillMaxHeight().background(ThemeMint.copy(alpha = 0.58f)))
                    }
                    if (otherSegment > 0L) {
                        Box(Modifier.weight(otherSegment.toFloat()).fillMaxHeight().background(Color(0xFFD7CBAF)))
                    }
                    if (deviceAvailableBytes > 0L) {
                        Box(Modifier.weight(deviceAvailableBytes.toFloat()).fillMaxHeight().background(Color(0xFFE8ECEB)))
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeStorageRow(color = ThemeMint, label = "照片", detail = "$photoCount 张", value = formatStorage(photoBytes))
                HomeStorageRow(color = ThemeMint.copy(alpha = 0.58f), label = "视频", detail = "$videoCount 个", value = formatStorage(videoBytes))
                HomeStorageRow(color = Color(0xFFD7CBAF), label = "其他及系统占用", detail = "估算", value = formatStorage(otherSegment))
            }
            Text(
                text = "媒体占用按当前已授权并扫描到的文件统计；其他为设备已用空间扣除照片和视频后的估算值。",
                fontSize = 9.sp,
                lineHeight = 13.sp,
                color = MutedText
            )
        }

        val scanStatus = when {
            isScanning -> "正在扫描 · 已读取 $scannedItems 项"
            scanMessage != null -> scanMessage
            !hasMediaAccess && permissionDenied -> "请在系统设置中开启照片和视频访问权限"
            !hasMediaAccess -> "尚未授权照片和视频访问"
            !hasFullMediaAccess -> "当前为部分访问 · 统计范围受授权限制"
            lastScanAt != null -> "最近扫描 · ${formatScanTime(lastScanAt)}"
            else -> "尚未扫描"
        }
        Text(
            text = scanStatus,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            fontSize = 10.sp,
            color = MutedText
        )
    }
}

@Composable
private fun HomeGlassCard(
    hazeState: HazeState,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.07f),
                spotColor = Color.Black.copy(alpha = 0.05f)
            )
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = PageBackground,
                    tints = emptyList(),
                    blurRadius = 10.dp,
                    noiseFactor = 0f
                )
            )
            .clip(shape)
            .background(GlassWhite)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun HomeAnalysisItem(
    title: String,
    count: Int?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MainText)
        Text(
            text = count?.let { "$it 项" } ?: "待分析",
            fontSize = 10.sp,
            color = MutedText
        )
    }
}

@Composable
private fun HomeStorageRow(
    color: Color,
    label: String,
    detail: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(label, fontSize = 11.sp, color = MainText)
                Text(detail, fontSize = 9.sp, color = MutedText)
            }
        }
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MainText)
    }
}

private fun formatScanTime(timestamp: Long): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun formatStorage(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1024L * 1024L -> String.format("%.0f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format("%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun AlbumsContent(
    media: LazyPagingItems<AlbumGridItem>,
    gridColumns: Int,
    hasMediaAccess: Boolean,
    mediaType: Int?,
    onMediaTypeChange: (Int?) -> Unit,
    searchQuery: String,
    months: List<MediaMonth>,
    selectedMonthKey: String?,
    selectedFolderPath: String?,
    onOpenFolders: () -> Unit,
    onMonthSelect: (String?) -> Unit,
    selectedIds: Set<Long>,
    onToggleSelection: (MediaAsset) -> Unit
) {
    if (!hasMediaAccess) {
        EmptyState(
            message = "开启照片访问权限后，这里会显示本机相册",
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    val previewAssets = media.itemSnapshotList.items
        .filterIsInstance<AlbumGridItem.Media>()
        .map { it.asset }
    var previewStartId by remember { mutableStateOf<Long?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 2.dp, bottom = 25.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "folder-entry") {
                AlbumFilterChip("文件夹", selectedFolderPath != null, onClick = onOpenFolders)
            }
            if (months.isNotEmpty()) {
                item(key = "all-months") {
                    AlbumFilterChip("全部月份", selectedMonthKey == null) { onMonthSelect(null) }
                }
                lazyItems(items = months, key = { it.monthKey }) { month ->
                    AlbumFilterChip(
                        label = month.monthKey.replace("-", "年") + "月",
                        selected = selectedMonthKey == month.monthKey
                    ) { onMonthSelect(month.monthKey) }
                }
            }
        }

        if (media.itemCount == 0) {
            val message = when {
                media.loadState.refresh is LoadState.Loading -> "正在加载相册内容"
                media.loadState.refresh is LoadState.Error -> "相册暂时无法加载，请稍后重试"
                searchQuery.isNotBlank() -> "没有找到匹配的照片"
                mediaType != null -> "当前分类还没有照片"
                else -> "还没有照片，先到首页开始扫描"
            }
            EmptyState(
                message = message,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            return
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 5.dp),
            verticalArrangement = Arrangement.spacedBy(if (gridColumns == 4) 8.dp else 10.dp),
            horizontalArrangement = Arrangement.spacedBy(if (gridColumns == 4) 8.dp else 10.dp)
        ) {
            items(
                count = media.itemCount,
                key = { index ->
                    when (val entry = media.peek(index)) {
                        is AlbumGridItem.Media -> "media-${entry.asset.id}"
                        is AlbumGridItem.DateHeader -> "date-${entry.date}"
                        null -> "placeholder-$index"
                    }
                },
                span = { index ->
                    if (media.peek(index) is AlbumGridItem.DateHeader) {
                        GridItemSpan(maxLineSpan)
                    } else {
                        GridItemSpan(1)
                    }
                }
            ) { index ->
                when (val entry = media[index]) {
                    is AlbumGridItem.DateHeader -> {
                        Text(
                            text = entry.label,
                            modifier = Modifier.fillMaxWidth().padding(start = 2.dp, top = 10.dp, bottom = 5.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MutedText
                        )
                    }
                    is AlbumGridItem.Media -> {
                        val asset = entry.asset
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(5.dp))
                                .background(GlassWhite)
                                .combinedClickable(
                                    onClick = {
                                        if (selectedIds.isNotEmpty()) onToggleSelection(asset)
                                        else previewStartId = asset.id
                                    },
                                    onLongClick = { onToggleSelection(asset) }
                                )
                        ) {
                            AsyncImage(
                                model = asset.contentUri,
                                contentDescription = asset.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (asset.mediaType == 2) {
                                Text(
                                    text = "▶",
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            }
                            if (asset.id in selectedIds) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f))
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(5.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(ThemeMint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    null -> Spacer(Modifier.aspectRatio(1f))
                }
            }
        }
    }

    previewStartId?.let { startId ->
        AlbumMediaPreviewDialog(
            assets = previewAssets,
            initialAssetId = startId,
            onDismiss = { previewStartId = null }
        )
    }
}

@Composable
private fun AlbumMediaPreviewDialog(
    assets: List<MediaAsset>,
    initialAssetId: Long,
    onDismiss: () -> Unit
) {
    if (assets.isEmpty()) return
    val initialPage = assets.indexOfFirst { it.id == initialAssetId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { assets.size })
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { page -> assets[page].id }
            ) { page ->
                val asset = assets[page]
                var scale by remember(asset.id) { mutableStateOf(1f) }
                var offset by remember(asset.id) { mutableStateOf(Offset.Zero) }
                val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                    scale = (scale * zoomChange).coerceIn(1f, 5f)
                    offset = if (scale <= 1f) Offset.Zero else offset + panChange
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(asset.id) {
                            detectTapGestures(
                                onDoubleTap = {
                                    scale = if (scale > 1f) 1f else 2.5f
                                    if (scale <= 1f) offset = Offset.Zero
                                }
                            )
                        }
                        .transformable(state = transformState),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = asset.contentUri,
                        contentDescription = asset.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                    )
                    if (asset.mediaType == 2) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable {
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.parse(asset.contentUri), "video/*")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▶", color = Color.White, fontSize = 25.sp)
                        }
                    }
                }
            }

            Text(
                text = "×",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 42.dp, end = 18.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(onClick = onDismiss),
                color = Color.White,
                fontSize = 26.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "${pagerState.currentPage + 1} / ${assets.size}",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
internal fun SectionHeading(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(3.dp).height(14.dp).clip(CircleShape).background(ThemeMint))
        Text(
            text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MainText,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun AlbumMediaTypeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (selected) MainText else MutedText,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 15.sp
        )
        if (selected) {
            Spacer(
                Modifier
                    .padding(top = 5.dp)
                    .width(20.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(ThemeMint)
            )
        }
    }
}

@Composable
private fun AlbumFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    androidx.compose.material3.Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) ThemeMint else Color.White,
        shadowElevation = if (selected) 1.5.dp else 0.dp
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) Color.White else Color(0xFF666666),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
internal fun FavoritesContent() {
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
                ambientColor = Color.Black.copy(alpha = 0.07f),
                spotColor = Color.Black.copy(alpha = 0.05f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(GlassWhite)
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
            .shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.03f), spotColor = Color.Black.copy(alpha = 0.03f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 15.dp),
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
        Text("›", fontSize = 19.sp, color = MutedText)
    }
}
