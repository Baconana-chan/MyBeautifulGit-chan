package org.baconana.mybeautifulgit_chan.git.cli

import org.baconana.mybeautifulgit_chan.app.writeFakeProcessScript
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitLocatorTest {

    @Test
    fun `finds real git in PATH`() {
        val discovery = GitLocator().discover()

        assertEquals(GitStatus.READY, discovery.status, discovery.problem)
        val installation = assertNotNull(discovery.installation)
        assertTrue(installation.version.isSupported)
        assertEquals(GitSource.PATH, installation.source)
        assertTrue(installation.executable.endsWith(gitFileName()))
    }

    @Test
    fun `huge fake git output is rejected within the probe bound`() {
        val dir = Files.createTempDirectory("git-chan-locator-huge")
        try {
            val fake = writeFakeProcessScript(dir, lines = 500)
            val locator = GitLocator(
                os = GitOs.current(),
                environment = emptyMap(),
                fileExists = { it == fake.toString() },
                directoryExists = { false },
            )

            val started = System.nanoTime()
            val discovery = locator.discover(fake.toString())
            val elapsedMs = (System.nanoTime() - started) / 1_000_000

            assertEquals(GitStatus.NOT_FOUND, discovery.status)
            assertTrue(elapsedMs < 8_000, "probe took $elapsedMs ms")
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `manual executable and containing directory are accepted`() {
        val executable = assertNotNull(executableOnPath())
        val locator = GitLocator()

        val byFile = locator.discover(executable)
        assertEquals(GitStatus.READY, byFile.status, byFile.problem)
        assertEquals(GitSource.MANUAL, byFile.installation?.source)

        val byDirectory = locator.discover(File(executable).parent)
        assertEquals(GitStatus.READY, byDirectory.status, byDirectory.problem)
    }

    @Test
    fun `missing manual path is reported as not found`() {
        val missing = File(System.getProperty("java.io.tmpdir"), "git-chan-missing-${System.nanoTime()}").path

        val discovery = GitLocator().discover(missing)

        assertEquals(GitStatus.NOT_FOUND, discovery.status)
        assertNull(discovery.installation)
        assertTrue(discovery.problem.orEmpty().isNotBlank())
    }

    @Test
    fun `old git is unsupported and lists missing features`() {
        val locator = GitLocator(
            os = GitOs.LINUX,
            environment = mapOf("PATH" to "/fake/bin"),
            fileExists = { it == normalized("/fake/bin/git") },
            directoryExists = { false },
            probe = { "git version 2.20.1" },
        )

        val discovery = locator.discover()

        assertEquals(GitStatus.UNSUPPORTED_VERSION, discovery.status)
        val installation = assertNotNull(discovery.installation)
        assertEquals(2, installation.version.major)
        assertEquals(20, installation.version.minor)
        assertFalse(installation.version.isSupported)
        assertTrue(discovery.unavailableFeatures.contains(GitFeature.REBASE_UPDATE_REFS))
        assertTrue(discovery.unavailableFeatures.contains(GitFeature.FORCE_IF_INCLUDES))
    }

    @Test
    fun `2_30 is ready but misses later features`() {
        val features = featuresFor("2.30.0")

        assertTrue(features.contains(GitFeature.REBASE_UPDATE_REFS))
        assertTrue(features.contains(GitFeature.MERGE_TREE_WRITE_TREE))
        assertFalse(features.contains(GitFeature.FORCE_IF_INCLUDES))
    }

    @Test
    fun `feature matrix follows git versions`() {
        assertEquals(
            listOf(
                GitFeature.FIXUP_AMEND,
                GitFeature.COMMIT_TRAILER,
                GitFeature.STASH_STAGED,
                GitFeature.REMERGE_DIFF,
                GitFeature.CRUFT_PACKS,
                GitFeature.REBASE_UPDATE_REFS,
                GitFeature.MERGE_TREE_WRITE_TREE,
                GitFeature.MERGE_TREE_MERGE_BASE,
            ),
            featuresFor("2.31.0"),
        )
        assertEquals(
            listOf(
                GitFeature.REMERGE_DIFF,
                GitFeature.CRUFT_PACKS,
                GitFeature.REBASE_UPDATE_REFS,
                GitFeature.MERGE_TREE_WRITE_TREE,
                GitFeature.MERGE_TREE_MERGE_BASE,
            ),
            featuresFor("2.35.0"),
        )
        assertEquals(
            listOf(
                GitFeature.REBASE_UPDATE_REFS,
                GitFeature.MERGE_TREE_WRITE_TREE,
                GitFeature.MERGE_TREE_MERGE_BASE,
            ),
            featuresFor("2.37.0"),
        )
        assertEquals(
            listOf(GitFeature.MERGE_TREE_MERGE_BASE),
            featuresFor("2.38.0"),
        )
        assertTrue(featuresFor("2.40.0").isEmpty())
    }

    @Test
    fun `newest unsupported git is reported`() {
        val locator = GitLocator(
            os = GitOs.LINUX,
            environment = mapOf("PATH" to "/new:/old"),
            fileExists = { it == normalized("/new/git") || it == normalized("/old/git") },
            probe = { path ->
                if (path.startsWith(normalized("/new"))) "git version 2.29.0" else "git version 2.10.0"
            },
        )

        val discovery = locator.discover()

        assertEquals(GitStatus.UNSUPPORTED_VERSION, discovery.status)
        assertEquals(29, discovery.installation?.version?.minor)
    }

    @Test
    fun `minimum supported version is ready`() {
        val locator = GitLocator(
            os = GitOs.LINUX,
            environment = mapOf("PATH" to "/fake"),
            fileExists = { it == normalized("/fake/git") },
            probe = { "git version 2.30.0" },
        )

        assertEquals(GitStatus.READY, locator.discover().status)
    }

    @Test
    fun `macOS shim is not executed without developer tools`() {
        var probed = false
        val locator = GitLocator(
            os = GitOs.MACOS,
            environment = mapOf("PATH" to "/usr/bin"),
            fileExists = { it == normalized("/usr/bin/git") },
            probe = {
                probed = true
                "git version 2.43.0"
            },
        )

        val discovery = locator.discover()

        assertEquals(GitStatus.NOT_FOUND, discovery.status)
        assertFalse(probed)
    }

    @Test
    fun `macOS shim is used when developer tools are installed`() {
        val locator = GitLocator(
            os = GitOs.MACOS,
            environment = mapOf("PATH" to "/usr/bin"),
            fileExists = {
                it == normalized("/usr/bin/git") ||
                    it == normalized("/Library/Developer/CommandLineTools/usr/bin/git")
            },
            probe = { "git version 2.43.0" },
        )

        val discovery = locator.discover()

        assertEquals(GitStatus.READY, discovery.status, discovery.problem)
        assertEquals(normalized("/usr/bin/git"), discovery.installation?.executable)
    }

    @Test
    fun `linux install options use detected package manager`() {
        val locator = GitLocator(
            os = GitOs.LINUX,
            environment = mapOf("PATH" to ""),
            fileExists = { it == normalized("/usr/bin/apt-get") },
        )

        val apt = assertNotNull(
            locator.installOptions().firstOrNull { it.kind == GitInstallKind.PACKAGE_MANAGER },
        )

        assertEquals(listOf("apt-get", "install", "-y", "git"), apt.command)
        assertEquals("apt-get install -y git", apt.commandLine)
        assertTrue(apt.requiresElevation)
    }

    @Test
    fun `windows update options use winget upgrade`() {
        val wingetDir = File("C:\\Windows\\System32\\WindowsApps").path
        val winget = File(wingetDir, "winget.exe").path
        val locator = GitLocator(
            os = GitOs.WINDOWS,
            environment = mapOf("PATH" to wingetDir),
            fileExists = { it == winget },
        )

        val option = assertNotNull(
            locator.updateOptions().firstOrNull { it.kind == GitInstallKind.PACKAGE_MANAGER },
        )

        assertEquals("winget", option.command?.firstOrNull())
        assertEquals("upgrade", option.command?.getOrNull(1))
        assertTrue(option.requiresElevation)
    }

    @Test
    fun `linux update options reuse the package manager install command`() {
        val locator = GitLocator(
            os = GitOs.LINUX,
            environment = mapOf("PATH" to ""),
            fileExists = { it == normalized("/usr/bin/apt-get") },
        )

        val option = assertNotNull(
            locator.updateOptions().firstOrNull { it.kind == GitInstallKind.PACKAGE_MANAGER },
        )

        assertEquals(listOf("apt-get", "install", "-y", "git"), option.command)
        assertTrue(option.requiresElevation)
    }

    @Test
    fun `windows install options include winget and portable git`() {
        val wingetDir = File("C:\\Windows\\System32\\WindowsApps").path
        val winget = File(wingetDir, "winget.exe").path
        val locator = GitLocator(
            os = GitOs.WINDOWS,
            environment = mapOf("PATH" to wingetDir, "ProgramFiles" to "C:\\Program Files"),
            fileExists = { it == winget },
        )

        val options = locator.installOptions()

        assertTrue(options.any { it.command?.firstOrNull() == "winget" })
        assertTrue(options.any { it.kind == GitInstallKind.PORTABLE })
        assertTrue(options.any { it.kind == GitInstallKind.DOWNLOAD_PAGE })
    }

    @Test
    fun `macOS install options include command line tools and brew`() {
        val locator = GitLocator(
            os = GitOs.MACOS,
            environment = mapOf("PATH" to ""),
            fileExists = { it == normalized("/opt/homebrew/bin/brew") },
        )

        val options = locator.installOptions()

        assertTrue(options.any { it.command == listOf("xcode-select", "--install") })
        assertTrue(
            locator.updateOptions().any {
                val command = it.command ?: return@any false
                command.dropLast(2) == listOf(normalized("/opt/homebrew/bin/brew")) &&
                    command.takeLast(2) == listOf("upgrade", "git")
            },
        )
        assertTrue(
            options.any {
                val command = it.command ?: return@any false
                command.dropLast(2) == listOf(normalized("/opt/homebrew/bin/brew")) &&
                    command.takeLast(2) == listOf("install", "git")
            },
        )
    }

    private fun featuresFor(version: String): List<GitFeature> {
        val locator = GitLocator(
            os = GitOs.LINUX,
            environment = mapOf("PATH" to "/fake"),
            fileExists = { it == normalized("/fake/git") },
            probe = { "git version $version" },
        )
        val discovery = locator.discover()
        assertEquals(GitStatus.READY, discovery.status, discovery.problem)
        return discovery.unavailableFeatures
    }

    private fun normalized(path: String): String = File(path).normalize().path

    private fun gitFileName(): String = if (isWindows()) "git.exe" else "git"

    private fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().lowercase().contains("win")

    private fun executableOnPath(): String? = System.getenv("PATH").orEmpty()
        .split(File.pathSeparatorChar)
        .mapNotNull { entry -> entry.trim().trim('"').takeIf { it.isNotEmpty() } }
        .map { File(it, gitFileName()) }
        .firstOrNull { it.isFile }
        ?.path
}
