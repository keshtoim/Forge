package com.keshtoim.forge.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
private data class GithubRelease(
    @SerialName("tag_name") val tag: String,
    val body: String? = null,
    @SerialName("html_url") val page: String,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
private data class GithubAsset(
    val name: String,
    @SerialName("browser_download_url") val url: String,
    val size: Long,
    // "sha256:<hex>", published by GitHub for every uploaded asset.
    val digest: String? = null,
)

data class AppUpdate(val version: String, val notes: String, val apkUrl: String, val size: Long, val sha256: String?, val page: String)

class UpdateCheckException(message: String, cause: Throwable? = null) : Exception(message, cause)

// Releases are published by CI from vX.Y.Z tags, each with the signed APK attached.
class UpdateChecker(private val baseUrl: String = "https://api.github.com", private val repository: String = "keshtoim/Forge") {
    private val json = Json { ignoreUnknownKeys = true }

    // The newest published release when it is newer than [current] and carries an APK; null when the app is up to date.
    suspend fun check(current: String): AppUpdate? = withContext(Dispatchers.IO) {
        val connection = URL("$baseUrl/repos/$repository/releases/latest").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Forge")
            when (val code = connection.responseCode) {
                // No release has been published yet.
                HttpURLConnection.HTTP_NOT_FOUND -> return@withContext null
                HttpURLConnection.HTTP_OK -> Unit
                else -> throw UpdateCheckException("GitHub answered $code")
            }
            val release = json.decodeFromString(GithubRelease.serializer(), connection.inputStream.bufferedReader().use { it.readText() })
            val version = release.tag.removePrefix("v")
            val apk = release.assets.firstOrNull { it.name.endsWith(".apk") }
            if (release.draft || release.prerelease || apk == null || !isNewer(version, current)) return@withContext null
            AppUpdate(
                version = version,
                notes = release.body.orEmpty().trim(),
                apkUrl = apk.url,
                size = apk.size,
                sha256 = apk.digest?.takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:"),
                page = release.page,
            )
        } catch (e: UpdateCheckException) {
            throw e
        } catch (e: Exception) {
            throw UpdateCheckException(e.message ?: e.javaClass.simpleName, e)
        } finally {
            connection.disconnect()
        }
    }
}

// Compares X.Y.Z numerically, so 0.10.0 is newer than 0.9.3; anything after a dash ("-beta") is ignored.
fun isNewer(candidate: String, current: String): Boolean {
    fun parts(version: String) = version.removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
    val a = parts(candidate)
    val b = parts(current)
    for (i in 0 until maxOf(a.size, b.size)) {
        val diff = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
        if (diff != 0) return diff > 0
    }
    return false
}
