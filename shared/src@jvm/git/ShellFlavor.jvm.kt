package org.baconana.mybeautifulgit_chan.git

actual fun currentShellFlavor(): ShellFlavor =
    if (System.getProperty("os.name").orEmpty().contains("win", ignoreCase = true)) {
        // Windows Terminal defaults to PowerShell and its literal single-quoted
        // strings are safe for every argument we display. Callers that copy to
        // cmd.exe can request ShellFlavor.CMD explicitly.
        ShellFlavor.POWERSHELL
    } else {
        ShellFlavor.POSIX
    }
