package org.baconana.mybeautifulgit_chan.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BoundedCacheTest {

    @Test
    fun `evicts least recently used entry`() {
        val cache = BoundedCache<String, Int>(maxEntries = 2)
        cache.put("a", 1)
        cache.put("b", 2)
        assertEquals(1, cache["a"])
        cache.put("c", 3)

        assertNull(cache["b"])
        assertEquals(1, cache["a"])
        assertEquals(3, cache["c"])
    }

    @Test
    fun `respects weight budget`() {
        val cache = BoundedCache<String, String>(
            maxEntries = 10,
            maxWeight = 10,
            weigher = { _, value -> value.length.toLong() },
        )
        cache.put("a", "12345")
        cache.put("b", "12345")
        cache.put("c", "12345")

        assertEquals(2, cache.size)
        assertEquals(10, cache.weight)
    }

    @Test
    fun `replacing a key updates its weight`() {
        val cache = BoundedCache<String, String>(
            maxEntries = 4,
            maxWeight = 100,
            weigher = { _, value -> value.length.toLong() },
        )
        cache.put("a", "long-value")
        cache.put("a", "x")

        assertEquals(1, cache.size)
        assertEquals(1, cache.weight)
        assertEquals("x", cache["a"])
    }
}
