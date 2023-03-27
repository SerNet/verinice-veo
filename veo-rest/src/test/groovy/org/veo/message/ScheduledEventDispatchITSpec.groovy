/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Alexander Koderman.
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
import static org.veo.core.usecase.unit.CreateDemoUnitUseCase.InputData
import static org.veo.rest.VeoRestConfiguration.PROFILE_BACKGROUND_TASKS

import java.util.concurrent.CountDownLatch

import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.GenericContainer

import org.veo.core.VeoSpringSpec
import org.veo.core.entity.Client
import org.veo.core.usecase.unit.CreateDemoUnitUseCase
import org.veo.core.usecase.unit.DeleteUnitUseCase
import org.veo.core.usecase.unit.GetUnitsUseCase
import org.veo.jobs.MessageDeletionJob
import org.veo.jobs.MessagingJob
import org.veo.jobs.UserSwitcher
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.jpa.StoredEventDataRepository
import org.veo.persistence.entity.jpa.StoredEventData

import groovy.util.logging.Slf4j
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.util.concurrent.PollingConditions

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
@EnableScheduling
class ScheduledEventDispatchITSpec extends VeoSpringSpec {

    public static final int NUM_EVENTS = 30000
    public static final int DEMO_NUM_EVENTS = 81

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private CreateDemoUnitUseCase createDemoUnitUseCase

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

    @AutoCleanup('revokeUser')
    UserSwitcher userSwitcher

    @Value('${veo.message.queues.veo}')
    String testQueue

    Client client

    @Autowired
    GetUnitsUseCase getUnitsUseCase

    @Autowired
    DeleteUnitUseCase deleteUnitUseCase

    def setupSpec() {
        rabbit = TestContainersUtil.startRabbitMqContainer()
    }

    def setup() {
        userSwitcher = new UserSwitcher()
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_UUID)
        eventSubscriber.receivedEvents.clear()
    }

    /**
     * This test fails if the {@link EventDispatcher} and/or
     * {@link org.veo.jobs.MessageDeletionJob.EventDeleter} get saturated with too many
     * messages. If StoredEvents are not ACKed and removed within the lockTime (default: 20s)
     * they will be sent again, causing redundant messages to appear in the receiver.
     * <p>
     * Limiting factors that can cause this are:
     * <ul>
     *  <li> not enough scheduler threads
     *  <li> max lock time too short
     *  <li> message broker too slow to handle message amount
     * </ul>
     *
     * Also, if the isolation level is not high enough while selecting events to be sent,
     * the {@link org.veo.jobs.EventRetriever} can suffer phantom reads which will also cause
     * redundant messages to be sent for the same stored event.
     */
    def "scheduled task sends messages for all stored events"() {
        given:
        def confirmationLatch = new CountDownLatch(NUM_EVENTS)
        eventDispatcher.addAckCallback { confirmationLatch.countDown() }

        when: "storing outgoing messages to be sent"
        // run in transaction so that all events are in the database when the first dispatcher gets scheduled:
        def events = executeInTransaction {
            (1..NUM_EVENTS)
                    .collect { new StoredEventData() }
                    .each { it.routingKey = routingKeyPrefix + "veo.testmessage" }
                    .each { storedEventRepository.save(it) }
        }

        then: "the table should have been cleared by the deletion job"
        new PollingConditions().within(60) {
            storedEventRepository.findAll().size() == 0
        }

        and: "confirmations have been received for sent messages"
        confirmationLatch.await(60, SECONDS)

        and: "all messages should have been received with no duplicates"
        eventSubscriber.receivedEvents.size() == NUM_EVENTS
        eventSubscriber.receivedEvents ==~ events.collect { EventMessage.from(it) }
    }

    @WithUserDetails("user@domain.example")
    def "Scheduled task sends events when profile is applied"() {
        given: "An event receiver"
        def confirmationLatch = new CountDownLatch(DEMO_NUM_EVENTS)
        eventDispatcher.addAckCallback { confirmationLatch.countDown() }

        when: "The client's demo unit is created"
        executeInTransaction {
            client = newClient()
            def dsgvoTestDomain = domainTemplateService.createDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
            client.addToDomains(dsgvoTestDomain)
            client = clientRepository.save(client)
            createDemoUnitUseCase.execute(new InputData(client.getId()))
        }

        and: "the event table has been completely cleared by the deletion job"
        new PollingConditions().within(10) {
            storedEventRepository.findAll().size() == 0
        }

        then: "confirmations have been received for sent messages"
        confirmationLatch.await(10, SECONDS)

        and: "all messages were received with no duplicates"
        eventSubscriber.receivedEvents.size() == DEMO_NUM_EVENTS
        !eventSubscriber.hasReceivedDuplicates
    }

    def cleanup() {
        def purgeCount = rabbitAdmin.purgeQueue(testQueue)
        if (purgeCount > 0)
            log.info("Test cleanup: purged {} remaining messages in test queue.", purgeCount)
        eventStoreDataRepository.deleteAll()
    }
}
