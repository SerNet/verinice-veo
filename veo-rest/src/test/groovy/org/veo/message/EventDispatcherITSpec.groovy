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

import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS
import static org.veo.rest.VeoRestConfiguration.PROFILE_BACKGROUND_TASKS

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

import org.veo.core.VeoSpringSpec
import org.veo.core.entity.event.StoredEvent
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

    @Value('${veo.message.dispatch.routing-key-prefix}')
    String routingKeyPrefix

    Set<EventMessage> sentEvents = new HashSet<>(NUM_EVENTS)

    @Autowired
    MessagingJob messagingJob

    @Autowired
    private RabbitAdmin rabbitAdmin

    @Value('${veo.message.consume.queue}')
    String testQueue

    static final Instant FOREVER_AND_EVER = Instant.now().plus(365000, ChronoUnit.DAYS)

    def setupSpec() {
        if (!System.env.containsKey('SPRING_RABBITMQ_HOST')) {
            println("Test will start RabbitMQ container...")

            rabbit = new GenericContainer("rabbitmq:3-management")
                    .withExposedPorts(5672, 15672)
                    .waitingFor(Wait.forListeningPort())
                    .tap {
                        it.start()
                    }

            System.properties.putAll([
                "spring.rabbitmq.host": rabbit.getContainerIpAddress(),
                "spring.rabbitmq.port": rabbit.getMappedPort(5672),
            ])
        }
    }

    def "Message queue roundtrip"() {
        given: "an event subscriber for a roundtrip check"
        eventSubscriber.setExpectedEvents(NUM_EVENTS)
        def confirmationLatch = new CountDownLatch(NUM_EVENTS)

        when: "the events are published"
        Long id = 0
        NUM_EVENTS.times {
            StoredEvent event = StoredEventData.newInstance("testEvent", routingKeyPrefix + "veo.testmessage")
            event.setId(id++)
            sentEvents.add EventMessage.from(event)
        }
        eventDispatcher.sendAsync(sentEvents, { EventMessage e, boolean ack ->
            if (ack) {
                confirmationLatch.countDown()
            }
        })

        then: "all events are received within an acceptable timeframe"
        eventSubscriber.getLatch().await(10, TimeUnit.SECONDS)
        eventSubscriber.getReceivedEvents().size() == NUM_EVENTS

        and: "the events are equal after marshalling and unmarshalling"
        eventSubscriber.getReceivedEvents() as Set == sentEvents

        and: "all publications has been confirmed"
        confirmationLatch.await(2, TimeUnit.SECONDS)
    }

    def "Publisher receives no confirmation without a matching queue"() {
        when: "an event for a routing key that nobody listens to is published"
        def confirmationLatch = new CountDownLatch(1)
        def event = new EventMessage("funky key that no one listens to", "content", 1, Instant.now())
        eventDispatcher.sendAsync(event, { EventMessage e, boolean ack ->
            if (ack) {
                confirmationLatch.countDown()
            }
        })

        then: "the publisher never receives a positive confirmation"
        !confirmationLatch.await(2, TimeUnit.SECONDS)
    }

    def "Dispatch stored events with confirmations"() {
        given:
        def events = new HashSet<StoredEventData>()
        NUM_EVENTS.times {
            events.add new StoredEventData().tap {
                routingKey = routingKeyPrefix + "veo.testmessage"
            }
        }

        when:
        storedEventRepository.saveAll(events)

        then:
        storedEventRepository.findPendingEvents(FOREVER_AND_EVER).size() == NUM_EVENTS

        when:
        messagingJob.sendMessages()

        then:
        storedEventRepository.findPendingEvents(FOREVER_AND_EVER).isEmpty()
    }
    def cleanup() {
        def purgeCount = rabbitAdmin.purgeQueue(testQueue)
        if (purgeCount>0)
            log.info("Test cleanup: purged {} remaining messages in test queue.", purgeCount)
        eventStoreDataRepository.deleteAll()
    }
}
