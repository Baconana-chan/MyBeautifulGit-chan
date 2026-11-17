package org.baconana.mybeautifulgit_chan.app

import java.util.concurrent.TimeUnit

/**
 * Определяет системную тему без сторонних библиотек. Если ОС не ответила
 * или настройка недоступна, выбираем тёмную — это безопасный дефолт проекта.
 */
object PlatformTheme {

    fun isDark(): Boolean = runCatching {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        when {
            os.contains("win") -> windowsIsDark()
            os.contains("mac") -> macIsDark()
            else -> linuxIsDark()
        }
    }.getOrDefault(true)

    private fun windowsIsDark(): Boolean {
        val output = read(
            listOf(
                "reg",
                "query",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                "/v",
                "AppsUseLightTheme",
            ),
        ) ?: return true
        return when {
            output.contains("0x0", ignoreCase = true) -> true
            output.contains("0x1", ignoreCase = true) -> false
            else -> true
        }
    }

    private fun macIsDark(): Boolean {
        val output = read(listOf("defaults", "read", "-g", "AppleInterfaceStyle")) ?: return true
        return output.contains("Dark", ignoreCase = true)
    }

    private fun linuxIsDark(): Boolean {
        val scheme = read(
            listOf("gsettings", "get", "org.gnome.desktop.interface", "color-scheme"),
        )
        if (scheme != null) {
            return scheme.contains("dark", ignoreCase = true)
        }
        val theme = read(
            listOf("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme"),
        ) ?: return true
        return theme.contains("dark", ignoreCase = true)
    }

    private fun read(command: List<String>): String? {
        val process = runCatching {
            ProcessBuilder(command).redirectErrorStream(true).start()
        }.getOrNull() ?: return null
        return runCatching {
            if (!process.waitFor(600, TimeUnit.MILLISECONDS)) {
                process.destroy()
                return null
            }
            process.inputStream.bufferedReader().use { it.readText().trim() }
        }.getOrNull()
    }
}
