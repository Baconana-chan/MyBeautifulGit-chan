package org.baconana.mybeautifulgit_chan.platform

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import kotlinx.coroutines.CancellationException
import java.io.File
import javax.swing.JFileChooser

object FileDialogs {
    /** Fallback для платформ/сессий, где нативный FileKit недоступен. */
    fun chooseDirectory(
        title: String,
        initial: String? = null,
        approveLabel: String = "Open",
    ): String? {
        val start = initial?.let(::File)?.takeIf { it.isDirectory }
            ?: File(System.getProperty("user.home"))
        val chooser = JFileChooser(start).apply {
            dialogTitle = title
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isAcceptAllFileFilterUsed = true
            approveButtonText = approveLabel
        }
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile?.absolutePath
        } else {
            null
        }
    }

    /**
     * Системный директорий-пикер: на Windows/macOS это нативный диалог,
     * на Linux — XDG portal с fallback внутри FileKit. Если FileKit не смог
     * инициализироваться, остаётся Swing-вариант, чтобы приложение не теряло
     * возможность открыть папку.
     */
    suspend fun chooseDirectorySystem(
        title: String,
        initial: String? = null,
        approveLabel: String = "Open",
    ): String? {
        val start = initial?.let(::File)?.takeIf { it.isDirectory }
        return try {
            FileKit.openDirectoryPicker(
                directory = start?.let(::PlatformFile),
                dialogSettings = FileKitDialogSettings(title = title),
            )?.file?.path
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            chooseDirectory(title, initial, approveLabel)
        }
    }
}
