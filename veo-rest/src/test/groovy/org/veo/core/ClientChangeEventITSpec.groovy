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

import static org.veo.core.entity.ClientState.ACTIVATED
import static org.veo.core.entity.ClientState.CREATED
import static org.veo.core.entity.ClientState.DEACTIVATED
import static org.veo.core.entity.ClientState.DELETED
import static org.veo.core.events.MessageCreatorImpl.EVENT_TYPE_CLIENT_CHANGE
import static org.veo.rest.VeoRestConfiguration.PROFILE_BACKGROUND_TASKS

import java.time.Instant

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.GenericContainer

import org.veo.core.entity.Client
import org.veo.core.entity.ClientState
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.message.EventDispatcher
import org.veo.message.EventMessage
import org.veo.message.RabbitMQSenderConfiguration
import org.veo.message.TestContainersUtil
import org.veo.message.TestEventSubscriber
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.DomainTemplateRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

import groovy.util.logging.Slf4j
import spock.lang.AutoCleanup
import spock.lang.Shared

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

    @Autowired
    EventDispatcher eventDispatcher

    @Autowired
    private ClientRepositoryImpl repository
    @Autowired
    private UnitRepositoryImpl unitRepository
    @Autowired
    private DomainRepositoryImpl domainRepository
    @Autowired
    private DomainTemplateRepositoryImpl domainTemplateRepository

    @Value('${veo.message.routing-key-prefix}')
    String routingKeyPrefix

    @Value('${veo.message.exchanges.veo-subscriptions}')
    String exchange

    String messageType = EVENT_TYPE_CLIENT_CHANGE
    String routingKey

    def setupSpec() {
        rabbit = TestContainersUtil.startRabbitMqContainer()
    }

    def setup() {
        routingKey = routingKeyPrefix + EVENT_TYPE_CLIENT_CHANGE
        createTestDomainTemplate(DSGVO_TEST_DOMAIN_TEMPLATE_ID)
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_UUID)
        createTestDomainTemplate(TEST_DOMAIN_TEMPLATE_ID)
    }

    def "publish create client event"() {
        when: "a create client event is send"
        def cId = Key.newUuid()
        def clientName = "new client"

        eventDispatcher.send(exchange, new EventMessage(routingKey, """{
            "eventType": "$messageType",
            "clientId": "${cId.uuidValue()}",
            "type": "CREATION",
            "name": "${clientName}",
            "domainProducts": {
                "DS-GVO" : ["Beispielorganisation"],
                "test-domain": []
            }
        }""", 1, Instant.now()))

        then: "the client is created, activated, the initial domains exist"
        defaultPolling.eventually {
            repository.exists(cId)
            with(repository.findById(cId).get()) {
                domains.size() == 2
                state == ACTIVATED
                name == clientName
            }
        }

        and: "no units are created"
        executeInTransaction {
            unitDataRepository.findByClientId(cId.value())
        }.size() == 0

        when:"we send a change -> add a domain"
        eventDispatcher.send(exchange, new EventMessage(routingKey, """{
            "eventType": "$messageType",
            "clientId": "${cId.uuidValue()}",
            "type": "MODIFICATION",
            "domainProducts": {
                "DSGVO-test" : []
            }
        }""",2,Instant.now()))

        then: "the event is sent and the domain is added"
        defaultPolling.eventually {

            def client = executeInTransaction {
                repository.findById(cId).get().tap {
                    domains.collect { it.profiles.size() }
                    domains*.profiles.collect{ it.name }
                }
            }
            with(client.domains.find { it.name == 'DS-GVO' }) {
                profiles.size() == 1
                profiles[0].name ==  'Beispielorganisation'
            }
            client.domains.size() == 3
            client.domains*.name ==~ [
                'DS-GVO',
                'DSGVO-test',
                'test-domain'
            ]
        }

        when: "we add a profile to a template and add this profile"
        def dt = domainTemplateDataRepository
                .findByIdWithProfilesAndRiskDefinitions(UUID.fromString(DSGVO_TEST_DOMAIN_TEMPLATE_ID)).get()

        dt.profiles.add(newProfile(dt) {
            name = 'the new profile'
            language = 'DE_de'
        })
        domainTemplateDataRepository.save(dt)
        eventDispatcher.send(exchange, new EventMessage(routingKey, """{
            "eventType": "$messageType",
            "clientId": "${cId.uuidValue()}",
            "type": "MODIFICATION",
            "domainProducts": {
                "DSGVO-test" : ["the new profile"]
            }
        }""",3,Instant.now()))

        then: "the event is sent and the domain is added"
        defaultPolling.eventually {

            def client = executeInTransaction {
                repository.findById(cId).get().tap {
                    domains.collect { it.profiles.size() }
                    domains*.profiles.collect{ it.name }
                }
            }
            with(client.domains.find { it.name == 'DSGVO-test' }) {
                profiles.size() == 1
                profiles[0].name ==  'the new profile'
            }
            client.domains.size() == 3
            client.domains*.name ==~ [
                'DS-GVO',
                'DSGVO-test',
                'test-domain'
            ]
        }

        when:"we send a change -> add a non existing domain/profiles"
        eventDispatcher.send(exchange, new EventMessage(routingKey, """{
            "eventType": "$messageType",
            "clientId": "${cId.uuidValue()}",
            "type": "MODIFICATION",
            "domainProducts": {
                "DSGVO-test1" : ["dontExist1","dontExist2"]
            }
        }""",4,Instant.now()))

        then: "the event is sent and no domain is added"
        defaultPolling.eventually {

            def client = executeInTransaction {
                repository.findById(cId).get().tap {
                    domains.collect { it.profiles.size() }
                    domains*.profiles.collect{ it.name }
                }
            }
            with(client.domains.find { it.name == 'DS-GVO' }) {
                profiles.size() == 1
                profiles[0].name ==  'Beispielorganisation'
            }
            client.domains.size() == 3
            client.domains*.name ==~ [
                'DS-GVO',
                'DSGVO-test',
                'test-domain'
            ]
        }

        when: "we delete the client with three domains"
        eventDispatcher.send(exchange, new EventMessage(routingKey, """{
            "eventType": "$messageType",
            "clientId": "${cId.uuidValue()}",
            "type": "DEACTIVATION"
        }""",5,Instant.now()))

        eventDispatcher.send(exchange, new EventMessage(routingKey, """{
            "eventType": "$messageType",
            "clientId": "${cId.uuidValue()}",
            "type": "DELETION"
        }""",6,Instant.now()))

        then: "the event is sent and the client and the domains are deleted"
        defaultPolling.within(10) {
            !repository.exists(cId)
        }
    }

    def "publish the deletion event"() {
        given: "a client and an unit"
        Client client = repository.save(newClient {
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
        eventDispatcher.send(exchange, msg)

        then: "the event is sent"
        defaultPolling.eventually {
            !unitRepository.exists(unit.id)
            !repository.exists(client.id)
        }
    }

    def "sync the maxUnits attribute"() {
        given: "a client and two units"
        Client client = repository.save(newClient {
            state = ACTIVATED
        })

        def cId = client.getIdAsString()
        unitRepository.save(newUnit(client))
        unitRepository.save(newUnit(client))

        when:"we send the event"
        eventDispatcher.send(exchange, new EventMessage(routingKey, """{
            "eventType": "$messageType",
            "clientId": "$cId",
            "type": "MODIFICATION",
            "maxUnits": 5
        }""", 1, Instant.now()))

        then: "the event is sent"
        defaultPolling.eventually {
            repository.findById(Key.uuidFrom(cId)).get().maxUnits == 5
        }

        when:"we send the next change"
        eventDispatcher.send(exchange, new EventMessage(routingKey, """{
            "eventType": "$messageType",
            "clientId": "$cId",
            "type": "MODIFICATION",
            "maxUnits": 15
        }""", 2, Instant.now()))

        then: "the event is sent and the maxUnits is updated"
        defaultPolling.eventually {
            repository.findById(Key.uuidFrom(cId)).get().maxUnits == 15
        }

        when:"we send the next change -> less units than exiting"
        eventDispatcher.send(exchange, new EventMessage(routingKey, """{
            "eventType": "$messageType",
            "clientId": "$cId",
            "type": "MODIFICATION",
            "maxUnits": 1
        }""",3,Instant.now()))

        then: "the event is sent and the maxUnits is updated"
        defaultPolling.eventually {
            repository.findById(Key.uuidFrom(cId)).get().maxUnits == 1
        }
    }

    def"publish wrong event type for client state"(ClientState startState, String eventType) {
        given: "a client an a unit"
        Client client = repository.save(newClient {
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
        def msg = new EventMessage(routingKeyPrefix+ EVENT_TYPE_CLIENT_CHANGE,eContent,1,Instant.now())
        log.info("publish event: {}", msg)
        eventDispatcher.send(exchange, msg)

        then: "the event is sent"
        defaultPolling.eventually {
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
            state = startState
        })
        def cId = client.getIdAsString()

        when:"we send the event"
        def eContent = """{
            "eventType": "$messageType",
            "clientId": "$cId",
            "type": "$eventType"
        }"""
        def msg = new EventMessage(routingKeyPrefix+ EVENT_TYPE_CLIENT_CHANGE,eContent,1,Instant.now())
        log.info("publish event: {}", msg)
        eventDispatcher.send(exchange, msg)

        then: "the event is sent"
        defaultPolling.eventually {
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
        given: "a client and a unit"
        Client client = repository.save(newClient {
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

        def msg = new EventMessage(routingKeyPrefix+ EVENT_TYPE_CLIENT_CHANGE,eContent,1,Instant.now())
        log.info("publish event: {}", msg)
        eventDispatcher.send(exchange, msg)

        then: "the event is sent"
        defaultPolling.eventually {
            repository.getById(client.id).state == ACTIVATED
        }

        when:"we send the event for modification"
        eContent = """{
            "eventType": "$messageType",
            "clientId": "$cId",
            "type": "MODIFICATION"
        }"""
        msg = new EventMessage(routingKeyPrefix + EVENT_TYPE_CLIENT_CHANGE, eContent, 2, Instant.now())
        log.info("publish event: {}", msg)
        eventDispatcher.send(exchange, msg)

        then: "the event is sent"
        defaultPolling.eventually {
            repository.getById(client.id).state == ACTIVATED
        }

        when:"we send the event for deactivation"
        eContent = """{
            "eventType": "$messageType",
            "clientId": "$cId",
            "type": "DEACTIVATION"
        }"""

        msg = new EventMessage(routingKeyPrefix + EVENT_TYPE_CLIENT_CHANGE, eContent, 3, Instant.now())
        log.info("publish event: {}", msg)
        eventDispatcher.send(exchange, msg)

        then: "the event is sent"
        defaultPolling.eventually {
            repository.getById(client.id).state == DEACTIVATED
        }

        when:"we send the event for deactivation"
        eventStoreDataRepository.deleteAll()
        eContent = """{
            "eventType": "$messageType",
            "clientId": "$cId",
            "type": "DELETION"
        }"""
        msg = new EventMessage(routingKeyPrefix + EVENT_TYPE_CLIENT_CHANGE, eContent, 4, Instant.now())
        log.info("publish event: {}", msg)
        eventDispatcher.send(exchange, msg)

        then: "the event is sent and all data is deleted"
        defaultPolling.eventually {
            !unitRepository.exists(unit.id)
            !domainRepository.exists(domain.id)
            !repository.exists(client.id)
        }

        and: "no outgoing events were produced for the deleted client"
        eventStoreDataRepository.findPendingEvents(Instant.now(), Pageable.unpaged()).size() == 0
    }
}
