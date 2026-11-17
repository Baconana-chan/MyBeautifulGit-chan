package org.baconana.mybeautifulgit_chan.platform

import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Проверка сети перед авто-установкой git, чтобы UI не висел на заведомо мёртвом соединении.
 *
 * Вызывать вне UI-потока: DNS-резолв не ограничен connect-timeout.
 */
object NetworkProbe {
    fun isAvailable(
        host: String = "git-scm.com",
        port: Int = 443,
        timeoutMs: Int = 1500,
    ): Boolean {
        require(host.isNotBlank()) { "host must not be blank" }
        require(port in 1..65535) { "port must be in 1..65535" }
        require(timeoutMs > 0) { "timeoutMs must be positive" }

        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (_: IOException) {
            false
        }
    }
}
