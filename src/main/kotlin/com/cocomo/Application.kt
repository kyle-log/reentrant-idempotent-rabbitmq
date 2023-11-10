package com.cocomo

import com.cocomo.library.amqp.rabbit.RabbitEventProcessor
import com.cocomo.library.idempotent.IdempotentApplicationEventMulticaster
import com.cocomo.library.idempotent.IdempotentChecker
import com.cocomo.library.idempotent.StandardIdempotentChecker
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
    fun idempotentChecker() = StandardIdempotentChecker(
        cacheManager = ConcurrentMapCacheManager(),
    )

    // Do not change bean name
    @Bean("applicationEventMulticaster")
    fun idempotentApplicationEventMulticaster(
        idempotenceChecker: IdempotentChecker,
    ) = IdempotentApplicationEventMulticaster(
        idempotenceChecker = idempotenceChecker,
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
