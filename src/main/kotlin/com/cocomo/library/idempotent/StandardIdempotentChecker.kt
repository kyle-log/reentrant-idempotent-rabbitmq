package com.cocomo.library.idempotent

import org.springframework.cache.Cache
import org.springframework.cache.CacheManager

class StandardIdempotentChecker(
    val cacheManager: CacheManager,
): IdempotentChecker {

    private val cache: Cache by lazy {
        cacheManager.getCache("idempotent-checker") ?: throw IllegalStateException("Cache not ready")
    }

    override fun alreadyProcessed(key: IdempotentKey): Boolean {
        val idempotentValue = cache.get(key.value, IdempotentValue::class.java) ?: IdempotentValue(false)
        return idempotentValue.value
    }

    override fun mark(key: IdempotentKey) {
        cache.put(key.value, IdempotentValue(true))
    }
}

data class IdempotentValue(val value: Boolean)