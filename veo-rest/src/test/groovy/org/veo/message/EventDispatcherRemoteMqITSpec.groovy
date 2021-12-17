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

import org.veo.core.VeoSpringSpec
import org.veo.core.entity.event.StoredEvent
import org.veo.jobs.MessagingJob
import org.veo.persistence.access.jpa.StoredEventDataRepository
import org.veo.persistence.entity.jpa.StoredEventData

import groovy.util.logging.Slf4j
import spock.lang.Requires

/**
 * Tests sending and receiving events against a remote message queue. Local tests will instead use a self-started
 * RabbitMQ container.
 *
 * Set the host and port from the remote queue to be tested
 * in your {@code application-local.yaml} or by using environment variables.
 *
 *
 * {@see EventDispatcherITSpec}
 */
@SpringBootTest(
classes = [TestEventSubscriber.class,
    RabbitMQSenderConfiguration.class,
]
)
@ActiveProfiles(["test", "background-tasks"])
@Requires({
    Boolean.valueOf(env['CI'])
})
@Slf4j
@DirtiesContext(classMode = AFTER_CLASS)
class EventDispatcherRemoteMqITSpec extends VeoSpringSpec {

    public static final int NUM_EVENTS = 100

    @Autowired
    EventDispatcher eventDispatcher

    @Autowired
    TestEventSubscriber eventSubscriber

    @Autowired
    RabbitAdmin rabbitAdmin

    @Autowired
    StoredEventDataRepository storedEventRepository

    @Autowired
    MessagingJob messagingJob

    static final Instant FOREVER_AND_EVER = Instant.now().plus(365000,
    ChronoUnit.DAYS)


    @Value('${veo.test.message.dispatch.routing_key_prefix:veo.testmessage.}')
    String routingKeyPrefix

    @Value('${veo.test.message.consume.queue:veo.entity_test_queue}')
    String testQueue

    @Value('${veo.test.message.consume.routing_key:veo.testmessage.#}')
    String binding

    Set<EventMessage> sentEvents = new HashSet<>(NUM_EVENTS)

    def setup() {
        def purgeCount = rabbitAdmin.purgeQueue(testQueue)
        if (purgeCount>0)
            log.info("Test setup: Purged {} remaining messages in test queue.", purgeCount)
        storedEventRepository.deleteAll()
    }

    def "Message queue roundtrip"() {
        given: "an event subscriber for a roundtrip check"
        eventSubscriber.setExpectedEvents(NUM_EVENTS)
        log.info("Testing message roundtrip with: routing key prefix: {}," +
                "queue: {}, binding: {}", routingKeyPrefix, testQueue, binding)

        when: "the events are published"
        def confirmationLatch = new CountDownLatch(NUM_EVENTS)
        Long id = 0
        NUM_EVENTS.times {
            StoredEvent event = StoredEventData.newInstance("testEvent", routingKeyPrefix + "storedevent")
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
                routingKey = routingKeyPrefix + "storedevent"
            }
        }

        when:
        storedEventRepository.saveAll(events)

        then:
        storedEventRepository.findPendingEvents(FOREVER_AND_EVER).size() == NUM_EVENTS

        when:
        messagingJob.sendMessages()

        then:
        storedEventRepository.findPendingEvents(FOREVER_AND_EVER).size() == 0
    }

    def cleanup() {
        def purgeCount = rabbitAdmin.purgeQueue(testQueue)
        if (purgeCount>0)
            log.info("Test cleanup: purged {} remaining messages in test queue.", purgeCount)
        storedEventRepository.deleteAll()
    }
}
