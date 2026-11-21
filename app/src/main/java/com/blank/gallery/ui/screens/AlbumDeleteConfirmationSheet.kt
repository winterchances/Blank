package com.blank.gallery.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blank.gallery.ui.theme.MainText
import com.blank.gallery.ui.theme.PageBackground

@Composable
internal fun AlbumDeleteConfirmationSheet(
    visible: Boolean,
    deletableCount: Int,
    protectedCount: Int,
    isAndroid11OrNewer: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300))
    ) {
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss
                )
        ) {
            Column(
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(max = screenHeight * 0.85f)
                    .shadow(
                        10.dp,
                        RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                        ambientColor = Color.Black.copy(alpha = 0.10f),
                        spotColor = Color.Black.copy(alpha = 0.10f)
                    )
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(PageBackground)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {}
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "移至回收站？",
                    color = MainText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    if (isAndroid11OrNewer) {
                        "将选中的 $deletableCount 项移入系统回收站，之后可在系统相册的回收站中恢复。"
                    } else {
                        "Android 10 不提供统一的系统回收站。继续后将逐项处理；需要授权的项目会显示系统确认，删除后可能无法恢复。"
                    },
                    color = MainText.copy(alpha = 0.72f),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Center
                )
                if (protectedCount > 0) {
                    Text(
                        "已自动排除受保护的 $protectedCount 项。",
                        color = MainText.copy(alpha = 0.58f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
                SheetAction("确认删除", danger = true, onClick = onConfirm)
                SheetAction("取消", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun SheetAction(
    label: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        Modifier.fillMaxWidth()
            .shadow(
                4.dp,
                RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.03f),
                spotColor = Color.Black.copy(alpha = 0.03f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (danger) Color(0xFFFF3B30) else MainText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
