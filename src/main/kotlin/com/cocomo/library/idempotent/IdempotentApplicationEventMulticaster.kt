package com.cocomo.library.idempotent

import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.PayloadApplicationEvent
import org.springframework.context.event.AbstractApplicationEventMulticaster
import org.springframework.context.event.GenericApplicationListener
import org.springframework.core.ResolvableType
import org.springframework.util.ErrorHandler

class IdempotentApplicationEventMulticaster(
    private val idempotenceChecker: IdempotentChecker,
    private val errorHandler: ErrorHandler = AlwaysThrowErrorHandler(),
) : AbstractApplicationEventMulticaster() {

    override fun multicastEvent(event: ApplicationEvent) {
        multicastEvent(event, resolveDefaultEventType(event))
    }

    override fun multicastEvent(event: ApplicationEvent, eventType: ResolvableType?) {
        val type = eventType ?: resolveDefaultEventType(event)
        for (listener in getApplicationListeners(event, type)) {
            try {
                invokeListener(listener, event)
            } catch (err: Throwable) {
                errorHandler.handleError(err)
            }
        }
    }

    private fun resolveDefaultEventType(event: ApplicationEvent): ResolvableType {
        return ResolvableType.forInstance(event)
    }

    private fun invokeListener(listener: ApplicationListener<*>, event: ApplicationEvent) {
        if ((listener is GenericApplicationListener) &&
            (event is PayloadApplicationEvent<*>) &&
            (event.payload is IdempotentEvent)
        ) {
            idempotenceChecker.runIfNotProcessed(
                key = IdempotentKey("${listener.listenerId}-${(event.payload as IdempotentEvent).uuid}")
            ) {
                doInvokeListener(listener, event)
            }
        } else {
            doInvokeListener(listener, event)
        }
    }

    private fun doInvokeListener(listener: ApplicationListener<*>, event: ApplicationEvent) {
        try {
            @Suppress("UNCHECKED_CAST")
            (listener as ApplicationListener<ApplicationEvent>).onApplicationEvent(event)
        } catch (ex: ClassCastException) {
            val msg = ex.message
            if (msg == null ||
                matchesClassCastMessage(msg, event.javaClass) ||
                event is PayloadApplicationEvent<*> && matchesClassCastMessage(msg, event.payload.javaClass)
            ) {
                // Possibly a lambda-defined listener which we could not resolve the generic event type for
                // -> let's suppress the exception.
            } else {
                throw ex
            }
        }
    }

    private fun matchesClassCastMessage(classCastMessage: String, eventClass: Class<*>): Boolean {
        // On Java 8, the message starts with the class name: "java.lang.String cannot be cast..."
        if (classCastMessage.startsWith(eventClass.name)) {
            return true
        }
        // On Java 11, the message starts with "class ..." a.k.a. Class.toString()
        if (classCastMessage.startsWith(eventClass.toString())) {
            return true
        }
        // On Java 9, the message used to contain the module name: "java.base/java.lang.String cannot be cast..."
        val moduleSeparatorIndex = classCastMessage.indexOf('/')
        return (moduleSeparatorIndex != -1) &&
                (classCastMessage.startsWith(eventClass.name, moduleSeparatorIndex + 1))
        // Assuming an unrelated class cast failure...
    }
}