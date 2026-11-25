package com.blank.gallery.ui.screens

import com.blank.gallery.ui.components.EmptyState
import com.blank.gallery.ui.components.FeatureTile
import com.blank.gallery.ui.theme.MainText
import com.blank.gallery.ui.theme.MutedText

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
    pendingDeleteCount: Int,
    onOpenPendingDelete: () -> Unit,
    onOpenCategory: (OrganizeSubPage) -> Unit,
    onOpenSelfMode: (OrganizeSubPage) -> Unit
) {
    val smartItems = listOf(
        Triple(OrganizeSubPage.DUPLICATES, "重复照片", "查找内容完全相同的照片"),
        Triple(OrganizeSubPage.SIMILAR, "相似照片", "找出画面相近的照片"),
        Triple(OrganizeSubPage.BURSTS, "连拍", "集中整理连续拍摄的照片"),
        Triple(OrganizeSubPage.SCREENSHOTS, "截图", "集中查看保存的截图"),
        Triple(OrganizeSubPage.LARGE_FILES, "大文件", "查找占用空间较大的照片")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 0.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 18.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onOpenPendingDelete),
            shape = RoundedCornerShape(18.dp),
            color = Color.White.copy(alpha = 0.58f),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "待删除",
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MainText
                )
                Text(
                    "${pendingDeleteCount} 张",
                    fontSize = 12.sp,
                    color = MutedText
                )
                Text(
                    "›",
                    modifier = Modifier.padding(start = 10.dp),
                    fontSize = 20.sp,
                    color = MutedText
                )
            }
        }
        Column {
            SectionHeading("智能整理")
            smartItems.chunked(2).forEachIndexed { index, rowItems ->
                if (index > 0) Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { (page, title, description) ->
                        FeatureTile(
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

        Column {
            SectionHeading("自己整理")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureTile(
                    title = "按顺序整理",
                    description = "按照照片时间顺序，逐张浏览",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSelfMode(OrganizeSubPage.SELF_ORDERED) }
                )
                FeatureTile(
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
internal fun OrganizeSubPageContent(page: OrganizeSubPage) {
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
