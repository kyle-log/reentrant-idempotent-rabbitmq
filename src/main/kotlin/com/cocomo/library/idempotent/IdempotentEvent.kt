package com.cocomo.library.idempotent

interface IdempotentEvent {
    val uuid: String
}