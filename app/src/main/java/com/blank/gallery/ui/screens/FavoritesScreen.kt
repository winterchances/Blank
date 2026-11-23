package com.blank.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.blank.gallery.data.MediaAsset
import com.blank.gallery.ui.theme.MainText
import com.blank.gallery.ui.theme.MutedText
import com.blank.gallery.ui.theme.ThemeMint

@Composable
internal fun FavoritesContent(
    favorites: List<MediaAsset>,
    protectedMedia: List<MediaAsset>,
    onToggleFavorite: (MediaAsset) -> Unit,
    onToggleProtected: (MediaAsset) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val assets = if (selectedTab == 0) favorites else protectedMedia

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            CollectionTab("我的收藏", selectedTab == 0, favorites.size) { selectedTab = 0 }
            CollectionTab("保护清单", selectedTab == 1, protectedMedia.size) { selectedTab = 1 }
        }

        if (assets.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    if (selectedTab == 0) "还没有收藏的照片" else "还没有受保护的照片",
                    color = MutedText,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                items(assets, key = { it.id }) { asset ->
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box {
                            AsyncImage(
                                model = asset.contentUri,
                                contentDescription = asset.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.035f))
                            )
                            Text(
                                if (selectedTab == 0) "♥" else "保护中",
                                modifier = Modifier.align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.88f))
                                    .clickable {
                                        if (selectedTab == 0) onToggleFavorite(asset)
                                        else onToggleProtected(asset)
                                    }
                                    .padding(horizontal = 7.dp, vertical = 4.dp),
                                color = ThemeMint,
                                fontSize = if (selectedTab == 0) 14.sp else 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            asset.displayName,
                            color = MainText,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionTab(
    title: String,
    selected: Boolean,
    count: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            "$title $count",
            color = if (selected) MainText else MutedText,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        Box(
            Modifier
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) ThemeMint else Color.Transparent)
                .width(24.dp)
                .height(2.dp)
        )
    }
}
