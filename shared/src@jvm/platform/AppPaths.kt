package org.baconana.mybeautifulgit_chan.platform

import java.nio.file.Path
import java.nio.file.Paths

object AppPaths {
    private const val APP_DIR = "MyBeautifulGit-chan"

    val configDir: Path by lazy {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val home = Paths.get(System.getProperty("user.home"))
        when {
            os.contains("win") -> {
                val appData = System.getenv("APPDATA")
                Paths.get(appData ?: home.resolve("AppData/Roaming").toString()).resolve(APP_DIR)
            }

            os.contains("mac") -> home.resolve("Library/Application Support").resolve(APP_DIR)

            else -> {
                val xdg = System.getenv("XDG_CONFIG_HOME")
                Paths.get(xdg ?: home.resolve(".config").toString()).resolve("mybeautifulgit-chan")
            }
        }
    }

    fun recentFile(): Path = configDir.resolve("recent.properties")

    /** Open repository tabs restored on the next start. */
    fun sessionFile(): Path = configDir.resolve("session.properties")

    fun settingsFile(): Path = configDir.resolve("settings.properties")

    /** Per-repository command history and favorites for the raw runner. */
    fun commandRunnerFile(): Path = configDir.resolve("command-runner.properties")

    /** Encrypted token payload; the key is kept in a separate owner-only file. */
    fun credentialsFile(): Path = configDir.resolve("credentials.bin")

    fun credentialKeyFile(): Path = configDir.resolve("credentials.key")

    /** Separate encrypted store for the opt-in AI commit-message API key. */
    fun aiCredentialsFile(): Path = configDir.resolve("ai-credentials.bin")

    fun aiCredentialKeyFile(): Path = configDir.resolve("ai-credentials.key")
}
