package com.blank.gallery.ui.screens

import com.blank.gallery.data.MediaAsset
import com.blank.gallery.ui.components.EmptyState
import com.blank.gallery.ui.components.FeatureTile
import com.blank.gallery.ui.theme.MainText
import com.blank.gallery.ui.theme.MutedText
import com.blank.gallery.ui.theme.ThemeMint
import com.blank.gallery.ui.theme.GlassWhite

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SectionCard(title = "相册概览") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OverviewMetric("照片", photoCount.toString())
                OverviewMetric("视频", videoCount.toString())
                OverviewMetric("占用空间", formatStorage(totalBytes))
            }
            Text(
                text = lastScanAt?.let { "最近扫描 · ${formatScanTime(it)}" } ?: "尚未扫描",
                fontSize = 11.sp,
                color = MutedText
            )
            if (!hasMediaAccess) {
                Text(
                    text = "照片和视频仅在本机读取，不会上传",
                    fontSize = 12.sp,
                    color = MutedText
                )
            }
            if (isScanning) {
                Text(
                    text = "正在扫描 · 已读取 $scannedItems 项",
                    fontSize = 12.sp,
                    color = MutedText
                )
            } else if (scanMessage != null) {
                Text(text = scanMessage, fontSize = 12.sp, color = MutedText)
            }
            Text(
                text = when {
                    isScanning -> "扫描中…"
                    !hasMediaAccess && permissionDenied -> "前往系统设置开启照片访问权限"
                    !hasMediaAccess -> "开启照片访问权限后，即可建立本机相册索引"
                    photoCount + videoCount == 0 -> "扫描本机照片和视频"
                    else -> "检查更新"
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ThemeMint.copy(alpha = 0.10f))
                    .clickable(enabled = !isScanning, onClick = onScanClick)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MainText
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
private fun OverviewMetric(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = MutedText)
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MainText)
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
internal fun AlbumsContent(
    media: LazyPagingItems<MediaAsset>,
    hasMediaAccess: Boolean,
    mediaType: Int?,
    onMediaTypeChange: (Int?) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    gridColumns: Int,
    onGridColumnsChange: (Int) -> Unit,
    sortBySize: Boolean,
    onSortBySizeChange: (Boolean) -> Unit
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AlbumFilterChip("全部", mediaType == null) { onMediaTypeChange(null) }
            AlbumFilterChip("照片", mediaType == 1) { onMediaTypeChange(1) }
            AlbumFilterChip("视频", mediaType == 2) { onMediaTypeChange(2) }
            Spacer(Modifier.weight(1f))
            AlbumFilterChip("四宫格", gridColumns == 4) { onGridColumnsChange(4) }
            AlbumFilterChip("三宫格", gridColumns == 3) { onGridColumnsChange(3) }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GlassWhite)
                .padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⌕", color = MutedText, fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = MainText,
                    fontSize = 13.sp
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (searchQuery.isEmpty()) {
                            Text("搜索照片或视频", color = MutedText, fontSize = 13.sp)
                        }
                        innerTextField()
                    }
                }
            )
            if (searchQuery.isNotEmpty()) {
                Text(
                    text = "清除",
                    modifier = Modifier.clickable { onSearchQueryChange("") },
                    color = MutedText,
                    fontSize = 12.sp
                )
            }
            AlbumFilterChip("时间", !sortBySize) { onSortBySizeChange(false) }
            AlbumFilterChip("大小", sortBySize) { onSortBySizeChange(true) }
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
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(count = media.itemCount) { index ->
                val asset = media[index]
                if (asset != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(5.dp))
                            .background(GlassWhite)
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
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp),
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.aspectRatio(1f))
                }
            }
        }
    }
}

@Composable
private fun AlbumFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) ThemeMint.copy(alpha = 0.13f) else GlassWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) MainText else MutedText
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
