package com.cocomo.library.idempotent

interface IdempotentExecutor {
    fun execute(key: String, f: () -> Unit)
}
