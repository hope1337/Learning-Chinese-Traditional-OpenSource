package com.example.chinese2.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File

/**
 * Owns the user-selected app data root (SAF tree URI).
 *
 * Irreplaceable user data (flashcards, confirmed history, owned images)
 * lives under this root so it remains on disk after uninstall and can be
 * re-linked on reinstall via [adoptTreeUri]. Reference data (MOE dictionary
 * and stroke order) ships in the APK and is restored by reinstall instead.
 *
 * With no folder selected, [backendRoot] falls back to an internal directory
 * so screens keep working.
 */
class AppDataRoot(private val appContext: Context) {
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun treeUri(): Uri? =
        prefs.getString(KEY_TREE_URI, null)?.let { runCatching { Uri.parse(it) }.getOrNull() }

    fun hasExternalRoot(): Boolean {
        val uri = treeUri() ?: return false
        return hasPersistedAccess(uri) && canProbe(uri)
    }

    fun displayName(): String = if (hasExternalRoot()) "Selected folder" else "App internal storage"

    fun backendRoot(): File {
        // Minimal starter: file-backed root. SAF document access is wrapped
        // by UserDataRepository through contentResolver streams.
        return File(appContext.filesDir, "app-data-root").apply { mkdirs() }
    }

    fun adoptTreeUri(uri: Uri): Boolean = try {
        appContext.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        prefs.edit().putString(KEY_TREE_URI, uri.toString()).apply()
        true
    } catch (_: Exception) {
        false
    }

    fun clearSelection() {
        treeUri()?.let { uri ->
            runCatching {
                appContext.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        prefs.edit().remove(KEY_TREE_URI).apply()
    }

    private fun hasPersistedAccess(uri: Uri): Boolean =
        appContext.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission && it.isWritePermission
        }

    private fun canProbe(uri: Uri): Boolean = runCatching {
        val docId = DocumentsContract.getTreeDocumentId(uri)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(uri, docId)
        appContext.contentResolver.query(
            children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
            null, null, null,
        )?.close()
        true
    }.getOrDefault(false)

    companion object {
        const val PREFS = "appdata_root_prefs"
        const val KEY_TREE_URI = "tree_uri"
        val REQUIRED_DIRS = listOf(
            "reference", "user/cards", "user/cards/media",
            "scan-history/images", "media", "import", "export", "metadata",
        )
    }
}
