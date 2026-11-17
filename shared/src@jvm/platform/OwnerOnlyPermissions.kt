package org.baconana.mybeautifulgit_chan.platform

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermissions

/** Best-effort owner-only permissions; Windows has no POSIX permission view. */
internal object OwnerOnlyPermissions {
    fun restrictFile(path: Path) {
        set(path, "rw-------")
    }

    fun restrictDirectory(path: Path) {
        set(path, "rwx------")
    }

    private fun set(path: Path, permissions: String) {
        runCatching {
            Files.getFileAttributeView(path, PosixFileAttributeView::class.java)
                ?.setPermissions(PosixFilePermissions.fromString(permissions))
        }
    }
}
