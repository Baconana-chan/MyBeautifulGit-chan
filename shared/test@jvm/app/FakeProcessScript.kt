package org.baconana.mybeautifulgit_chan.app

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

/**
 * Creates a git-like executable that prints a valid version line and then a
 * large amount of output. Windows uses a .cmd file; POSIX uses a small shell
 * script with an executable bit.
 */
internal fun writeFakeProcessScript(directory: Path, lines: Int = 500): Path {
    require(lines > 0) { "lines must be positive" }
    val windows = System.getProperty("os.name").orEmpty().lowercase().contains("win")
    val file = directory.resolve(if (windows) "fake-git.cmd" else "fake-git")
    val payload = "x".repeat(100)
    val script = if (windows) {
        buildString {
            append("@echo off\r\n")
            append("echo git version 2.99.0\r\n")
            repeat(lines) {
                append("echo ").append(payload).append("\r\n")
            }
        }
    } else {
        buildString {
            append("#!/bin/sh\n")
            append("echo 'git version 2.99.0'\n")
            append("i=0\n")
            append("while [ \"\$i\" -lt $lines ]; do\n")
            append("  echo '").append(payload).append("'\n")
            append("  i=\$((i+1))\n")
            append("done\n")
        }
    }
    Files.writeString(file, script)
    if (!windows) {
        runCatching {
            Files.setPosixFilePermissions(
                file,
                PosixFilePermissions.fromString("rwx------"),
            )
        }
    }
    return file
}
