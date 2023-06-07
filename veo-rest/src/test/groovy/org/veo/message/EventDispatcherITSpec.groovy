/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.message

import static java.util.concurrent.TimeUnit.SECONDS
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS
import static org.veo.rest.VeoRestConfiguration.PROFILE_BACKGROUND_TASKS

import java.util.concurrent.CountDownLatch

import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.GenericContainer

import org.veo.core.VeoSpringSpec
import org.veo.jobs.MessageDeletionJob
import org.veo.jobs.MessagingJob
import org.veo.persistence.access.jpa.StoredEventDataRepository
import org.veo.persistence.entity.jpa.StoredEventData

import groovy.util.logging.Slf4j
import spock.lang.AutoCleanup
import spock.lang.Shared

/**
 * Tests messaging using a RabbitMQ container.
 *
 * If you want to test against a running RabbitMQ instance instead, set the
 * SPRING_RABBITMQ_HOST variable. Otherwise, the test will start a container itself.
 */
@SpringBootTest(
classes = [TestEventSubscriber.class,
    RabbitMQSenderConfiguration.class,
]
)
@ActiveProfiles(["test", PROFILE_BACKGROUND_TASKS])
@DirtiesContext(classMode = AFTER_CLASS)
@Slf4j
class EventDispatcherITSpec extends VeoSpringSpec {

    public static final int NUM_EVENTS = 100

    @Shared
    @AutoCleanup("stop")
    private GenericContainer rabbit

    @Autowired
    EventDispatcher eventDispatcher

    @Autowired
    TestEventSubscriber eventSubscriber

    @Autowired
    StoredEventDataRepository storedEventRepository

    @Value('${veo.message.routing-key-prefix}')
    String routingKeyPrefix

    @Autowired
    MessagingJob messagingJob

    @Autowired
    MessageDeletionJob messageDeletionJob

    @Autowired
    private RabbitAdmin rabbitAdmin

    @Value('${veo.message.queues.veo}')
    String testQueue

    def setupSpec() {
        rabbit = TestContainersUtil.startRabbitMqContainer()
    }

    def setup() {
        eventSubscriber.receivedEvents.clear()
    }

    def "jobs send messages from DB"() {
        given:
        def confirmationLatch = new CountDownLatch(NUM_EVENTS)
        eventDispatcher.addAckCallback { confirmationLatch.countDown() }

        when: "storing outgoing messages"
        def events = (1..NUM_EVENTS)
                .collect { new StoredEventData() }
                .each { it.routingKey = routingKeyPrefix + "veo.testmessage" }
                .each { storedEventRepository.save(it) }

        then: "DB is filled and subscriber has not received anything yet"
        storedEventRepository.findAll().size() == NUM_EVENTS
        eventSubscriber.receivedEvents.size() == 0

        when: "sending stored messages"
        messagingJob.sendMessages()

        then: "the table should be cleared by deletion job"
        defaultPolling.eventually {
            messageDeletionJob.deleteMessages()
            storedEventRepository.findAll().isEmpty()
        }

        and: "confirmations have been received"
        confirmationLatch.await(2, SECONDS)

        and: "messages should have been received"
        eventSubscriber.receivedEvents.size() == NUM_EVENTS
        eventSubscriber.receivedEvents ==~ events.collect { EventMessage.from(it) }
    }

    def "deletion job tolerates already deleted message"() {
        when: "storing and sending two messages"
        def messages = (1..2)
                .collect { new StoredEventData() }
                .each { it.routingKey = routingKeyPrefix + "veo.testmessage" }
                .each { storedEventRepository.save(it) }
        messagingJob.sendMessages()

        and: "one message is already deleted for some reason"
        storedEventRepository.delete(messages.last())

        then: "the deletion job should still clear the table"
        defaultPolling.eventually {
            messageDeletionJob.deleteMessages()
            storedEventRepository.findAll().isEmpty()
        }
    }

    def "Publisher receives no confirmation without a matching queue"() {
        given:
        def confirmationLatch = new CountDownLatch(1)
        eventDispatcher.addAckCallback { confirmationLatch.countDown() }

        when: "an event for a routing key that nobody listens to is published"
        storedEventRepository.save(new StoredEventData().tap {
            it.routingKey = "funky key that no one listens to"
        })
        messagingJob.sendMessages()

        then: "the publisher never receives a positive confirmation"
        !confirmationLatch.await(2, SECONDS)

        and: "the message is not deleted from the DB"
        messageDeletionJob.deleteMessages()
        storedEventRepository.findAll().size() == 1
    }

    def cleanup() {
        def purgeCount = rabbitAdmin.purgeQueue(testQueue)
        if (purgeCount > 0)
            log.info("Test cleanup: purged {} remaining messages in test queue.", purgeCount)
        eventStoreDataRepository.deleteAll()
    }
}
