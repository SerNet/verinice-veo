package org.veo.adapter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arb
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.function.Supplier

class CacheProp : BehaviorSpec({
    given("a cache") {
        `when`("the key is missing") {
            then("the suppliers value should be returned") {
                checkAll(cacheArb(Arb.string(minSize = 1), Arb.string(minSize = 1)), Arb.string(), Arb.string()) { cache, key, value ->
                    // We assume that some arbitrary keys are not in the cache.
                    if (!cache.containsKey(key)) {
                        cache.get(key).orElseCreate { value } shouldBe value
                    }
                }
            }
        }
        `when`("the supplier was used") {
            then("the cached value is used on second call") {
                checkAll(cacheArb(Arb.string(minSize = 1), Arb.string(minSize = 1)), Arb.string(), Arb.string()) { cache, key, value ->
                    // We assume that some arbitrary keys are not in the cache.
                    if (!cache.containsKey(key)) {
                        val mockSupplier = mockk<Supplier<String>> {
                            every { get() } returns value
                        }
                        cache.get(key).orElseCreate(mockSupplier) shouldBe value
                        cache.get(key).orElseCreate(mockSupplier) shouldBe value
                        verify(exactly = 1) { mockSupplier.get() }
                    }
                }
            }
        }
        `when`("a value is put") {
            then("the value can be retrieved") {
                checkAll(cacheArb(Arb.string(minSize = 1), Arb.string(minSize = 1)), Arb.string(), Arb.string()) { cache, key, value ->
                    cache.put(key, value)
                    cache.get(key).orElseCreate(null) shouldBe value
                }
            }
            then("the supplier is not called") {
                checkAll(cacheArb(Arb.string(minSize = 1), Arb.string(minSize = 1)), Arb.string(), Arb.string()) { cache, key, cacheValue ->
                    val mockSupplier = mockk<Supplier<String>> {
                        every { get() } returns cacheValue
                    }
                    cache.put(key, cacheValue)
                    cache.get(key).orElseCreate(mockSupplier)
                    verify(exactly = 0) { mockSupplier.get() }
                }
            }
        }
    }
    given("an empty cache") {
        `when`("looking for an element") {
            then("the supplier is always used") {
                checkAll(Arb.string(), Arb.string()) { key, value ->
                    val mockSupplier = mockk<Supplier<String>> {
                        every { get() } returns value
                    }
                    Cache<String, String>().get(key).orElseCreate(mockSupplier)
                    verify(exactly = 1) { mockSupplier.get() }
                }
            }
        }
    }
})

fun <K, V> cacheArb(
    keyArb: Arb<K>,
    valueArb: Arb<V>,
    minSize: Int = 1,
    maxSize: Int = 100
): Arb<Cache<K, V>> = arb { randomSource ->
    val map = Arb.map(keyArb, valueArb, minSize, maxSize).values(randomSource)
    map.map { sample -> Cache<K, V>(sample.value) }
}
