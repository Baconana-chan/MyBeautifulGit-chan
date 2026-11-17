package org.baconana.mybeautifulgit_chan.app

import java.util.LinkedHashMap

/**
 * Небольшой LRU-кэш с ограничением по числу записей и приблизительному весу.
 *
 * Используется для неизменяемых данных коммита (hash -> inspection): их можно
 * держать между переключениями выбора, но нельзя позволить кэшу бесконечно
 * удерживать многомегабайтные diff-ы.
 */
class BoundedCache<K, V>(
    private val maxEntries: Int,
    private val maxWeight: Long = Long.MAX_VALUE,
    private val weigher: (K, V) -> Long = { _, _ -> 1L },
) {
    init {
        require(maxEntries > 0) { "maxEntries must be positive" }
        require(maxWeight > 0) { "maxWeight must be positive" }
    }

    private val entries = object : LinkedHashMap<K, V>(16, 0.75f, true) {}
    private var currentWeight = 0L

    val size: Int
        @Synchronized get() = entries.size

    val weight: Long
        @Synchronized get() = currentWeight

    @Synchronized
    operator fun get(key: K): V? = entries[key]

    @Synchronized
    fun put(key: K, value: V) {
        val itemWeight = weigher(key, value).coerceAtLeast(0L)
        val previous = entries.put(key, value)
        if (previous != null) currentWeight -= weigher(key, previous)
        currentWeight += itemWeight

        val iterator = entries.entries.iterator()
        while ((entries.size > maxEntries || currentWeight > maxWeight) && iterator.hasNext()) {
            val evicted = iterator.next()
            iterator.remove()
            currentWeight -= weigher(evicted.key, evicted.value)
        }
        if (currentWeight < 0) currentWeight = 0
    }

    @Synchronized
    fun clear() {
        entries.clear()
        currentWeight = 0
    }
}
