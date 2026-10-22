package io.github.gythialy.sporteditor.data

import io.github.gythialy.sporteditor.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class UpdateInfo(val version: String, val htmlUrl: String, val releaseNotes: String)

/**
 * Checks the latest GitHub release.
 *
 * The API is public and unauthenticated, so this is only used for an explicit user action.
 */
object UpdateChecker {
    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/gythialy/SportEditor/releases/latest"

    suspend fun checkLatest(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val json = fetchLatestRelease()
        val tag = json.optString("tag_name").trim()
        if (tag.isEmpty() || !VersionComparator.isNewer(tag, currentVersion)) {
            null
        } else {
            UpdateInfo(
                version = tag,
                htmlUrl = json.optString("html_url"),
                releaseNotes = json.optString("body")
            )
        }
    }

    private fun fetchLatestRelease(): JSONObject {
        val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "SportEditor/" + BuildConfig.VERSION_NAME)
        }
        return try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException("GitHub API HTTP " + code)
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private const val TIMEOUT_MS = 10_000
}
