package com.blank.gallery.ui.screens

import com.blank.gallery.data.MediaFolder
import com.blank.gallery.ui.theme.MainText
import com.blank.gallery.ui.theme.MutedText
import com.blank.gallery.ui.theme.PageBackground
import com.blank.gallery.ui.theme.GlassWhite

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun AlbumSearchSheet(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = PageBackground,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        scrimColor = Color.Black.copy(alpha = 0.4f)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("搜索照片或视频", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MainText)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(horizontal = 15.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = MainText, fontSize = 14.sp),
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text("输入照片或视频名称", color = MutedText, fontSize = 14.sp)
                        inner()
                    }
                )
                if (query.isNotEmpty()) {
                    Text("清除", modifier = Modifier.clickable { onQueryChange("") }, color = MutedText, fontSize = 12.sp)
                }
            }
            Text(
                if (query.isBlank()) "输入关键词即可筛选相册内容" else "搜索条件已应用，关闭后查看筛选结果",
                fontSize = 12.sp,
                color = MutedText
            )
        }
    }
}

@Composable
internal fun AlbumFolderDrawer(
    folders: List<MediaFolder>,
    selectedFolderPath: String?,
    onFolderSelect: (String?) -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp).fillMaxHeight(),
        drawerShape = RoundedCornerShape(topEnd = 30.dp, bottomEnd = 30.dp),
        drawerContainerColor = PageBackground,
        drawerTonalElevation = 0.dp
    ) {
        Column(Modifier.fillMaxHeight().padding(horizontal = 20.dp)) {
            Text("文件夹", modifier = Modifier.padding(top = 34.dp, bottom = 18.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MainText)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item(key = "all-folders") {
                    FolderDrawerItem("全部文件夹", selectedFolderPath == null) { onFolderSelect(null) }
                }
                items(folders, key = { it.relativePath }) { folder ->
                    FolderDrawerItem(folder.relativePath.trimEnd('/'), selectedFolderPath == folder.relativePath) {
                        onFolderSelect(folder.relativePath)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FolderDrawerItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) GlassWhite else Color.Transparent,
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = MainText,
                maxLines = 2
            )
            if (selected) Text("✓", fontSize = 14.sp, color = MainText)
        }
    }
}
