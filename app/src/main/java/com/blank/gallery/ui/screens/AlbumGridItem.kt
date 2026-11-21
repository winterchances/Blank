package com.blank.gallery.ui.screens

import com.blank.gallery.data.MediaAsset
import java.time.LocalDate

internal sealed interface AlbumGridItem {
    data class Media(val asset: MediaAsset) : AlbumGridItem
    data class DateHeader(val date: LocalDate, val label: String) : AlbumGridItem
}
