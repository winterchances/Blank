package com.blank.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.blank.gallery.data.MediaAsset
import com.blank.gallery.data.SelfOrganizeSummary
import com.blank.gallery.ui.components.EmptyState
import com.blank.gallery.ui.theme.MainText
import com.blank.gallery.ui.theme.MutedText
import com.blank.gallery.ui.theme.ThemeMint

@Composable
internal fun SelfOrganizeScreen(
    asset: MediaAsset?,
    nextAsset: MediaAsset? = null,
    previewAssets: List<MediaAsset> = emptyList(),
    summary: SelfOrganizeSummary,
    canUndo: Boolean,
    isLoading: Boolean,
    hasPhotoAccess: Boolean,
    isScanning: Boolean,
    feedbackMessage: String? = null,
    onRequestAccess: () -> Unit,
    onKeep: () -> Unit,
    onPendingDelete: () -> Unit,
    onSkip: () -> Unit,
    onUndo: () -> Unit,
    onOpenPendingDelete: () -> Unit
) {
    var swipeDirection by remember { mutableStateOf(1) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "剩余 ${summary.remainingCount} 项",
                    color = MainText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "保留 ${summary.keptCount} ·",
                        color = MutedText,
                        fontSize = 12.sp
                    )
                    Text(
                        " 待删除 ${summary.pendingDeleteCount}",
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenPendingDelete)
                            .padding(vertical = 2.dp),
                        color = ThemeMint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        " · 跳过 ${summary.skippedCount}",
                        color = MutedText,
                        fontSize = 12.sp
                    )
                }
            }
            Text(
                "撤销",
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(
                        enabled = canUndo && !isLoading,
                        onClick = onUndo
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = if (canUndo && !isLoading) ThemeMint else MutedText.copy(alpha = 0.45f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        feedbackMessage?.let {
            Text(it, color = MutedText, fontSize = 12.sp)
        }

        if (!hasPhotoAccess) {
            EmptyState(
                message = "请先允许留白访问照片和视频，才能开始整理",
                modifier = Modifier.weight(1f)
            )
            ActionSurface("授权照片和视频访问", onClick = onRequestAccess)
        } else if (isScanning || isLoading) {
            EmptyState(
                message = if (isScanning) "正在读取照片，请稍候" else "正在更新整理进度",
                modifier = Modifier.weight(1f)
            )
        } else if (asset == null) {
            EmptyState(
                message = if (summary.totalCount == 0) {
                    "暂时没有可整理的媒体"
                } else {
                    "这些媒体已经整理完了"
                },
                modifier = Modifier.weight(1f)
            )
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.035f))
                    .pointerInput(asset.id) {
                        var dragDistance = 0f
                        val swipeThreshold = 80.dp.toPx()
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, amount ->
                                dragDistance += amount
                                change.consume()
                            },
                            onDragEnd = {
                                when {
                                    dragDistance < -swipeThreshold -> { swipeDirection = 1; onPendingDelete() }
                                    dragDistance > swipeThreshold -> { swipeDirection = -1; onKeep() }
                                }
                                dragDistance = 0f
                            },
                            onDragCancel = { dragDistance = 0f }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                previewAssets.take(3).asReversed().forEachIndexed { stackIndex, preview ->
                    val rotations = listOf(-3.2f, 2.4f, -1.3f)
                    val horizontalOffsets = listOf((-13).dp, 11.dp, (-4).dp)
                    val verticalOffsets = listOf((-5).dp, 8.dp, 20.dp)
                    val scales = listOf(0.94f, 0.97f, 0.99f)
                    val stackModifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .graphicsLayer {
                            rotationZ = rotations[stackIndex]
                            translationX = horizontalOffsets[stackIndex].toPx()
                            translationY = verticalOffsets[stackIndex].toPx()
                            scaleX = scales[stackIndex]
                            scaleY = scales[stackIndex]
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                    AnimatedContent(
                        targetState = preview,
                        modifier = stackModifier,
                        transitionSpec = {
                            (slideInVertically(tween(260)) { it / 5 } + fadeIn(tween(220))) togetherWith
                                (slideOutVertically(tween(220)) { -it / 5 } + fadeOut(tween(180)))
                        },
                        label = "self-organize-stack-$stackIndex"
                    ) { shownPreview ->
                        if (shownPreview.mediaType == 2) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("视频", color = MutedText, fontSize = 12.sp)
                            }
                        } else {
                            AsyncImage(
                                model = shownPreview.contentUri,
                                contentDescription = shownPreview.displayName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                AnimatedContent(
                    targetState = asset,
                    transitionSpec = {
                        (slideInVertically(
                            animationSpec = tween(320, easing = FastOutSlowInEasing)
                        ) { if (swipeDirection > 0) it else -it } + fadeIn(tween(220))) togetherWith
                            (slideOutVertically(
                                animationSpec = tween(280, easing = FastOutSlowInEasing)
                            ) { if (swipeDirection > 0) -it else it } + fadeOut(tween(180)))
                    },
                    label = "self-organize-photo"
                ) { currentAsset ->
                    if (currentAsset.mediaType == 2) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(bottom = 12.dp)
                                .background(Color.Black.copy(alpha = 0.035f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("视频", color = MutedText, fontSize = 14.sp)
                        }
                    } else {
                        AsyncImage(
                            model = currentAsset.contentUri,
                            contentDescription = currentAsset.displayName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(bottom = 12.dp)
                        )
                    }
                }
                if (asset.mediaType == 2) {
                    Text(
                        "视频",
                        modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
                            .clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
            Text(
                asset.displayName,
                color = MainText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                "上滑删除 · 下滑保留",
                color = MutedText,
                fontSize = 11.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionSurface("保留", Modifier.weight(1f), onClick = { swipeDirection = -1; onKeep() })
                ActionSurface("待删除", Modifier.weight(1f), onClick = { swipeDirection = 1; onPendingDelete() })
                ActionSurface("跳过", Modifier.weight(1f), onClick = onSkip)
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ActionSurface(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                4.dp,
                RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.03f),
                spotColor = Color.Black.copy(alpha = 0.03f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(17.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (enabled) MainText else MutedText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
