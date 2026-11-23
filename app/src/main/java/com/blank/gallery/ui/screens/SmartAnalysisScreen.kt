package com.blank.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.blank.gallery.data.MediaAsset
import com.blank.gallery.ui.theme.MainText
import com.blank.gallery.ui.theme.MutedText
import com.blank.gallery.ui.theme.ThemeMint

@Composable
internal fun SmartAnalysisScreen(
    page: OrganizeSubPage,
    groups: List<List<MediaAsset>>,
    hasAnalyzed: Boolean,
    hasScanned: Boolean,
    isRunning: Boolean,
    progressText: String?,
    errorText: String?,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onKeep: (MediaAsset) -> Unit,
    onPendingDelete: (MediaAsset) -> Unit
) {
    val description = when (page) {
        OrganizeSubPage.DUPLICATES -> "通过文件内容校验，找出完全相同的照片"
        OrganizeSubPage.SIMILAR -> "分析画面特征，整理内容相近的照片"
        OrganizeSubPage.BURSTS -> "结合拍摄时间与画面相似度识别连拍"
        OrganizeSubPage.SCREENSHOTS -> "根据文件名和所在文件夹识别截图"
        OrganizeSubPage.LARGE_FILES -> "找出占用空间较大的照片和视频"
        else -> ""
    }
    val itemCount = groups.sumOf { it.size }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(description, color = MutedText, fontSize = 12.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    if (isRunning) "正在分析" else if (hasAnalyzed) "找到 ${groups.size} 组 · ${itemCount} 项" else "尚未分析",
                    color = MainText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                progressText?.let { Text(it, color = MutedText, fontSize = 11.sp) }
            }
            Button(
                onClick = if (isRunning) onCancel else onStart,
                enabled = isRunning || hasScanned,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeMint,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(if (isRunning) "取消" else if (hasAnalyzed) "重新分析" else "开始分析")
            }
        }
        if (!hasScanned) Text("请先回到首页完成媒体扫描", color = MutedText, fontSize = 12.sp)
        errorText?.let { Text(it, color = MutedText, fontSize = 12.sp) }
        when {
            isRunning && groups.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("正在分析本机媒体，请稍候", color = MutedText, fontSize = 13.sp)
            }
            hasAnalyzed && groups.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("没有发现符合条件的内容", color = MutedText, fontSize = 13.sp)
            }
            groups.isNotEmpty() -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                itemsIndexed(
                    items = groups,
                    key = { index, group -> "analysis-group-${index}-${group.firstOrNull()?.id ?: 0L}" }
                ) { index, group ->
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (group.size > 1) "第 ${index + 1} 组 · ${group.size} 项" else "待处理项目",
                                color = MainText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (page == OrganizeSubPage.DUPLICATES) {
                                Text("内容完全相同", color = MutedText, fontSize = 10.sp)
                            }
                        }
                        group.forEach { asset ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = asset.contentUri,
                                    contentDescription = asset.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.width(76.dp).aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black.copy(alpha = 0.035f))
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        asset.displayName,
                                        color = MainText,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        if (asset.mediaType == 2) "视频" else "照片",
                                        color = MutedText,
                                        fontSize = 10.sp
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Text(
                                            "保留",
                                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                                .clickable { onKeep(asset) }
                                                .padding(vertical = 4.dp, horizontal = 2.dp),
                                            color = ThemeMint,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "加入待删除",
                                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                                .clickable { onPendingDelete(asset) }
                                                .padding(vertical = 4.dp, horizontal = 2.dp),
                                            color = MutedText,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
