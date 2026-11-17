package org.baconana.mybeautifulgit_chan.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import org.baconana.mybeautifulgit_chan.domain.AppVersion
import org.baconana.mybeautifulgit_chan.domain.UpdateChannel
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object UpdateSource {
    const val REPOSITORY = "Baconana-chan/MyBeautifulGit-chan"
    const val RELEASES_PAGE = "https://github.com/$REPOSITORY/releases"
    const val RELEASES_API =
        "https://api.github.com/repos/$REPOSITORY/releases?per_page=30"
}

data class AppRelease(
    val tag: String,
    val version: AppVersion,
    val name: String,
    val pageUrl: String,
    val prerelease: Boolean,
    val notes: String,
) {
    val isStable: Boolean
        get() = !prerelease && !version.isPrerelease
}

enum class ReleaseCheckStatus {
    UpdateAvailable,
    UpToDate,
    NoReleases,
    RepositoryUnavailable,
    Failed,
}

data class ReleaseCheckResult(
    val status: ReleaseCheckStatus,
    val channel: UpdateChannel,
    val current: AppVersion,
    val release: AppRelease? = null,
    val failure: String? = null,
)

/**
 * Read-only GitHub Releases client. It deliberately does not download assets
 * or install anything: packages and artifact verification come later.
 */
object GitHubReleases {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client by lazy {
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }

    fun check(
        channel: UpdateChannel,
        current: AppVersion = AppVersion.current,
        apiUrl: String = UpdateSource.RELEASES_API,
    ): ReleaseCheckResult {
        val request = HttpRequest.newBuilder(URI.create(apiUrl))
            .timeout(Duration.ofSeconds(12))
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "MyBeautifulGit-chan/${AppVersion.CURRENT_TAG}")
            .GET()
            .build()
        return try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            interpretResponse(response.statusCode(), response.body(), channel, current)
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            failure(channel, current, "request interrupted")
        } catch (exception: Exception) {
            failure(channel, current, exception.message ?: exception::class.simpleName.orEmpty())
        }
    }

    fun interpretResponse(
        statusCode: Int,
        body: String,
        channel: UpdateChannel,
        current: AppVersion = AppVersion.current,
    ): ReleaseCheckResult {
        if (statusCode == 404) {
            return ReleaseCheckResult(
                status = ReleaseCheckStatus.RepositoryUnavailable,
                channel = channel,
                current = current,
            )
        }
        if (statusCode !in 200..299) {
            return failure(channel, current, "GitHub returned HTTP $statusCode")
        }
        val releases = runCatching { parseReleases(body) }.getOrElse { exception ->
            return failure(
                channel,
                current,
                exception.message ?: "invalid GitHub Releases response",
            )
        }
        return evaluate(releases, channel, current)
    }

    fun evaluate(
        releases: List<AppRelease>,
        channel: UpdateChannel,
        current: AppVersion = AppVersion.current,
    ): ReleaseCheckResult {
        val eligible = releases.filter { release ->
            channel.includesPrereleases || release.isStable
        }
        val latest = eligible.maxByOrNull { it.version }
            ?: return ReleaseCheckResult(
                status = ReleaseCheckStatus.NoReleases,
                channel = channel,
                current = current,
            )
        return if (latest.version > current) {
            ReleaseCheckResult(
                status = ReleaseCheckStatus.UpdateAvailable,
                channel = channel,
                current = current,
                release = latest,
            )
        } else {
            ReleaseCheckResult(
                status = ReleaseCheckStatus.UpToDate,
                channel = channel,
                current = current,
                release = latest,
            )
        }
    }

    fun parseReleases(body: String): List<AppRelease> {
        val array = json.parseToJsonElement(body) as? JsonArray
            ?: error("GitHub Releases response is not an array")
        return array.mapNotNull { element ->
            val release = element as? JsonObject ?: return@mapNotNull null
            if (release.boolean("draft") == true) return@mapNotNull null
            val tag = release.string("tag_name") ?: return@mapNotNull null
            val version = AppVersion.parse(tag) ?: return@mapNotNull null
            val pageUrl = release.string("html_url")
                ?.takeIf { it.startsWith("${UpdateSource.RELEASES_PAGE}/") }
                ?: UpdateSource.RELEASES_PAGE
            AppRelease(
                tag = tag,
                version = version,
                name = release.string("name") ?: tag,
                pageUrl = pageUrl,
                prerelease = release.boolean("prerelease") == true,
                notes = release.string("body").orEmpty().take(4_000),
            )
        }
    }

    private fun failure(
        channel: UpdateChannel,
        current: AppVersion,
        detail: String,
    ): ReleaseCheckResult = ReleaseCheckResult(
        status = ReleaseCheckStatus.Failed,
        channel = channel,
        current = current,
        failure = detail.take(300),
    )

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

    private fun JsonObject.boolean(key: String): Boolean? =
        (this[key] as? JsonPrimitive)?.booleanOrNull
}
