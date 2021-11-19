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
import static org.veo.message.EventDispatcher.NOP_CALLBACK

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.spock.Testcontainers

import org.veo.MessagingJob
import org.veo.core.VeoSpringSpec
import org.veo.core.entity.event.StoredEvent
import org.veo.persistence.access.jpa.StoredEventDataRepository
import org.veo.persistence.entity.jpa.StoredEventData

import groovy.util.logging.Slf4j
import spock.lang.IgnoreIf
import spock.lang.Shared

/**
 * Tests messaging using a RabbitMQ container bootstrapped by the test itself.
 *
 * If you want to test against a running RabbitMQ instance instead, use
 * EventDispatcherRemoteMqITSpec instead.
 *
 * This test is disabled in CI environments where the availability from a docker daemon
 * is not guaranteed.
 *
 * {@see EventDispatcherRemoteMqITSpec}
 */
@SpringBootTest(
classes = [EventDispatcherITSpec.class,
    RabbitMQSenderConfiguration.class,
]
)
@ActiveProfiles(["test", "publishing-enabled"])
@Testcontainers
@IgnoreIf({
    Boolean.valueOf(env['CI'])
})
@Slf4j
@DirtiesContext(classMode = AFTER_CLASS)
class EventDispatcherITSpec extends VeoSpringSpec {

    public static final int NUM_EVENTS = 100

    @Shared
    GenericContainer rabbit = new GenericContainer("rabbitmq:3-management")
    .withExposedPorts(5672, 15672)
    .waitingFor(Wait.forListeningPort())
    .tap {
        it.start()
    }

    @Autowired
    EventDispatcher eventDispatcher

    @Autowired
    TestEventSubscriber eventSubscriber

    @Autowired
    StoredEventDataRepository storedEventRepository

    @Value('${veo.test.message.dispatch.routing_key_prefix:veo.testmessage.}')
    String routingKeyPrefix

    Set<EventMessage> sentEvents = new HashSet<>(NUM_EVENTS)

    @Autowired
    MessagingJob messagingJob

    static final Instant FOREVER_AND_EVER = Instant.now().plus(365000, ChronoUnit.DAYS)

    @Bean
    public TestEventSubscriber getSubscriber() {
        return new TestEventSubscriber()
    }

    def setupSpec() {
        println("Test will start RabbitMQ container...")
        System.properties.putAll([
            "spring.rabbitmq.host": rabbit.getContainerIpAddress(),
            "spring.rabbitmq.port": rabbit.getMappedPort(5672),
        ])
    }

    def "Message queue roundtrip"() {
        given: "an event subscriber for a roundtrip check"
        eventSubscriber.setExpectedEvents(NUM_EVENTS)

        when: "the events are published"
        Long id = 0
        NUM_EVENTS.times {
            StoredEvent event = StoredEventData.newInstance("testEvent", routingKeyPrefix + "storedevent")
            event.setId(id++)
            sentEvents.add EventMessage.from(event)
        }
        eventDispatcher.sendAsync(sentEvents, NOP_CALLBACK)

        then: "all events are received within an acceptable timeframe"
        eventSubscriber.getLatch().await(10, TimeUnit.SECONDS)
        eventSubscriber.getReceivedEvents().size() == NUM_EVENTS

        and: "the events are equal after marshalling and unmarshalling"
        eventSubscriber.getReceivedEvents() as Set == sentEvents
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
        storedEventRepository.findPendingEvents(FOREVER_AND_EVER).isEmpty()
    }
}
