package test

import java.util.Collections
import java.util.concurrent.locks.ReentrantLock
import java.util.stream.IntStream
import kotlin.concurrent.withLock

val capacity = 10
val initializer: (Int) -> Int = { it * 2 }
val accessCount = 10
val loopCount = 1000000
val numberOfMeasurements = 20

fun main(args: Array<String>) {
    val t1 = measurement(LruCache1(capacity, initializer))
    val t2 = measurement(LruCache3(capacity, initializer))
    println("cache1 " + t1)
    println("cache2 " + t2)
}

fun measurement(cache: LruCache<Int, out Any>): Double =
        (1..numberOfMeasurements)
                .map {
                    val t0 = System.currentTimeMillis()
                    run(cache)
                    System.currentTimeMillis() - t0
                }
                .average()

fun run(cache: LruCache<Int, out Any>) =
        IntStream.range(0, loopCount).parallel().forEach { cache[it / accessCount] }

interface LruCache<K, V> {
    operator fun get(key: K): V
}

class LruCache1<K, V>(val capacity: Int, private val initializer: (K) -> V) : LruCache<K, V> {
    private val map = Collections.synchronizedMap(object : LinkedHashMap<K, V>(capacity, 1.0F, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?) = size > capacity
    })

    override operator fun get(key: K): V = map.computeIfAbsent(key, initializer)
    override fun toString() = map.toString()
}

class LruCache3<K, V>(val capacity: Int, private val initializer: (K) -> V)  : LruCache<K, V> {
    private val lock = ReentrantLock()
    private val map = object: LinkedHashMap<K, V>(capacity, 1F, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?) = size > capacity
    }

    override operator fun get(key: K): V = lock.withLock { return map.getOrPut(key) { initializer(key) } }
    override fun toString(): String = lock.withLock { return map.toString() }
}
