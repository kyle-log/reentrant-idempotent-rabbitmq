package com.cocomo.library.amqp.rabbit

import com.cocomo.library.amqp.EventMessageConverter
import com.cocomo.library.amqp.EventMessageConverter.DestinationMessage
import com.cocomo.library.event.EventPublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageListener
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.*
import java.util.concurrent.Executors

class RabbitEventProcessor private constructor(
    val messageConverter: EventMessageConverter,
    val eventPublisher: RabbitTemplate,
    val applicationEventPublisher: ApplicationEventPublisher,
    val messageListenerContainer: AbstractMessageListenerContainer,
) : EventPublisher,
    MessageListener,
    SmartLifecycle,
    InitializingBean,
    DisposableBean,
    BeanFactoryAware,
    ApplicationContextAware,
    ApplicationEventPublisherAware {

    init {
        messageListenerContainer.setMessageListener(this)
    }

    override fun <E : Any> publish(event: E) {
        val message = messageConverter.toMessage(event)
        eventPublisher.send(message.exchange, message.routingKey, message.content)
    }

    override fun onMessage(message: Message) {
        val event = messageConverter.fromMessage<Any>(message)
        val isDead = XDeathHeaders.of(message).counts() >= 3
        if (isDead) {
            val deadMessage = DestinationMessage("amq.direct", "worker.dead", message)
            eventPublisher.send(deadMessage.exchange, deadMessage.routingKey, deadMessage.content)
        } else {
            applicationEventPublisher.publishEvent(event)
        }
    }

    override fun setBeanFactory(beanFactory: BeanFactory) {
        eventPublisher.setBeanFactory(beanFactory)
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        messageListenerContainer.setApplicationContext(applicationContext)
    }

    override fun setApplicationEventPublisher(applicationEventPublisher: ApplicationEventPublisher) {
        messageListenerContainer.setApplicationEventPublisher(applicationEventPublisher)
    }

    override fun afterPropertiesSet() {
        eventPublisher.afterPropertiesSet()
        messageListenerContainer.afterPropertiesSet()
    }

    override fun start() = messageListenerContainer.start()

    override fun stop() = messageListenerContainer.stop()

    override fun stop(callback: Runnable) = messageListenerContainer.stop(callback)

    override fun isRunning() = messageListenerContainer.isRunning

    override fun isAutoStartup() = messageListenerContainer.isAutoStartup

    override fun getPhase() = messageListenerContainer.phase

    override fun destroy() {
        eventPublisher.destroy()
        messageListenerContainer.destroy()
    }

    companion object {
        fun create(
            connectionFactory: ConnectionFactory,
            applicationEventPublisher: ApplicationEventPublisher,
        ): RabbitEventProcessor {
            val rabbitTemplate = RabbitTemplate(connectionFactory)
            val rabbitAdmin = RabbitAdmin(connectionFactory).apply {
                isAutoStartup = false
                setIgnoreDeclarationExceptions(false)
                afterPropertiesSet()
            }

            val rabbitMessageListenerContainer = SimpleMessageListenerContainer().apply {
                setConnectionFactory(connectionFactory)
                setQueueNames("worker")
                setConcurrency("1")
                setTaskExecutor(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()))
                setMismatchedQueuesFatal(false)
                setMissingQueuesFatal(false)
                setAutoDeclare(false)
                setAmqpAdmin(rabbitAdmin)
                setConsumerStartTimeout(10000)
                setShutdownTimeout(10 * 1000)
                setPrefetchCount(100)
                setDefaultRequeueRejected(false)
            }

            return RabbitEventProcessor(
                messageConverter = EventMessageConverter(
                    defaultExchange = "amq.fanout",
                ),
                eventPublisher = rabbitTemplate,
                applicationEventPublisher = applicationEventPublisher,
                messageListenerContainer = rabbitMessageListenerContainer,
            )
        }
    }
}
