package com.blank.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class OrganizeSubPage(val title: String) {
    DUPLICATES("重复照片"),
    SIMILAR("相似照片"),
    BURSTS("连拍"),
    SCREENSHOTS("截图"),
    LARGE_FILES("大文件"),
    SELF_ORDERED("按顺序整理"),
    SELF_RANDOM("随机整理")
}

@Composable
internal fun OrganizeContent(
    onOpenCategory: (OrganizeSubPage) -> Unit,
    onOpenSelfMode: (OrganizeSubPage) -> Unit
) {
    val smartItems = listOf(
        Triple(OrganizeSubPage.DUPLICATES, "重复照片", "查找内容完全相同的照片"),
        Triple(OrganizeSubPage.SIMILAR, "相似照片", "找出画面相近的照片"),
        Triple(OrganizeSubPage.BURSTS, "连拍", "集中整理连续拍摄的照片"),
        Triple(OrganizeSubPage.SCREENSHOTS, "截图", "集中查看保存的截图"),
        Triple(OrganizeSubPage.LARGE_FILES, "大文件", "查找占用空间较大的照片和视频")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "智能整理",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MainText
            )
            smartItems.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { (page, title, description) ->
                        OrganizeCategoryTile(
                            title = title,
                            description = description,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenCategory(page) }
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "自己整理",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MainText
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OrganizeCategoryTile(
                    title = "按顺序整理",
                    description = "按照照片时间顺序，逐张浏览",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSelfMode(OrganizeSubPage.SELF_ORDERED) }
                )
                OrganizeCategoryTile(
                    title = "随机整理",
                    description = "随机选择一张照片作为起点",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSelfMode(OrganizeSubPage.SELF_RANDOM) }
                )
            }
        }
    }
}


@Composable
private fun OrganizeSubPageContent(page: OrganizeSubPage) {
    val message = when (page) {
        OrganizeSubPage.SELF_ORDERED ->
            "扫描完成后，将按照照片时间顺序，从尚未整理且未受保护的照片开始"
        OrganizeSubPage.SELF_RANDOM ->
            "扫描完成后，从尚未整理且未受保护的照片中随机选择起点，之后也会继续随机抽取"
        else ->
            "尚未扫描，扫描完成后会显示待整理内容"
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        EmptyState(
            message = message,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun OrganizeCategoryTile(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.02f),
                spotColor = Color.Black.copy(alpha = 0.02f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(GlassWhite)
            .border(1.dp, Color.Black.copy(alpha = 0.045f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MainText
            )
            Text("›", fontSize = 20.sp, color = MutedText)
        }
        Text(
            text = description,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = MutedText
        )
    }
}


