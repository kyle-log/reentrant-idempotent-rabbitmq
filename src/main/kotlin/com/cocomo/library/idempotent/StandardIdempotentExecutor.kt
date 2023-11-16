package com.cocomo.library.idempotent

import org.springframework.cache.Cache
import org.springframework.cache.CacheManager

class StandardIdempotentExecutor(
    val cacheManager: CacheManager,
) : IdempotentExecutor {

    private val cache: Cache by lazy {
        cacheManager.getCache("idempotent-checker") ?: throw IllegalStateException("Cache not ready")
    }

    override fun execute(key: String, f: () -> Unit) {
        if (alreadyProcessed(key)) {
            println("already executed")
        } else {
            f()
            cache.put(key, IdempotentValue(true))
        }
    }

    private fun alreadyProcessed(key: String): Boolean {
        val idempotentValue = cache.get(key, IdempotentValue::class.java) ?: IdempotentValue(false)
        return idempotentValue.value
    }
}

data class IdempotentValue(val value: Boolean)