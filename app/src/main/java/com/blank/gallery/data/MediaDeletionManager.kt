package com.blank.gallery.data

import android.app.RecoverableSecurityException
import android.app.PendingIntent
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Platform-specific, user-consented media removal. */
class MediaDeletionManager(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    fun createTrashRequest(contentUris: List<String>): PendingIntent {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        return MediaStore.createTrashRequest(resolver, contentUris.map(Uri::parse), true)
    }

    suspend fun deleteLegacyItem(contentUri: String): LegacyDeleteResult =
        withContext(Dispatchers.IO) {
            try {
                val count = resolver.delete(Uri.parse(contentUri), null, null)
                if (count > 0) LegacyDeleteResult.Deleted else LegacyDeleteResult.Missing
            } catch (error: SecurityException) {
                val recoverable = error as? RecoverableSecurityException
                    ?: return@withContext LegacyDeleteResult.Failed
                LegacyDeleteResult.RequiresConsent(
                    recoverable.userAction.actionIntent.intentSender
                )
            } catch (_: Exception) {
                LegacyDeleteResult.Failed
            }
        }

    sealed class LegacyDeleteResult {
        object Deleted : LegacyDeleteResult()
        object Missing : LegacyDeleteResult()
        object Failed : LegacyDeleteResult()
        data class RequiresConsent(val intentSender: IntentSender) : LegacyDeleteResult()
    }
}
