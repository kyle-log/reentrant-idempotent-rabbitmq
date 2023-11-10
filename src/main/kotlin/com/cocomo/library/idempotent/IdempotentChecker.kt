package com.cocomo.library.idempotent

interface IdempotentChecker {
    fun alreadyProcessed(key: IdempotentKey): Boolean
    fun mark(key: IdempotentKey)
}

@JvmInline
value class IdempotentKey(val value: String)

fun <T> IdempotentChecker.runIfNotProcessed(key: IdempotentKey, block: () -> T) =
    if (alreadyProcessed(key)) {
        // do nothing
        println("already processed")
    } else {
        block()
        mark(key)
        println("processed")
    }