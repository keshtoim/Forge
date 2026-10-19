package com.keshtoim.forge.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val update: AppUpdate) : UpdateState

    // [progress] is 0..1, or null while the size is not known yet.
    data class Downloading(val update: AppUpdate, val progress: Float?) : UpdateState
    data class Ready(val update: AppUpdate) : UpdateState
    data class Failed(val reason: UpdateFailure, val update: AppUpdate?) : UpdateState
}

enum class UpdateFailure { CHECK, DOWNLOAD, CORRUPTED, SIGNATURE }

// Finds, downloads and hands a new release to the system installer, which updates the app in place.
class Updater(
    private val context: Context,
    private val scope: CoroutineScope,
    private val checker: UpdateChecker = UpdateChecker(),
) {
    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)
    private val downloads = context.getSystemService(DownloadManager::class.java)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state

    val currentVersion: String = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

    private val _autoCheckEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO, true))
    val autoCheckEnabled: StateFlow<Boolean> = _autoCheckEnabled

    private var work: Job? = null

    fun setAutoCheck(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO, enabled) }
        _autoCheckEnabled.value = enabled
    }

    // Called on every start; reaches GitHub at most once a day and only when the user left automatic checks on.
    fun checkIfDue(now: Long = System.currentTimeMillis()) {
        if (!_autoCheckEnabled.value || now - prefs.getLong(KEY_LAST_CHECK, 0) < DAY_MS) return
        check(quiet = true)
    }

    // A quiet check stays silent about failures and "up to date": nobody asked for it.
    fun check(quiet: Boolean = false) {
        if (work?.isActive == true) return
        work = scope.launch {
            if (!quiet) _state.value = UpdateState.Checking
            _state.value = try {
                val update = checker.check(currentVersion)
                prefs.edit { putLong(KEY_LAST_CHECK, System.currentTimeMillis()) }
                when {
                    update != null -> UpdateState.Available(update)
                    quiet -> UpdateState.Idle
                    else -> UpdateState.UpToDate
                }
            } catch (_: UpdateCheckException) {
                if (quiet) UpdateState.Idle else UpdateState.Failed(UpdateFailure.CHECK, null)
            }
        }
    }

    fun download(update: AppUpdate) {
        if (work?.isActive == true) return
        work = scope.launch {
            val file = apkFile(update)
            file.delete()
            val request = DownloadManager.Request(update.apkUrl.toUri())
                .setTitle("Forge ${update.version}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(file))
            val id = downloads.enqueue(request)
            _state.value = UpdateState.Downloading(update, null)
            _state.value = awaitDownload(id, update) ?: verified(update, file)
        }
    }

    private suspend fun awaitDownload(id: Long, update: AppUpdate): UpdateState? {
        while (true) {
            val (status, done, total) = downloads.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
                if (!cursor.moveToFirst()) return UpdateState.Failed(UpdateFailure.DOWNLOAD, update)
                Triple(
                    cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                )
            }
            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> return null
                DownloadManager.STATUS_FAILED -> return UpdateState.Failed(UpdateFailure.DOWNLOAD, update)
                else -> _state.value = UpdateState.Downloading(update, if (total > 0) done.toFloat() / total else null)
            }
            delay(400)
        }
    }

    // A file that differs from the published one is never handed to the installer.
    private suspend fun verified(update: AppUpdate, file: File): UpdateState = withContext(Dispatchers.IO) {
        when {
            update.sha256 != null && !sha256(file).equals(update.sha256, ignoreCase = true) -> UpdateState.Failed(UpdateFailure.CORRUPTED, update)
            !sameSigner(file) -> UpdateState.Failed(UpdateFailure.SIGNATURE, update)
            else -> UpdateState.Ready(update)
        }
    }

    // Android installs over the current app only from an APK signed with the same key; a debug build from CI is not.
    private fun sameSigner(file: File): Boolean {
        val pm = context.packageManager
        val archive = pm.getPackageArchiveInfo(file.path, signatureFlags()) ?: return false
        val installed = pm.getPackageInfo(context.packageName, signatureFlags())
        return archive.signers() == installed.signers()
    }

    // Android 8+ asks the user once to let Forge install apps; until then the installer can't be opened.
    fun canInstall() = context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun installIntent(update: AppUpdate): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apkFile(update))
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun apkFile(update: AppUpdate) =
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "forge-${update.version}.apk")

    private companion object {
        const val KEY_AUTO = "auto_check"
        const val KEY_LAST_CHECK = "last_check"
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}

fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

@Suppress("DEPRECATION")
private fun signatureFlags() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES

@Suppress("DEPRECATION")
private fun PackageInfo.signers(): Set<String> {
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) signingInfo?.apkContentsSigners else signatures
    return signatures.orEmpty().map { it.toCharsString() }.toSet()
}
