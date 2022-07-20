/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman
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
package org.veo.jobs

import static org.veo.core.usecase.unit.CreateDemoUnitUseCase.InputData

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.VeoSpringSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.usecase.unit.CreateDemoUnitUseCase
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.service.DefaultDomainCreator

import spock.lang.AutoCleanup

/**
 * Tests the job that resets the demo unit in regular intervals.
 *
 * Uses an account only for setup - the actual job must run with its own (system) credentials
 * because no user will be logged in when the background task runs.
 */
class ResetDemoUnitJobITSpec extends VeoSpringSpec {

    public static final String MODIFIED_PROCESS_NAME = "Old Modified Process"
    public static final String ASSET_NAME = "Shiny New Asset"

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private CreateDemoUnitUseCase createDemoUnitUseCase

    @Autowired
    private ResetDemoUnitJob job

    @Autowired
    DefaultDomainCreator defaultDomainCreator

    @AutoCleanup('revokeUser')
    UserSwitcher userSwitcher

    @Override
    def setup() {
        userSwitcher = new UserSwitcher()
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_UUID)
    }

    def "Reset demo unit to defaults"() {
        given: "an existing demo unit"
        def clientId = UUID.randomUUID().toString()
        userSwitcher.switchToUser("testuser", clientId)

        def client = createClient(clientId)
        def unit = createDemoUnit(client)

        when: "the demo unit is modified"
        def unmodifiedDemoUnitAssetCount = assetDataRepository.count()
        txTemplate.execute {
            assetDataRepository.save(
                    newAsset(unit, { name = ASSET_NAME }))
            def process = findByUnit(processDataRepository, unit).first()
            process.setName(MODIFIED_PROCESS_NAME)
        }
        userSwitcher.revokeUser()

        then: 'the changes are persisted'
        with(unit) {
            it.name == 'Demo'
        }
        unitRepository.findByClient(client).size() == 1
        assetDataRepository.count() == unmodifiedDemoUnitAssetCount + 1
        with(findByUnit(processDataRepository, unit)*.name) {
            MODIFIED_PROCESS_NAME in it
        }
        with(findByUnit(assetDataRepository, unit)*.name) {
            ASSET_NAME in it
        }

        when: "the unit is reset"
        job.resetAllDemoUnits()

        then: "the unit is reset to defaults"
        unitRepository.findByClient(client).size() == 1
        def resetUnit = unitRepository.findByClient(client).first()
        resetUnit != null
        assetDataRepository.count() == unmodifiedDemoUnitAssetCount
        with(resetUnit) {
            it.name == 'Demo'
        }
        with(findByUnit(processDataRepository, unit)*.name) {
            !(MODIFIED_PROCESS_NAME in it)
        }
        with(findByUnit(assetDataRepository, unit)*.name) {
            !(ASSET_NAME in it)
        }
    }

    def "Demo unit is migrated on reset"() {
        given: "an existing demo unit"
        def clientId = UUID.randomUUID().toString()
        userSwitcher.switchToUser("testuser", clientId)
        def client = createClient(clientId)
        def unit = createDemoUnit(client)

        expect: "some demo processes to have processing details"
        executeInTransaction {
            findByUnit(processDataRepository, unit)
                    .collectMany { it.customAspects }
                    .any { it.type == "process_processingDetails" }
        }

        when: "removing processing details from the element type definition"
        executeInTransaction {
            domainDataRepository.findAllByDomainTemplateId(DSGVO_DOMAINTEMPLATE_UUID).first().with{
                getElementTypeDefinition("process").customAspects.remove("process_processingDetails")
                domainDataRepository.save(it)
            }
        }

        and: "resetting the demo unit"
        userSwitcher.revokeUser()
        job.resetAllDemoUnits()
        def resetUnit = unitRepository.findByClient(client).first()

        then: "no demo processes have processing details"
        executeInTransaction {
            !findByUnit(processDataRepository, resetUnit)
                    .collectMany { it.customAspects }
                    .any { it.type == "process_processingDetails" }
        }
    }

    def "Reset demo unit for client with no demo unit"() {
        given: "a new client"
        def clientId = UUID.randomUUID().toString()
        userSwitcher.switchToUser("testuser", clientId)
        def client = createClient(clientId)
        userSwitcher.revokeUser()

        expect: "the client has no demo unit"
        unitRepository.findByClient(client).size() == 0

        when: "The demo unit is reset"
        job.resetAllDemoUnits()

        then: "A new demo unit was created"
        unitRepository.findByClient(client).size() == 1
    }

    def "Reset demo unit for client with multiple demo units"() {
        given: "two clients, one has two demo units"
        def clientId1 = UUID.randomUUID().toString()
        userSwitcher.switchToUser("testuser1", clientId1)
        def client1 = createClient(clientId1)

        txTemplate.execute {
            // create the first demo unit for client1:
            createDemoUnitUseCase.execute(new InputData(client1.id))
        }

        def client1ModifiedDemoUnit = txTemplate.execute {
            // create the second demo unit for client1, with modifications:
            def client1ModifiedDemoUnit = createDemoUnitUseCase.execute(new InputData(client1.id)).unit

            findByUnit(processDataRepository, client1ModifiedDemoUnit)

            def process = findByUnit(processDataRepository, client1ModifiedDemoUnit)
                    .first()
            process.setName(MODIFIED_PROCESS_NAME)
            client1ModifiedDemoUnit
        }
        userSwitcher.revokeUser()

        def clientId2 = UUID.randomUUID().toString()
        userSwitcher.switchToUser("testuser2", clientId2)
        def client2 = createClient(clientId2)

        def client2ModifiedDemoUnit = txTemplate.execute {
            // create one unit for client2, with modifications:
            def client2ModifiedDemoUnit = createDemoUnitUseCase.execute(new InputData(client2.id)).unit
            def process = findByUnit(processDataRepository, client2ModifiedDemoUnit)
                    .first()
            process.setName(MODIFIED_PROCESS_NAME)
            client2ModifiedDemoUnit
        }
        userSwitcher.revokeUser()

        expect:
        unitRepository.findByClient(client1).size() == 2
        unitRepository.findByClient(client2).size() == 1

        with(findByUnit(processDataRepository, client1ModifiedDemoUnit)*.name) {
            MODIFIED_PROCESS_NAME in it
        }
        with(findByUnit(processDataRepository, client2ModifiedDemoUnit)*.name) {
            MODIFIED_PROCESS_NAME in it
        }

        when: "the demo units are reset"
        job.resetAllDemoUnits()

        then: "the first client was skipped"
        unitRepository.findByClient(client1).size() == 2
        with(findByUnit(processDataRepository, client1ModifiedDemoUnit)*.name) {
            MODIFIED_PROCESS_NAME in it
        }

        and: "the second client's demo unit was reset"
        unitRepository.findByClient(client2).size() == 1
        unitRepository.findById(client2ModifiedDemoUnit.getId()).isEmpty()
        with(findByUnit(processDataRepository, client2ModifiedDemoUnit)*.name) {
            !(MODIFIED_PROCESS_NAME in it)
        }
    }

    Client createClient(String clientId) {
        txTemplate.execute {
            def client = newClient {
                id = Key.uuidFrom(clientId)
            }
            defaultDomainCreator.addDefaultDomains(client)
            clientRepository.save(client)
            client
        }
    }

    Unit createDemoUnit(client) {
        def unit = txTemplate.execute {
            createDemoUnitUseCase.execute(new InputData(client.id)).unit
        }
        unit
    }
}
