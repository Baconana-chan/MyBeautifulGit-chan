package org.baconana.mybeautifulgit_chan.app

import org.baconana.mybeautifulgit_chan.domain.AppVersion
import org.baconana.mybeautifulgit_chan.domain.UpdateChannel
import org.baconana.mybeautifulgit_chan.update.AppRelease
import org.baconana.mybeautifulgit_chan.update.GitHubReleases
import org.baconana.mybeautifulgit_chan.update.ReleaseCheckStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateModelTest {

    @Test
    fun `channels keep chan and kun wire names and accept legacy aliases`() {
        assertEquals("chan", UpdateChannel.Chan.wireName)
        assertEquals("kun", UpdateChannel.Kun.wireName)
        assertEquals(UpdateChannel.Chan, UpdateChannel.parse(" stable "))
        assertEquals(UpdateChannel.Kun, UpdateChannel.parse("BETA"))
        assertEquals(UpdateChannel.Chan, UpdateChannel.parse("chan"))
        assertEquals(UpdateChannel.Kun, UpdateChannel.parse("kun"))
        assertEquals(UpdateChannel.Chan, UpdateChannel.parse("unknown"))
        assertFalse(UpdateChannel.Chan.includesPrereleases)
        assertTrue(UpdateChannel.Kun.includesPrereleases)
    }

    @Test
    fun `versions parse tags and compare with semver precedence`() {
        val release = assertNotNull(AppVersion.parse("v1.2.3"))
        assertEquals(1, release.major)
        assertEquals(2, release.minor)
        assertEquals(3, release.patch)
        assertEquals("1.2.3", release.display)

        val prerelease = assertNotNull(AppVersion.parse("1.2.3-kun.2"))
        assertTrue(prerelease.isPrerelease)
        assertTrue(prerelease < release)
        assertTrue(release > prerelease)

        assertTrue(assertNotNull(AppVersion.parse("1.2.3-beta.2")) <
            assertNotNull(AppVersion.parse("1.2.3-beta.10")))
        assertNull(AppVersion.parse("1.2"))
        assertNull(AppVersion.parse("v1.2.3-"))
        assertEquals(AppVersion.CURRENT_TAG, "v0.1.0-dev")
        assertEquals(AppVersion.current.display, "0.1.0-dev")
    }

    @Test
    fun `stable channel ignores prereleases and picks the newest stable release`() {
        val releases = releases()
        val stable = GitHubReleases.evaluate(
            releases = releases,
            channel = UpdateChannel.Chan,
            current = AppVersion.parse("1.2.0")!!,
        )
        assertEquals(ReleaseCheckStatus.UpdateAvailable, stable.status)
        assertEquals("v1.3.0", stable.release?.tag)
    }

    @Test
    fun `beta channel includes stable and prerelease releases`() {
        val beta = GitHubReleases.evaluate(
            releases = releases(),
            channel = UpdateChannel.Kun,
            current = AppVersion.parse("1.3.0")!!,
        )
        assertEquals(ReleaseCheckStatus.UpdateAvailable, beta.status)
        assertEquals("v1.4.0-kun.1", beta.release?.tag)

        val upToDate = GitHubReleases.evaluate(
            releases = releases(),
            channel = UpdateChannel.Kun,
            current = AppVersion.parse("1.4.0")!!,
        )
        assertEquals(ReleaseCheckStatus.UpToDate, upToDate.status)
        assertEquals("v1.4.0-kun.1", upToDate.release?.tag)
    }

    @Test
    fun `no eligible releases and unpublished repository are reported separately`() {
        val noStable = GitHubReleases.evaluate(
            releases = releases().filter { it.prerelease },
            channel = UpdateChannel.Chan,
            current = AppVersion.parse("1.2.0")!!,
        )
        assertEquals(ReleaseCheckStatus.NoReleases, noStable.status)

        val none = GitHubReleases.evaluate(
            releases = emptyList(),
            channel = UpdateChannel.Chan,
            current = AppVersion.parse("1.2.0")!!,
        )
        assertEquals(ReleaseCheckStatus.NoReleases, none.status)

        val missing = GitHubReleases.interpretResponse(
            statusCode = 404,
            body = "",
            channel = UpdateChannel.Chan,
            current = AppVersion.parse("1.2.0")!!,
        )
        assertEquals(ReleaseCheckStatus.RepositoryUnavailable, missing.status)
    }

    @Test
    fun `GitHub JSON parsing skips drafts and releases with unknown tags`() {
        val parsed = GitHubReleases.parseReleases(
            """
            [
              {
                "tag_name": "v2.0.0",
                "name": "Draft",
                "prerelease": false,
                "draft": true,
                "html_url": "https://github.com/Baconana-chan/MyBeautifulGit-chan/releases/tag/v2.0.0"
              },
              {
                "tag_name": "nightly",
                "name": "Nightly",
                "prerelease": true,
                "draft": false
              },
              {
                "tag_name": "v1.2.3",
                "name": "Release",
                "prerelease": false,
                "draft": false,
                "html_url": "https://github.com/Baconana-chan/MyBeautifulGit-chan/releases/tag/v1.2.3",
                "body": "changes"
              }
            ]
            """.trimIndent(),
        )
        assertEquals(1, parsed.size)
        assertEquals("v1.2.3", parsed.single().tag)
        assertEquals("changes", parsed.single().notes)

        val invalid = GitHubReleases.interpretResponse(
            statusCode = 200,
            body = "{\"not\":\"an array\"}",
            channel = UpdateChannel.Chan,
            current = AppVersion.parse("1.2.0")!!,
        )
        assertEquals(ReleaseCheckStatus.Failed, invalid.status)
    }

    private fun releases(): List<AppRelease> = listOf(
        release("v1.3.0", "1.3.0", prerelease = false),
        release("v1.4.0-kun.1", "1.4.0-kun.1", prerelease = true),
        release("v1.2.9", "1.2.9", prerelease = false),
    )

    private fun release(tag: String, version: String, prerelease: Boolean): AppRelease =
        AppRelease(
            tag = tag,
            version = AppVersion.parse(version)!!,
            name = tag,
            pageUrl = "https://github.com/Baconana-chan/MyBeautifulGit-chan/releases/tag/$tag",
            prerelease = prerelease,
            notes = "",
        )
}
