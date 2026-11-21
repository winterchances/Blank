package com.blank.gallery.ui.screens

import com.blank.gallery.ui.theme.GlassWhite
import com.blank.gallery.ui.theme.MainText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
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

@Composable
internal fun AlbumBatchActionBar(
    favoriteLabel: String,
    protectLabel: String,
    onFavorite: () -> Unit,
    onProtect: () -> Unit,
    onDelete: () -> Unit,
    deleteEnabled: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(GlassWhite)
            .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        BatchAction(favoriteLabel, onFavorite, Modifier.weight(1f))
        BatchAction(protectLabel, onProtect, Modifier.weight(1f))
        BatchAction("删除", onDelete, Modifier.weight(1f), enabled = deleteEnabled)
        BatchAction("取消", onCancel, Modifier.weight(1f))
    }
}

@Composable
private fun BatchAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Column(
        modifier = modifier
            .height(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, color = if (enabled) MainText else MainText.copy(alpha = 0.35f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
