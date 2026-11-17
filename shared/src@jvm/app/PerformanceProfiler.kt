package org.baconana.mybeautifulgit_chan.app

import java.util.Locale
import kotlin.math.roundToLong

/** Накопленная статистика одного именованного участка работы. */
data class PerformanceSpan(
    val name: String,
    val count: Long,
    val lastMs: Long,
    val averageMs: Long,
    val maxMs: Long,
)

/** Счётчик событий (cache hit/miss, число подгруженных страниц и т. п.). */
data class PerformanceCounter(
    val name: String,
    val value: Long,
)

/**
 * Лёгкий профилировщик для чтения репозитория.
 *
 * Он не запускает sampling и не влияет на Git: замеряет только участки,
 * которые уже выполняются, и хранит последние/средние/максимальные значения.
 * Этого достаточно, чтобы отличить медленный `git status` от медленного
 * построения графа или лишнего перечитывания истории.
 */
class PerformanceProfiler {
    private data class SpanAccumulator(
        var count: Long = 0,
        var lastMs: Long = 0,
        var totalMs: Long = 0,
        var maxMs: Long = 0,
    )

    private val lock = Any()
    private val spans = LinkedHashMap<String, SpanAccumulator>()
    private val counters = LinkedHashMap<String, Long>()

    fun <T> measure(name: String, block: () -> T): T {
        val started = System.nanoTime()
        try {
            return block()
        } finally {
            record(name, elapsedMs(started))
        }
    }

    suspend fun <T> measureSuspend(name: String, block: suspend () -> T): T {
        val started = System.nanoTime()
        try {
            return block()
        } finally {
            record(name, elapsedMs(started))
        }
    }

    fun record(name: String, durationMs: Long) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val duration = durationMs.coerceAtLeast(0)
        synchronized(lock) {
            val span = spans.getOrPut(clean) { SpanAccumulator() }
            span.count += 1
            span.lastMs = duration
            span.totalMs += duration
            if (duration > span.maxMs) span.maxMs = duration
        }
    }

    fun increment(name: String, delta: Long = 1) {
        val clean = name.trim()
        if (clean.isEmpty() || delta == 0L) return
        synchronized(lock) {
            counters[clean] = (counters[clean] ?: 0L) + delta
        }
    }

    fun counter(name: String): Long = synchronized(lock) { counters[name] ?: 0L }

    fun snapshot(): List<PerformanceSpan> = synchronized(lock) {
        spans.map { (name, span) ->
            PerformanceSpan(
                name = name,
                count = span.count,
                lastMs = span.lastMs,
                averageMs = if (span.count == 0L) 0 else {
                    (span.totalMs.toDouble() / span.count).roundToLong()
                },
                maxMs = span.maxMs,
            )
        }
    }

    fun counters(): List<PerformanceCounter> = synchronized(lock) {
        counters.map { (name, value) -> PerformanceCounter(name, value) }
    }

    fun reset() {
        synchronized(lock) {
            spans.clear()
            counters.clear()
        }
    }

    private fun elapsedMs(startedNanos: Long): Long =
        ((System.nanoTime() - startedNanos) / 1_000_000.0).roundToLong()
}

/** Снимок для экрана профиля: профайлер вместе с текущим размером окон и кэшей. */
data class PerformanceReport(
    val spans: List<PerformanceSpan>,
    val counters: List<PerformanceCounter>,
    val historyLoaded: Int,
    val historyHasMore: Boolean,
    val graphRows: Int,
    val graphLanes: Int,
    val inspectionCacheEntries: Int,
    val inspectionCacheBytes: Long,
)

internal fun formatProfileDuration(ms: Long): String = when {
    ms < 1_000 -> "$ms ms"
    else -> String.format(Locale.ROOT, "%.1f s", ms / 1000.0)
}

internal fun formatProfileBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.ROOT, "%.1f KiB", bytes / 1024.0)
    else -> String.format(Locale.ROOT, "%.1f MiB", bytes / (1024.0 * 1024.0))
}
