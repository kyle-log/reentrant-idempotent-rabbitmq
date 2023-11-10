package com.cocomo.library.event

import com.cocomo.library.idempotent.IdempotentEvent
import java.util.*

data class Event(
    val value: String,
    override val uuid: String = UUID.randomUUID().toString(),
) : IdempotentEvent