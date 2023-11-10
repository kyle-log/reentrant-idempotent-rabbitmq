package com.cocomo.worker

import com.cocomo.library.event.Event
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener

class EventHandler {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @EventListener
    fun handle(event: Event) {
        when (event.value) {
            "success" -> logger.info { "Handle message. $event" }
            else -> throw RuntimeException("failed")
        }
    }
}