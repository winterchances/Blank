package com.blank.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
    assets: List<MediaAsset>,
    hasAnalyzed: Boolean,
    hasScanned: Boolean,
    isRunning: Boolean,
    progressText: String?,
    errorText: String?,
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    val description = when (page) {
        OrganizeSubPage.DUPLICATES -> "通过文件内容校验，找出完全相同的照片"
        OrganizeSubPage.SIMILAR -> "分析画面特征，整理内容相近的照片"
        OrganizeSubPage.BURSTS -> "结合拍摄时间与画面相似度识别连拍"
        OrganizeSubPage.SCREENSHOTS -> "根据文件名和所在文件夹识别截图"
        OrganizeSubPage.LARGE_FILES -> "找出占用空间较大的照片和视频"
        else -> ""
    }

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
                    if (isRunning) "正在分析" else if (hasAnalyzed) "找到 ${assets.size} 项" else "尚未分析",
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
            isRunning && assets.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("正在分析本机媒体，请稍候", color = MutedText, fontSize = 13.sp)
            }
            hasAnalyzed && assets.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("没有发现符合条件的内容", color = MutedText, fontSize = 13.sp)
            }
            assets.isNotEmpty() -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(assets, key = { it.id }) { asset ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AsyncImage(
                            model = asset.contentUri,
                            contentDescription = asset.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.035f))
                        )
                        Text(
                            asset.displayName,
                            color = MainText,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
