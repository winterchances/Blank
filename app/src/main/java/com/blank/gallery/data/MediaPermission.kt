package com.blank.gallery.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object MediaPermission {
    fun requiredPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= 33 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun hasAnyMediaAccess(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= 34 ->
            has(context, Manifest.permission.READ_MEDIA_IMAGES) ||
                has(context, Manifest.permission.READ_MEDIA_VIDEO) ||
                has(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        Build.VERSION.SDK_INT >= 33 ->
            has(context, Manifest.permission.READ_MEDIA_IMAGES) ||
                has(context, Manifest.permission.READ_MEDIA_VIDEO)
        else -> has(context, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun hasFullImageAccess(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= 33 -> has(context, Manifest.permission.READ_MEDIA_IMAGES)
        else -> has(context, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun hasFullVideoAccess(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= 33 -> has(context, Manifest.permission.READ_MEDIA_VIDEO)
        else -> has(context, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun canReadImages(context: Context): Boolean =
        hasFullImageAccess(context) || hasSelectedMediaAccess(context)

    fun canReadVideos(context: Context): Boolean =
        hasFullVideoAccess(context) || hasSelectedMediaAccess(context)

    private fun hasSelectedMediaAccess(context: Context): Boolean =
        Build.VERSION.SDK_INT >= 34 &&
            has(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)

    private fun has(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
