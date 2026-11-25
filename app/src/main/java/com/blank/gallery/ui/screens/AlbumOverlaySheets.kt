package com.blank.gallery.ui.screens

import com.blank.gallery.data.MediaFolder
import com.blank.gallery.ui.theme.MainText
import com.blank.gallery.ui.theme.MutedText
import com.blank.gallery.ui.theme.PageBackground
import com.blank.gallery.ui.theme.GlassWhite
import com.blank.gallery.ui.theme.blankGlassSurface

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState

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
                "搜索照片",
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
                        if (query.isEmpty()) Text("输入照片名称", color = MutedText, fontSize = 14.sp)
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
    protectedFolderPaths: List<String>,
    onFolderSelect: (String?) -> Unit,
    onToggleFolderProtection: (String, Boolean) -> Unit,
    hazeState: HazeState
) {
    val drawerShape = RoundedCornerShape(topEnd = 30.dp, bottomEnd = 30.dp)
    ModalDrawerSheet(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .padding(vertical = 24.dp)
            .blankGlassSurface(hazeState, drawerShape, glassAlpha = 0.76f),
        drawerShape = drawerShape,
        drawerContainerColor = Color.Transparent,
        drawerTonalElevation = 0.dp
    ) {
        Column(
            Modifier.fillMaxHeight().padding(horizontal = 20.dp)
        ) {
            Text(
                "文件夹",
                modifier = Modifier.padding(top = 26.dp, bottom = 18.dp, start = 8.dp),
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = MainText
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item(key = "all-folders") {
                    FolderDrawerItem(
                        label = "全部文件夹",
                        selected = selectedFolderPath == null,
                        protectionLabel = "",
                        onClick = { onFolderSelect(null) },
                        onToggleProtected = null
                    )
                }
                items(folders, key = { it.relativePath }) { folder ->
                    val normalizedFolderPath = folder.relativePath.trimEnd('/') + "/"
                    val exactProtected = protectedFolderPaths.contains(normalizedFolderPath)
                    val protectedByParent = protectedFolderPaths.any { protectedPath ->
                        protectedPath != normalizedFolderPath && normalizedFolderPath.startsWith(protectedPath)
                    }
                    FolderDrawerItem(
                        label = folder.relativePath.trimEnd('/'),
                        selected = selectedFolderPath == folder.relativePath,
                        protectionLabel = when {
                            protectedByParent -> "上级保护"
                            exactProtected -> "已保护"
                            else -> "不整理"
                        },
                        onClick = { onFolderSelect(folder.relativePath) },
                        onToggleProtected = if (protectedByParent) null else {
                            { onToggleFolderProtection(folder.relativePath, !exactProtected) }
                        }
                    )
                }
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun FolderDrawerItem(
    label: String,
    selected: Boolean,
    protectionLabel: String,
    onClick: () -> Unit,
    onToggleProtected: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color.White.copy(alpha = 0.56f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = MainText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (protectionLabel.isNotEmpty()) {
            Text(
                text = protectionLabel,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .then(if (onToggleProtected != null) Modifier.clickable(onClick = onToggleProtected) else Modifier),
                fontSize = 11.sp,
                color = MutedText
            )
        }
        if (selected) {
            Text("✓", modifier = Modifier.padding(start = 8.dp), fontSize = 13.sp, color = MainText)
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
    onViewSortClick: () -> Unit,
    onViewSortAnchor: (Rect) -> Unit,
    hazeState: HazeState
) {
    AlbumHeaderAction(AlbumGridVector, "视图与排序", onViewSortClick, onViewSortAnchor, hazeState)
}

@Composable
private fun AlbumHeaderAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    onAnchorPosition: (Rect) -> Unit,
    hazeState: HazeState
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(36.dp)
            .shadow(
                2.dp,
                androidx.compose.foundation.shape.CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.03f),
                spotColor = Color.Black.copy(alpha = 0.03f)
            )
            .blankGlassSurface(hazeState, androidx.compose.foundation.shape.CircleShape)
            .onGloballyPositioned { onAnchorPosition(it.boundsInRoot()) }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
internal fun AlbumViewSortDropdown(
    visible: Boolean,
    anchor: Rect?,
    selectedGridColumns: Int,
    sortBySize: Boolean,
    hazeState: HazeState,
    onSelectGrid: (Int) -> Unit,
    onSelectSort: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    if (anchor == null) return
    val density = LocalDensity.current
    val menuWidth = 184.dp
    val menuX = with(density) { anchor.right.toDp() - menuWidth }
    val menuY = with(density) { anchor.bottom.toDp() + 8.dp }
    val menuShape = RoundedCornerShape(18.dp)

    Box(Modifier.fillMaxSize().zIndex(20f)) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(320)),
            exit = fadeOut(tween(durationMillis = 98, delayMillis = 182))
        ) {
            Box(
                Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.06f))
                    .clickable(onClick = onDismiss)
            )
        }

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.offset(x = menuX, y = menuY).width(menuWidth),
            enter = scaleIn(
                animationSpec = tween(320),
                transformOrigin = TransformOrigin(1f, 0f)
            ) + fadeIn(tween(320)),
            exit = scaleOut(
                animationSpec = tween(280),
                transformOrigin = TransformOrigin(1f, 0f)
            ) + fadeOut(tween(durationMillis = 98, delayMillis = 182))
        ) {
            Column(
                modifier = Modifier
                    .shadow(
                        elevation = 10.dp,
                        shape = menuShape,
                        ambientColor = Color.Black.copy(alpha = 0.045f),
                        spotColor = Color.Black.copy(alpha = 0.055f)
                    )
                    .blankGlassSurface(hazeState, menuShape, glassAlpha = 0.94f)
                    .clip(menuShape)
                    .padding(vertical = 6.dp)
            ) {
                DropdownOption("四宫格", selectedGridColumns == 4) { onSelectGrid(4) }
                DropdownOption("三宫格", selectedGridColumns == 3) { onSelectGrid(3) }
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                        .height(0.5.dp).background(Color.Black.copy(alpha = 0.08f))
                )
                DropdownOption("按时间排序", !sortBySize) { onSelectSort(false) }
                DropdownOption("按文件大小排序", sortBySize) { onSelectSort(true) }
            }
        }
    }
}

@Composable
private fun DropdownOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = MainText
        )
        if (selected) {
            Text("✓", fontSize = 14.sp, color = MainText)
        }
    }
}
