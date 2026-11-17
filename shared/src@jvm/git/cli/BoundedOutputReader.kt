package org.baconana.mybeautifulgit_chan.git.cli

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Drains a process stream while retaining at most [limit] bytes.
 *
 * Once the limit is exceeded it keeps draining (so the child cannot block on a
 * full pipe) and invokes [onLimit] once. The caller uses that callback to kill
 * the process tree; the retained bytes and [truncated] flag are still available
 * after the reader thread joins.
 */
internal class BoundedOutputReader(
    private val input: InputStream,
    private val limit: Long,
    private val onLimit: () -> Unit = {},
    private val onChunk: ((ByteArray, Int) -> Unit)? = null,
) : Runnable {

    private val output = ByteArrayOutputStream()
    private var limitSignalled = false

    @Volatile
    var truncated: Boolean = false
        private set

    override fun run() {
        val effectiveLimit = limit.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        try {
            input.use { stream ->
                val buffer = ByteArray(8192)
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    onChunk?.let { callback ->
                        runCatching { callback(buffer, read) }
                    }
                    val remaining = effectiveLimit - output.size()
                    if (remaining <= 0) {
                        markTruncated()
                        continue
                    }
                    val copied = minOf(remaining, read)
                    output.write(buffer, 0, copied)
                    if (copied < read) markTruncated()
                }
            }
        } catch (_: IOException) {
            // The process was killed or closed its pipe.
        }
    }

    fun bytes(): ByteArray = output.toByteArray()

    private fun markTruncated() {
        truncated = true
        if (!limitSignalled) {
            limitSignalled = true
            runCatching(onLimit)
        }
    }
}
