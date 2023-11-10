package com.cocomo.library.event

interface EventPublisher {
    fun <E : Any> publish(event: E)
}