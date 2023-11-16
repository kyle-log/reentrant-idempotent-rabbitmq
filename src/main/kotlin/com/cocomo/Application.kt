package com.cocomo

import com.cocomo.library.amqp.rabbit.RabbitEventProcessor
import com.cocomo.library.event.ApplicationEventProcessor
import com.cocomo.library.event.CustomApplicationEventMulticaster
import com.cocomo.library.event.StandardApplicationEventProcessor
import com.cocomo.library.event.decoratedBy
import com.cocomo.library.idempotent.IdempotentApplicationEventProcessor
import com.cocomo.library.idempotent.IdempotentExecutor
import com.cocomo.library.idempotent.StandardIdempotentExecutor
import com.cocomo.worker.EventHandler
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

@Configuration
class Configuration {

    // You can change cacheManager to redis or something
    @Bean
    fun idempotentChecker() = StandardIdempotentExecutor(
        cacheManager = ConcurrentMapCacheManager(),
    )

    @Bean
    fun applicationEventProcessor(
        idempotentExecutor: IdempotentExecutor,
    ) = StandardApplicationEventProcessor()
        .decoratedBy { IdempotentApplicationEventProcessor(it, idempotentExecutor) }

    // Do not change bean name
    @Bean("applicationEventMulticaster")
    fun customApplicationEventMulticaster(
        applicationEventProcessor: ApplicationEventProcessor,
    ) = CustomApplicationEventMulticaster(
        applicationEventProcessor = applicationEventProcessor,
    )

    @Bean
    fun eventHandler() = EventHandler()

    @Bean
    fun rabbitEventProcessor(
        connectionFactory: ConnectionFactory,
        applicationEventPublisher: ApplicationEventPublisher,
        environment: Environment
    ) = RabbitEventProcessor.create(
        connectionFactory = connectionFactory,
        applicationEventPublisher = applicationEventPublisher,
    )
}
