package com.blank.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlin.math.abs
import kotlinx.coroutines.launch
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
                message = "请先允许留白访问照片，才能开始整理",
                modifier = Modifier.weight(1f)
            )
            ActionSurface("授权照片访问", onClick = onRequestAccess)
        } else if (isScanning) {
            EmptyState(
                message = "正在读取照片，请稍候",
                modifier = Modifier.weight(1f)
            )
        } else if (isLoading) {
            Box(Modifier.weight(1f).fillMaxWidth())
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
            val photoOffset = remember(asset.id) { Animatable(0f) }
            var dragOffset by remember(asset.id) { mutableFloatStateOf(0f) }
            var lastDragDirection by remember(asset.id) { mutableFloatStateOf(0f) }
            var isAnimating by remember(asset.id) { mutableStateOf(false) }
            val dragScope = rememberCoroutineScope()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { translationY = if (isAnimating) photoOffset.value else dragOffset }
                    .pointerInput(asset.id) {
                        val velocityTracker = VelocityTracker()
                        detectDragGestures(
                            onDragStart = {
                                if (!isAnimating) {
                                    velocityTracker.resetTracking()
                                    lastDragDirection = 0f
                                }
                            },
                            onDrag = { change, dragAmount ->
                                if (!isAnimating) {
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    dragOffset += dragAmount.y
                                    if (dragAmount.y != 0f) lastDragDirection = if (dragAmount.y < 0f) -1f else 1f
                                    change.consume()
                                }
                            },
                            onDragEnd = {
                                if (!isAnimating) {
                                    val velocityY = velocityTracker.calculateVelocity().y
                                    val distanceY = dragOffset
                                    val distanceThreshold = size.height * 0.22f
                                    val direction = when {
                                        abs(distanceY) >= distanceThreshold -> if (distanceY < 0f) -1f else 1f
                                        abs(velocityY) >= 1000f -> lastDragDirection
                                        else -> 0f
                                    }
                                    val containerHeight = size.height.toFloat()
                                    dragScope.launch {
                                        photoOffset.snapTo(distanceY)
                                        isAnimating = true
                                        if (direction != 0f) {
                                            photoOffset.animateTo(
                                                targetValue = direction * containerHeight * 1.2f,
                                                animationSpec = tween(
                                                    durationMillis = 110,
                                                    easing = FastOutLinearInEasing
                                                )
                                            )
                                            if (direction < 0f) onPendingDelete() else onKeep()
                                        } else {
                                            photoOffset.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f),
                                                initialVelocity = velocityY.coerceIn(-5000f, 5000f)
                                            )
                                            dragOffset = 0f
                                            isAnimating = false
                                        }
                                    }
                                }
                            },
                            onDragCancel = {
                                if (!isAnimating) {
                                    dragScope.launch {
                                        photoOffset.snapTo(dragOffset)
                                        isAnimating = true
                                        photoOffset.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f)
                                        )
                                        dragOffset = 0f
                                        isAnimating = false
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = asset.contentUri,
                    contentDescription = asset.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
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
