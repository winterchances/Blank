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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
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
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)
        ) {
            Text(
                "搜索照片或视频",
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MainText
            )
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(15.dp),
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


private val AlbumFolderVector: ImageVector = ImageVector.Builder(
    name = "AlbumFolder",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = null,
        stroke = SolidColor(Color(0xFF333333)),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(20.59f, 13.41f)
        lineTo(13.42f, 20.58f)
        quadTo(12.25f, 21.75f, 11.01f, 20.58f)
        lineTo(2f, 12f)
        lineTo(2f, 2f)
        lineTo(12f, 2f)
        lineTo(20.59f, 10.59f)
        quadTo(21.76f, 11.76f, 20.59f, 13.41f)
        close()
        moveTo(7f, 7f)
        lineTo(7.01f, 7f)
    }
}.build()

private val AlbumSortVector: ImageVector = ImageVector.Builder(
    name = "AlbumSort",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = null,
        stroke = SolidColor(Color(0xFF333333)),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 20f)
        lineTo(12f, 4f)
        moveTo(6f, 10f)
        lineTo(12f, 4f)
        lineTo(18f, 10f)
    }
}.build()

private val AlbumGridVector: ImageVector = ImageVector.Builder(
    name = "AlbumGrid",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = null,
        stroke = SolidColor(Color(0xFF333333)),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(3f, 3f)
        lineTo(10f, 3f)
        lineTo(10f, 10f)
        lineTo(3f, 10f)
        close()
        moveTo(14f, 3f)
        lineTo(21f, 3f)
        lineTo(21f, 10f)
        lineTo(14f, 10f)
        close()
        moveTo(14f, 14f)
        lineTo(21f, 14f)
        lineTo(21f, 21f)
        lineTo(14f, 21f)
        close()
        moveTo(3f, 14f)
        lineTo(10f, 14f)
        lineTo(10f, 21f)
        lineTo(3f, 21f)
        close()
    }
}.build()

@Composable
internal fun AlbumHeaderActionButtons(
    onFolderClick: () -> Unit,
    onSortClick: () -> Unit,
    onGridClick: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AlbumHeaderAction(AlbumFolderVector, "文件夹", onFolderClick)
        AlbumHeaderAction(AlbumSortVector, "排序", onSortClick)
        AlbumHeaderAction(AlbumGridVector, "网格布局", onGridClick)
    }
}

@Composable
private fun AlbumHeaderAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(36.dp).clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Image(
                imageVector = icon,
                contentDescription = description,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlbumOptionsSheet(
    title: String,
    options: List<String>,
    onSelect: (String) -> Unit,
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)
        ) {
            Text(
                title,
                modifier = Modifier.padding(vertical = 16.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MainText
            )
            options.forEach { option ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onSelect(option) },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    Text(
                        option,
                        modifier = Modifier.padding(17.dp),
                        fontSize = 15.sp,
                        color = MainText
                    )
                }
            }
        }
    }
}
