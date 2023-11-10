package com.cocomo.web

import com.cocomo.library.event.Event
import com.cocomo.library.event.EventPublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController(
    val eventPublisher: EventPublisher
) {

    @GetMapping
    fun test() {
        val event = Event("success")
        eventPublisher.publish(event)
        eventPublisher.publish(event)
        eventPublisher.publish(event)
    }

    @GetMapping("fail")
    fun testFail() {
        val event = Event("fail")
        eventPublisher.publish(event)
    }
}