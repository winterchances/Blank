package com.blank.gallery.ui.screens

import com.blank.gallery.data.MediaAsset
import com.blank.gallery.data.MediaMonth
import com.blank.gallery.ui.components.EmptyState
import com.blank.gallery.ui.components.FeatureTile
import com.blank.gallery.ui.theme.MainText
import com.blank.gallery.ui.theme.MutedText
import com.blank.gallery.ui.theme.ThemeMint
import com.blank.gallery.ui.theme.GlassWhite

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    totalBytes: Long,
    lastScanAt: Long?,
    hasMediaAccess: Boolean,
    hasFullMediaAccess: Boolean,
    permissionDenied: Boolean,
    isScanning: Boolean,
    scannedItems: Int,
    scanMessage: String?,
    onScanClick: () -> Unit,
    onOpenSmart: () -> Unit,
    onOpenSelf: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(GlassWhite)
                .padding(horizontal = 15.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OverviewMetric("照片", photoCount.toString(), Modifier.weight(1f))
            OverviewMetric("视频", videoCount.toString(), Modifier.weight(1f))
            OverviewMetric("占用空间", formatStorage(totalBytes), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = lastScanAt?.let { "最近扫描 · ${formatScanTime(it)}" } ?: "尚未扫描",
            fontSize = 11.sp,
            color = MutedText
        )
        if (!hasMediaAccess) {
            Text("照片和视频仅在本机读取，不会上传", fontSize = 12.sp, color = MutedText)
        } else if (!hasFullMediaAccess) {
            Text("目前只能读取已授权的部分照片和视频", fontSize = 12.sp, color = MutedText)
        }
        if (isScanning) {
            Text("正在扫描 · 已读取 $scannedItems 项", fontSize = 12.sp, color = MutedText)
        } else if (scanMessage != null) {
            Text(scanMessage, fontSize = 12.sp, color = MutedText)
        }
        Text(
            text = when {
                isScanning -> "扫描中…"
                !hasMediaAccess && permissionDenied -> "前往系统设置开启照片和视频访问权限"
                !hasMediaAccess -> "允许访问照片和视频后，即可建立本机相册索引"
                !hasFullMediaAccess -> "允许访问全部照片和视频"
                photoCount + videoCount == 0 -> "扫描本机照片和视频"
                else -> "检查更新"
            },
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ThemeMint.copy(alpha = 0.10f))
                .clickable(enabled = !isScanning, onClick = onScanClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MainText
        )
        Spacer(Modifier.height(25.dp))
        SectionHeading("快捷整理")
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

@Composable
private fun OverviewMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = MutedText)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF111111), maxLines = 1)
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

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 15.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(25.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumMediaTypeTab("全部", mediaType == null) { onMediaTypeChange(null) }
            AlbumMediaTypeTab("照片", mediaType == 1) { onMediaTypeChange(1) }
            AlbumMediaTypeTab("视频", mediaType == 2) { onMediaTypeChange(2) }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 2.dp, bottom = 25.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                searchQuery.isNotBlank() -> "没有找到匹配的照片或视频"
                mediaType != null -> "当前分类还没有媒体内容"
                else -> "还没有相册内容，先到首页开始扫描"
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MainText
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
                                    onClick = { if (selectedIds.isNotEmpty()) onToggleSelection(asset) },
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
}

@Composable
internal fun SectionHeading(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 25.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(4.dp).height(16.dp).clip(CircleShape).background(ThemeMint))
        Text(
            text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
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
            fontSize = 16.sp
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
        shadowElevation = if (selected) 3.dp else 1.dp
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else Color(0xFF666666),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
            .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.03f), spotColor = Color.Black.copy(alpha = 0.03f))
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
