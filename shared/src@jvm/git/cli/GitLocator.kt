package org.baconana.mybeautifulgit_chan.git.cli

import org.baconana.mybeautifulgit_chan.git.GitVersion
import org.baconana.mybeautifulgit_chan.git.displayCommandLine
import org.baconana.mybeautifulgit_chan.i18n.EnglishStrings
import org.baconana.mybeautifulgit_chan.i18n.Language
import org.baconana.mybeautifulgit_chan.i18n.Strings
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

enum class GitOs {
    WINDOWS,
    MACOS,
    LINUX,
    OTHER,
    ;

    companion object {
        fun current(): GitOs {
            val name = System.getProperty("os.name").orEmpty().lowercase()
            return when {
                name.contains("win") -> WINDOWS
                name.contains("mac") || name.contains("darwin") -> MACOS
                name.contains("linux") -> LINUX
                else -> OTHER
            }
        }
    }
}

enum class GitSource { MANUAL, PATH, WELL_KNOWN }

enum class GitStatus { READY, UNSUPPORTED_VERSION, NOT_FOUND }

/**
 * Возможности, которые приложение использует, и версия git, где они появились.
 * Минимум — 2.30, поэтому список нужен для честного «чего не будет» на старых сборках.
 */
enum class GitFeature(
    val since: GitVersion,
    val titleEn: String,
    val titleRu: String,
) {
    FORCE_IF_INCLUDES(
        GitVersion(2, 30, 0, "2.30.0"),
        "--force-if-includes (safer force-push)",
        "--force-if-includes (безопасный force-push)",
    ),
    FIXUP_AMEND(
        GitVersion(2, 32, 0, "2.32.0"),
        "commit --fixup=amend:/reword:",
        "commit --fixup=amend:/reword:",
    ),
    COMMIT_TRAILER(
        GitVersion(2, 32, 0, "2.32.0"),
        "commit --trailer (Signed-off-by and other trailers)",
        "commit --trailer (Signed-off-by и другие трейлеры)",
    ),
    STASH_STAGED(
        GitVersion(2, 35, 0, "2.35.0"),
        "stash push --staged (stash staged changes only)",
        "stash push --staged (stash только staged-изменений)",
    ),
    REMERGE_DIFF(
        GitVersion(2, 36, 0, "2.36.0"),
        "log/show --remerge-diff (inspect merge conflict resolution)",
        "log/show --remerge-diff (разбор разрешения merge-конфликтов)",
    ),
    CRUFT_PACKS(
        GitVersion(2, 37, 0, "2.37.0"),
        "gc --cruft (cruft packs for unreachable objects)",
        "gc --cruft (cruft-паки для недостижимых объектов)",
    ),
    REBASE_UPDATE_REFS(
        GitVersion(2, 38, 0, "2.38.0"),
        "rebase --update-refs (rebase dependent branches)",
        "rebase --update-refs (перенос зависимых веток)",
    ),
    MERGE_TREE_WRITE_TREE(
        GitVersion(2, 38, 0, "2.38.0"),
        "merge-tree --write-tree (merge preview without a worktree)",
        "merge-tree --write-tree (превью merge без worktree)",
    ),
    MERGE_TREE_MERGE_BASE(
        GitVersion(2, 40, 0, "2.40.0"),
        "merge-tree --merge-base (preview with an explicit base)",
        "merge-tree --merge-base (превью с явной базой)",
    ),
    ;

    fun title(language: Language): String =
        if (language == Language.RU) titleRu else titleEn
}

data class GitInstallation(
    val executable: String,
    val version: GitVersion,
    val source: GitSource,
)

data class GitDiscovery(
    val status: GitStatus,
    val installation: GitInstallation? = null,
    val searched: List<String> = emptyList(),
    val problem: String? = null,
) {
    val unavailableFeatures: List<GitFeature>
        get() = installation
            ?.version
            ?.let { version -> GitFeature.entries.filter { version < it.since } }
            .orEmpty()
}

enum class GitInstallKind { PACKAGE_MANAGER, DEVELOPER_TOOLS, PORTABLE, DOWNLOAD_PAGE }

/**
 * Один из способов получить git. Команды выполняются без `sudo`: повышение прав —
 * задача системного диалога, который показывает UI.
 */
data class GitInstallOption(
    val kind: GitInstallKind,
    val title: String,
    val detail: String,
    val command: List<String>? = null,
    val url: String? = null,
    val requiresElevation: Boolean = false,
    val requiresNetwork: Boolean = true,
) {
    val commandLine: String?
        get() = command?.let { displayCommandLine(it.first(), it.drop(1)) }
}

/**
 * Ищет системный git: PATH, затем стандартные места установки.
 *
 * Наличие файла ничего не доказывает (macOS `/usr/bin/git` — shim, который без Command Line
 * Tools открывает системный установщик), поэтому кандидат всегда проверяется реальным
 * запуском `git --version`.
 */
class GitLocator(
    private val os: GitOs = GitOs.current(),
    private val environment: Map<String, String> = System.getenv(),
    private val fileExists: (String) -> Boolean = { File(it).isFile },
    private val directoryExists: (String) -> Boolean = { File(it).isDirectory },
    private val probe: (String) -> String? = ::probeGitVersion,
    private val strings: Strings = EnglishStrings,
) {

    fun discover(manualExecutable: String? = null): GitDiscovery {
        val manual = manualExecutable?.trim()?.trim('"').orEmpty()
        if (manual.isNotEmpty()) {
            val discovery = probeCandidates(manualCandidates(manual).map { it to GitSource.MANUAL })
            return if (discovery.status == GitStatus.NOT_FOUND) {
                discovery.copy(problem = strings.setupManualProblem(manual))
            } else {
                discovery
            }
        }

        val candidates = buildList {
            pathDirectories().forEach { add(File(it, gitExecutableName).path to GitSource.PATH) }
            wellKnownCandidates().forEach { add(it to GitSource.WELL_KNOWN) }
        }
        return probeCandidates(candidates)
    }

    fun installOptions(): List<GitInstallOption> = when (os) {
        GitOs.WINDOWS -> buildList {
            findOnPath("winget.exe")?.let {
                add(
                    GitInstallOption(
                        kind = GitInstallKind.PACKAGE_MANAGER,
                        title = strings.installWingetTitle,
                        detail = strings.installWingetDetail,
                        command = listOf(
                            "winget",
                            "install",
                            "--id",
                            "Git.Git",
                            "-e",
                            "--source",
                            "winget",
                            "--accept-package-agreements",
                            "--accept-source-agreements",
                        ),
                        requiresElevation = true,
                    ),
                )
            }
            add(
                GitInstallOption(
                    kind = GitInstallKind.PORTABLE,
                    title = strings.installPortableTitle,
                    detail = strings.installPortableDetail,
                    url = DOWNLOAD_WINDOWS,
                ),
            )
            add(
                GitInstallOption(
                    kind = GitInstallKind.DOWNLOAD_PAGE,
                    title = strings.installDownloadTitle,
                    detail = strings.installWindowsDetail,
                    url = DOWNLOAD_WINDOWS,
                ),
            )
        }

        GitOs.MACOS -> buildList {
            add(
                GitInstallOption(
                    kind = GitInstallKind.DEVELOPER_TOOLS,
                    title = strings.installXcodeTitle,
                    detail = strings.installXcodeDetail,
                    command = listOf("xcode-select", "--install"),
                    requiresElevation = false,
                ),
            )
            findBrew()?.let { brew ->
                add(
                    GitInstallOption(
                        kind = GitInstallKind.PACKAGE_MANAGER,
                        title = strings.installBrewTitle,
                        detail = strings.installBrewDetail,
                        command = listOf(brew, "install", "git"),
                        requiresElevation = false,
                    ),
                )
            }
            add(
                GitInstallOption(
                    kind = GitInstallKind.DOWNLOAD_PAGE,
                    title = strings.installDownloadTitle,
                    detail = strings.installMacDetail,
                    url = DOWNLOAD_MACOS,
                ),
            )
        }

        GitOs.LINUX, GitOs.OTHER -> buildList {
            packageManager()?.let { manager ->
                add(
                    GitInstallOption(
                        kind = GitInstallKind.PACKAGE_MANAGER,
                        title = strings.installPackageManagerTitle(manager.name),
                        detail = strings.installPackageManagerDetail,
                        command = manager.command,
                        requiresElevation = true,
                    ),
                )
            }
            add(
                GitInstallOption(
                    kind = GitInstallKind.DOWNLOAD_PAGE,
                    title = strings.installDownloadTitle,
                    detail = strings.installLinuxDetail,
                    url = DOWNLOAD_LINUX,
                ),
            )
        }
    }

    /**
     * Способы обновить уже найденный git. В отличие от установки с нуля, Windows
     * использует `winget upgrade`, а не повторный `install`.
     */
    fun updateOptions(): List<GitInstallOption> = when (os) {
        GitOs.WINDOWS -> buildList {
            findOnPath("winget.exe")?.let {
                add(
                    GitInstallOption(
                        kind = GitInstallKind.PACKAGE_MANAGER,
                        title = strings.updateWingetTitle,
                        detail = strings.updateWingetDetail,
                        command = listOf(
                            "winget",
                            "upgrade",
                            "--id",
                            "Git.Git",
                            "-e",
                            "--source",
                            "winget",
                            "--accept-package-agreements",
                            "--accept-source-agreements",
                        ),
                        requiresElevation = true,
                    ),
                )
            }
            add(
                GitInstallOption(
                    kind = GitInstallKind.PORTABLE,
                    title = strings.installPortableTitle,
                    detail = strings.installPortableDetail,
                    url = DOWNLOAD_WINDOWS,
                ),
            )
            add(
                GitInstallOption(
                    kind = GitInstallKind.DOWNLOAD_PAGE,
                    title = strings.installDownloadTitle,
                    detail = strings.installWindowsDetail,
                    url = DOWNLOAD_WINDOWS,
                ),
            )
        }

        GitOs.MACOS -> buildList {
            findBrew()?.let { brew ->
                add(
                    GitInstallOption(
                        kind = GitInstallKind.PACKAGE_MANAGER,
                        title = strings.updateBrewTitle,
                        detail = strings.updateBrewDetail,
                        command = listOf(brew, "upgrade", "git"),
                        requiresElevation = false,
                    ),
                )
            }
            add(
                GitInstallOption(
                    kind = GitInstallKind.DOWNLOAD_PAGE,
                    title = strings.installDownloadTitle,
                    detail = strings.installMacDetail,
                    url = DOWNLOAD_MACOS,
                ),
            )
        }

        GitOs.LINUX, GitOs.OTHER -> buildList {
            packageManager()?.let { manager ->
                add(
                    GitInstallOption(
                        kind = GitInstallKind.PACKAGE_MANAGER,
                        title = strings.updatePackageManagerTitle(manager.name),
                        detail = strings.updatePackageManagerDetail,
                        command = manager.command,
                        requiresElevation = true,
                    ),
                )
            }
            add(
                GitInstallOption(
                    kind = GitInstallKind.DOWNLOAD_PAGE,
                    title = strings.installDownloadTitle,
                    detail = strings.installLinuxDetail,
                    url = DOWNLOAD_LINUX,
                ),
            )
        }
    }

    private fun probeCandidates(candidates: List<Pair<String, GitSource>>): GitDiscovery {
        val searched = LinkedHashSet<String>()
        val seen = HashSet<String>()
        val tooOld = mutableListOf<GitInstallation>()

        for ((candidate, source) in candidates) {
            val path = File(candidate).normalize().path
            searched += path
            if (!seen.add(dedupeKey(path))) continue
            if (!canProbe(path)) continue
            if (!fileExists(path)) continue

            val output = probe(path) ?: continue
            val version = GitVersion.parse(output) ?: continue
            val installation = GitInstallation(path, version, source)
            if (version.isSupported) {
                return GitDiscovery(GitStatus.READY, installation, searched.toList())
            }
            tooOld += installation
        }

        val newestOld = tooOld.maxByOrNull { it.version }
        if (newestOld != null) {
            val minimum = GitVersion.MINIMUM_SUPPORTED
            return GitDiscovery(
                status = GitStatus.UNSUPPORTED_VERSION,
                installation = newestOld,
                searched = searched.toList(),
                problem = strings.setupTooOldProblem(
                    foundMajor = newestOld.version.major,
                    foundMinor = newestOld.version.minor,
                    minimumMajor = minimum.major,
                    minimumMinor = minimum.minor,
                ),
            )
        }

        return GitDiscovery(
            status = GitStatus.NOT_FOUND,
            searched = searched.toList(),
            problem = strings.setupNotFoundProblem,
        )
    }

    private fun manualCandidates(raw: String): List<String> {
        if (fileExists(raw)) return listOf(raw)
        if (directoryExists(raw)) {
            val names = if (os == GitOs.WINDOWS) {
                listOf("git.exe", "cmd/git.exe", "bin/git.exe")
            } else {
                listOf("git")
            }
            return names.map { File(raw, it).path }
        }
        return listOf(raw)
    }

    private fun pathDirectories(): List<String> {
        val raw = environment.entries
            .firstOrNull { it.key.equals("PATH", ignoreCase = true) }
            ?.value
            ?: return emptyList()
        val separator = if (os == GitOs.WINDOWS) ';' else ':'
        return raw.split(separator)
            .mapNotNull { entry -> entry.trim().trim('"').takeIf { it.isNotEmpty() } }
    }

    private fun findOnPath(fileName: String): String? = pathDirectories()
        .asSequence()
        .map { File(it, fileName).path }
        .firstOrNull { fileExists(it) }

    private fun findBrew(): String? = listOf("/opt/homebrew/bin/brew", "/usr/local/bin/brew")
        .map(::systemPath)
        .firstOrNull { fileExists(it) }

    private fun packageManager(): PackageManager? = PACKAGE_MANAGERS.firstOrNull { manager ->
        manager.executables.any { fileExists(systemPath(it)) }
    }

    private fun wellKnownCandidates(): List<String> = when (os) {
        GitOs.WINDOWS -> buildList {
            environmentValue("ProgramFiles")?.let {
                add(File(it, "Git/cmd/git.exe").path)
                add(File(it, "Git/bin/git.exe").path)
            }
            environmentValue("ProgramFiles(x86)")?.let { add(File(it, "Git/cmd/git.exe").path) }
            environmentValue("LocalAppData")?.let { add(File(it, "Programs/Git/cmd/git.exe").path) }
            environmentValue("ProgramData")?.let { add(File(it, "chocolatey/bin/git.exe").path) }
            environmentValue("UserProfile")?.let { add(File(it, "scoop/shims/git.exe").path) }
        }

        GitOs.MACOS -> listOf(
            "/opt/homebrew/bin/git",
            "/usr/local/bin/git",
            "/usr/bin/git",
            "/Applications/Xcode.app/Contents/Developer/usr/bin/git",
        )

        GitOs.LINUX -> listOf(
            "/usr/bin/git",
            "/usr/local/bin/git",
            "/snap/bin/git",
        )

        GitOs.OTHER -> emptyList()
    }

    private fun environmentValue(name: String): String? = environment.entries
        .firstOrNull { it.key.equals(name, ignoreCase = true) }
        ?.value

    private fun canProbe(path: String): Boolean {
        if (os != GitOs.MACOS || path != systemPath("/usr/bin/git")) return true
        // Без CLT этот shim не «не работает», а показывает системный установщик.
        return appleDeveloperToolsInstalled()
    }

    private fun appleDeveloperToolsInstalled(): Boolean =
        fileExists(systemPath("/Library/Developer/CommandLineTools/usr/bin/git")) ||
            fileExists(systemPath("/Applications/Xcode.app/Contents/Developer/usr/bin/git"))

    private fun dedupeKey(path: String): String =
        if (os == GitOs.WINDOWS) path.lowercase() else path

    private fun systemPath(value: String): String = File(value).normalize().path

    private val gitExecutableName: String
        get() = if (os == GitOs.WINDOWS) "git.exe" else "git"

    private data class PackageManager(val name: String, val executables: List<String>, val command: List<String>)

    private companion object {
        const val DOWNLOAD_WINDOWS = "https://git-scm.com/download/win"
        const val DOWNLOAD_MACOS = "https://git-scm.com/download/mac"
        const val DOWNLOAD_LINUX = "https://git-scm.com/download/linux"

        val PACKAGE_MANAGERS = listOf(
            PackageManager("apt", listOf("/usr/bin/apt-get"), listOf("apt-get", "install", "-y", "git")),
            PackageManager("dnf", listOf("/usr/bin/dnf"), listOf("dnf", "install", "-y", "git")),
            PackageManager("zypper", listOf("/usr/bin/zypper"), listOf("zypper", "install", "-y", "git")),
            PackageManager("pacman", listOf("/usr/bin/pacman"), listOf("pacman", "-S", "--noconfirm", "git")),
            PackageManager("apk", listOf("/sbin/apk", "/usr/bin/apk"), listOf("apk", "add", "git")),
        )
    }
}

private fun probeGitVersion(executable: String): String? {
    val process = try {
        ProcessBuilder(executable, "--version")
            .redirectErrorStream(true)
            .start()
    } catch (_: IOException) {
        return null
    }

    runCatching { process.outputStream.close() }
    val output = BoundedOutputReader(
        input = process.inputStream,
        limit = MAX_VERSION_OUTPUT_BYTES,
        onLimit = { destroyProcessTree(process) },
    )
    val reader = thread(name = "git-locator-probe", isDaemon = true) {
        output.run()
    }

    val finished = runCatching { process.waitFor(10, TimeUnit.SECONDS) }.getOrDefault(false)
    if (!finished) {
        destroyProcessTree(process)
        reader.join(1_000)
        return null
    }

    reader.join(2_000)
    if (output.truncated || reader.isAlive || process.exitValue() != 0) return null
    return output.bytes().toString(Charsets.UTF_8)
}

private fun destroyProcessTree(process: Process) {
    runCatching {
        process.toHandle().descendants().forEach { it.destroy() }
    }
    process.destroy()
    val stopped = runCatching { process.waitFor(1, TimeUnit.SECONDS) }.getOrDefault(false)
    if (!stopped) {
        runCatching {
            process.toHandle().descendants().forEach { it.destroyForcibly() }
        }
        process.destroyForcibly()
    }
}

private const val MAX_VERSION_OUTPUT_BYTES = 16L * 1024
