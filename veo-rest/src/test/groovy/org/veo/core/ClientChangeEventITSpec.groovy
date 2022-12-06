/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Urs Zeidler
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
package org.veo.core

import static org.veo.core.entity.Client.ClientState.*
import static org.veo.rest.VeoRestConfiguration.PROFILE_BACKGROUND_TASKS

import java.time.Instant

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

import org.veo.core.entity.Client
import org.veo.core.entity.Client.ClientState
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.events.MessageCreatorImpl
import org.veo.message.EventDispatcher
import org.veo.message.EventMessage
import org.veo.message.RabbitMQSenderConfiguration
import org.veo.message.TestEventSubscriber
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

import groovy.util.logging.Slf4j
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.util.concurrent.PollingConditions

@SpringBootTest(
classes = [TestEventSubscriber.class,
    RabbitMQSenderConfiguration.class,
]
)
@ActiveProfiles(["test", PROFILE_BACKGROUND_TASKS])
@Slf4j
class ClientChangeEventITSpec  extends VeoSpringSpec {
    @Shared
    @AutoCleanup("stop")
    private GenericContainer rabbit

    @Autowired EventDispatcher eventDispatcher;

    @Autowired
    private ClientRepositoryImpl repository
    @Autowired
    private UnitRepositoryImpl unitRepository
    @Autowired
    private DomainRepositoryImpl domainRepository

    @Value('${veo.message.consume.subscription-routing-key-prefix}')
    String routingKeyPrefix

    String messageType = MessageCreatorImpl.ROUTING_KEY_ELEMENT_CLIENT_CHANGE
    String routingKey

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

    def setup() {
        routingKey = routingKeyPrefix + MessageCreatorImpl.ROUTING_KEY_ELEMENT_CLIENT_CHANGE
    }

    def"publish the deletion event"() {
        given: "a client and an unit"
        Client client = repository.save(newClient {
            name = "Demo Client"
            state = DEACTIVATED
        })

        def cId = client.getIdAsString()
        def unit = unitRepository.save(newUnit(client))
        when:"we send the event"
        def eContent = """{
            "eventType": "$messageType",
            "clientId": "$cId",
            "type": "DELETION"
        }"""
        def msg = new EventMessage(routingKey,eContent,1,Instant.now())
        log.info("publish event: {}", msg)
        eventDispatcher.send(msg)

        then: "the event is sent"
        new PollingConditions().within(5) {
            !unitRepository.exists(unit.id)
            !repository.exists(client.id)
        }
    }

    def "sync the maxUnits attribute"() {
        given: "a client and two units"
        Client client = repository.save(newClient {
            name = "Demo Client"
            state = ACTIVATED
        })

        def cId = client.getIdAsString()
        unitRepository.save(newUnit(client))
        unitRepository.save(newUnit(client))
        when:"we send the event"
        eventDispatcher.send(new EventMessage(routingKey,"""{
            "eventType": "$messageType",
            "clientId": "$cId",
            "type": "MODIFICATION",
            "maxUnits": 5
        }""",1,Instant.now()))

        then: "the event is sent"
        new PollingConditions().within(5) {
            repository.findById(Key.uuidFrom(cId)).get().maxUnits == 5
        }

        when:"we send the next change"
        eventDispatcher.send(new EventMessage(routingKey,"""{
            "eventType": "$messageType",
            "clientId": "$cId",
            "type": "MODIFICATION",
            "maxUnits": 15
        }""",1,Instant.now()))

        then: "the event is sent and the maxUnits is updated"
        new PollingConditions().within(5) {
            repository.findById(Key.uuidFrom(cId)).get().maxUnits == 15
        }

        when:"we send the next change -> less units than exiting"
        eventDispatcher.send(new EventMessage(routingKey,"""{
            "eventType": "$messageType",
            "clientId": "$cId",
            "type": "MODIFICATION",
            "maxUnits": 1
        }""",1,Instant.now()))

        then: "the event is sent and the maxUnits is updated"
        new PollingConditions().within(5) {
            repository.findById(Key.uuidFrom(cId)).get().maxUnits == 1
        }
    }

    def"publish wrong event type for client state"(ClientState startState, String eventType) {
        given: "a client an a unit"
        Client client = repository.save(newClient {
            name = "Demo Client"
            state = startState
        })

        def cId = client.getIdAsString()
        def unit = unitRepository.save(newUnit(client))
        when:"we send the event"

        def eContent = """{
            "eventType": "$messageType",
            "clientId": "$cId",
            "type": "$eventType"
        }"""
        def msg = new EventMessage(routingKeyPrefix+MessageCreatorImpl.ROUTING_KEY_ELEMENT_CLIENT_CHANGE,eContent,1,Instant.now())
        log.info("publish event: {}", msg)
        eventDispatcher.send(msg)

        then: "the event is sent"
        new PollingConditions().within(5) {
            unitRepository.exists(unit.id)
            repository.getById(client.id).state == startState
        }

        where:

        startState | eventType
        //when created only activation is valid
        CREATED | "DELETION"
        CREATED | "MODIFICATION"
        CREATED | "DEACTIVATION"
        CREATED | "CREATION"
        //when activated only deactivation is valid
        ACTIVATED | "DELETION"
        ACTIVATED | "ACTIVATION"
        ACTIVATED | "CREATION"
        //when activated modification event does not change the state
        ACTIVATED | "MODIFICATION"
        //when deactivated activation and deletion is valid
        DEACTIVATED | "MODIFICATION"
        DEACTIVATED | "CREATION"
        DEACTIVATED | "DEACTIVATION"
        //when deleted nothing is valid
        DELETED | "ACTIVATION"
        DELETED | "MODIFICATION"
        DELETED | "DEACTIVATION"
        DELETED | "CREATION"
        DELETED | "DELETION"
    }

    def"publish event type for next client state"(ClientState startState, String eventType, ClientState nextState) {
        given: "a client an a unit"
        Client client = repository.save(newClient {
            name = "Demo Client"
            state = startState
        })

        def cId = client.getIdAsString()
        def unit = unitRepository.save(newUnit(client))
        when:"we send the event"
        def eContent = """{
            "eventType": "$messageType",
            "clientId": "$cId",
            "type": "$eventType"
        }"""
        def msg = new EventMessage(routingKeyPrefix+MessageCreatorImpl.ROUTING_KEY_ELEMENT_CLIENT_CHANGE,eContent,1,Instant.now())
        log.info("publish event: {}", msg)
        eventDispatcher.send(msg)

        then: "the event is sent"
        new PollingConditions().within(5) {
            repository.getById(client.id).state == nextState
        }

        where:

        startState | eventType | nextState
        //all valid state transitions
        CREATED | "ACTIVATION" | ACTIVATED
        ACTIVATED | "MODIFICATION" | ACTIVATED
        ACTIVATED | "DEACTIVATION" | DEACTIVATED
        DEACTIVATED | "ACTIVATION" | ACTIVATED
    }

    def "publish valid state transitions start with CREATED and end with DELETED"() {
        given: "a client an a unit"
        Client client = repository.save(newClient {
            name = "Demo Client"
            state = CREATED
        })
        Domain domain = newDomain(client) {
            name = "27001"
            description = "ISO/IEC"
            abbreviation = "ISO"
        }
        client.addToDomains(domain)
        repository.save(client)

        def cId = client.getIdAsString()
        def unit = unitRepository.save(newUnit(client))
        when:"we send the event for activation"
        def eContent = """{
            "eventType": "$messageType",
            "clientId": "$cId",
            "type": "ACTIVATION"
        }"""

        def msg = new EventMessage(routingKeyPrefix+MessageCreatorImpl.ROUTING_KEY_ELEMENT_CLIENT_CHANGE,eContent,1,Instant.now())
        log.info("publish event: {}", msg)
        eventDispatcher.send(msg)

        then: "the event is sent"
        new PollingConditions().within(5) {
            repository.getById(client.id).state == ACTIVATED
        }

        when:"we send the event for modification"
        eContent = """{
            "eventType": "$messageType",
            "clientId": "$cId",
            "type": "MODIFICATION"
        }"""
        msg = new EventMessage(routingKeyPrefix+MessageCreatorImpl.ROUTING_KEY_ELEMENT_CLIENT_CHANGE,eContent,1,Instant.now())
        log.info("publish event: {}", msg)
        eventDispatcher.send(msg)

        then: "the event is sent"
        new PollingConditions().within(5) {
            repository.getById(client.id).state == ACTIVATED
        }

        when:"we send the event for deactivation"
        eContent = """{
            "eventType": "$messageType",
            "clientId": "$cId",
            "type": "DEACTIVATION"
        }"""

        msg = new EventMessage(routingKeyPrefix+MessageCreatorImpl.ROUTING_KEY_ELEMENT_CLIENT_CHANGE,eContent,1,Instant.now())
        log.info("publish event: {}", msg)
        eventDispatcher.send(msg)

        then: "the event is sent"
        new PollingConditions().within(5) {
            repository.getById(client.id).state == DEACTIVATED
        }

        when:"we send the event for deactivation"
        eventStoreDataRepository.deleteAll()
        eContent = """{
            "eventType": "$messageType",
            "clientId": "$cId",
            "type": "DELETION"
        }"""
        msg = new EventMessage(routingKeyPrefix+MessageCreatorImpl.ROUTING_KEY_ELEMENT_CLIENT_CHANGE,eContent,1,Instant.now())
        log.info("publish event: {}", msg)
        eventDispatcher.send(msg)

        then: "the event is sent and all data is deleted"
        new PollingConditions().within(5) {
            !unitRepository.exists(unit.id)
            !domainRepository.exists(domain.id)
            !repository.exists(client.id)
        }

        and: "no outgoing events were produced for the deleted client"
        eventStoreDataRepository.findPendingEvents(Instant.now(), Pageable.unpaged()).size() == 0
    }
}
