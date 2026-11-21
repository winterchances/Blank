package com.blank.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.blank.gallery.ui.theme.GlassWhite
import com.blank.gallery.ui.theme.MainText
import com.blank.gallery.ui.theme.MutedText
import com.blank.gallery.ui.theme.ThemeMint

@Composable
internal fun SelfOrganizeScreen(
    asset: MediaAsset?,
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
                    "剩余 ${summary.remainingCount} 张",
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
                message = "请先允许留白访问照片，才能开始整理",
                modifier = Modifier.weight(1f)
            )
            ActionSurface("授权照片访问", onClick = onRequestAccess)
        } else if (isScanning || isLoading) {
            EmptyState(
                message = if (isScanning) "正在读取照片，请稍候" else "正在更新整理进度",
                modifier = Modifier.weight(1f)
            )
        } else if (asset == null) {
            EmptyState(
                message = if (summary.totalCount == 0) {
                    "暂时没有可整理的照片"
                } else {
                    "这些照片已经整理完了"
                },
                modifier = Modifier.weight(1f)
            )
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
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
                                    dragDistance < -swipeThreshold -> onKeep()
                                    dragDistance > swipeThreshold -> onPendingDelete()
                                }
                                dragDistance = 0f
                            },
                            onDragCancel = { dragDistance = 0f }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = asset,
                    transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                    },
                    label = "self-organize-photo"
                ) { currentAsset ->
                    AsyncImage(
                        model = currentAsset.contentUri,
                        contentDescription = currentAsset.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
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
                "上滑保留 · 下滑加入待删除",
                color = MutedText,
                fontSize = 11.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionSurface("保留", Modifier.weight(1f), onClick = onKeep)
                ActionSurface("待删除", Modifier.weight(1f), onClick = onPendingDelete)
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
                RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.02f),
                spotColor = Color.Black.copy(alpha = 0.02f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(GlassWhite)
            .border(1.dp, Color.Black.copy(alpha = 0.045f), RoundedCornerShape(18.dp))
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (enabled) MainText else MutedText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
