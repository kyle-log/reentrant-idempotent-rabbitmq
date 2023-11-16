package com.cocomo.library.idempotent

interface IdempotentEvent {
    val idempotentKey: String
}
