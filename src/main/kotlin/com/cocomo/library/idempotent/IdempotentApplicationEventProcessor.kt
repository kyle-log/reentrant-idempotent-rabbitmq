package com.cocomo.library.idempotent

import com.cocomo.library.event.ApplicationEventProcessor
import org.springframework.context.ApplicationEvent
import org.springframework.context.PayloadApplicationEvent
import org.springframework.context.event.GenericApplicationListener

class IdempotentApplicationEventProcessor(
    private val delegate: ApplicationEventProcessor,
    private val idempotenceChecker: IdempotentExecutor,
) : ApplicationEventProcessor {

    override fun process(listener: GenericApplicationListener, event: ApplicationEvent) {
        if (event !is PayloadApplicationEvent<*>) {
            return delegate.process(listener, event)
        }
        val payload = event.payload
        if (payload !is IdempotentEvent) {
            return delegate.process(listener, event)

        }
        idempotenceChecker.execute(
            key = "${listener.listenerId}-${payload.idempotentKey}"
        ) {
            delegate.process(listener, event)
        }
    }
}