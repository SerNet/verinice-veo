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

import static org.veo.core.usecase.unit.CreateDemoUnitUseCase.*

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoSpringSpec
import org.veo.core.entity.Client
import org.veo.core.usecase.unit.CreateDemoUnitUseCase
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

@WithUserDetails("user@domain.example")
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

    def "Reset demo unit to defaults"() {
        given: "an existing demo unit"
        def client = createClient()
        def unit = txTemplate.execute {
            createDemoUnitUseCase.execute(new InputData(client.id)).unit
        }

        when: "the demo unit is modified"
        def unmodifiedDemoUnitAssetCount = assetDataRepository.count()
        txTemplate.execute{
            assetDataRepository.save(
                    newAsset(unit, {name = ASSET_NAME }))
            def process = processDataRepository
                    .findByUnits([unit.getIdAsString()] as Set)
                    .first()
            process.setName(MODIFIED_PROCESS_NAME)
        }

        then: 'the changes are persisted'
        with(unit) {
            it.name == 'Demo'
        }
        unitRepository.findByClient(client).size() == 1
        assetDataRepository.count() == unmodifiedDemoUnitAssetCount + 1
        with (processDataRepository.findByUnits([unit.getIdAsString()] as Set)*.name) {
            MODIFIED_PROCESS_NAME in it
        }
        with(assetDataRepository.findByUnits([unit.getIdAsString()] as Set)*.name) {
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
        with(processDataRepository.findByUnits([unit.getIdAsString()] as Set)*.name) {
            !(MODIFIED_PROCESS_NAME in it)
        }
        with(assetDataRepository.findByUnits([unit.getIdAsString()] as Set)*.name) {
            !(ASSET_NAME in it)
        }
    }

    def "Reset demo unit for client with no demo unit"() {
        given: "a new client"
        def client = createClient()

        expect: "the client has no demo unit"
        unitRepository.findByClient(client).size() == 0

        when: "The demo unit is reset"
        job.resetAllDemoUnits()

        then: "A new demo unit was created"
        unitRepository.findByClient(client).size() == 1
    }

    def "Reset demo unit for client with multiple demo units"() {
        given: "two clients, one has two demo units"
        def client1 = createClient()
        def client2 = createClient()

        txTemplate.execute {
            // create the first demo unit for client1:
            createDemoUnitUseCase.execute(new InputData(client1.id))
        }

        def client1ModifiedDemoUnit = txTemplate.execute {
            // create the second demo unit for client1, with modifications:
            def client1ModifiedDemoUnit = createDemoUnitUseCase.execute(new InputData(client1.id)).unit
            def process = processDataRepository
                    .findByUnits([
                        client1ModifiedDemoUnit.getIdAsString()
                    ] as Set)
                    .first()
            process.setName(MODIFIED_PROCESS_NAME)
            client1ModifiedDemoUnit
        }

        def client2ModifiedDemoUnit = txTemplate.execute {
            // create one unit for client2, with modifications:
            def client2ModifiedDemoUnit = createDemoUnitUseCase.execute(new InputData(client2.id)).unit
            def process = processDataRepository
                    .findByUnits([
                        client2ModifiedDemoUnit.getIdAsString()
                    ] as Set)
                    .first()
            process.setName(MODIFIED_PROCESS_NAME)
            client2ModifiedDemoUnit
        }

        expect:
        unitRepository.findByClient(client1).size() == 2
        unitRepository.findByClient(client2).size() == 1

        with (processDataRepository.findByUnits([
            client1ModifiedDemoUnit.getIdAsString()
        ] as Set)*.name) {
            MODIFIED_PROCESS_NAME in it
        }
        with (processDataRepository.findByUnits([
            client2ModifiedDemoUnit.getIdAsString()
        ] as Set)*.name) {
            MODIFIED_PROCESS_NAME in it
        }

        when: "the demo units are reset"
        job.resetAllDemoUnits()

        then: "the first client was skipped"
        unitRepository.findByClient(client1).size() == 2
        with (processDataRepository.findByUnits([
            client1ModifiedDemoUnit.getIdAsString()
        ] as Set)*.name) {
            MODIFIED_PROCESS_NAME in it
        }

        and: "the second client's demo unit was reset"
        unitRepository.findByClient(client2).size() == 1
        unitRepository.findById(client2ModifiedDemoUnit.getId()).isEmpty()
        with (processDataRepository.findByUnits([
            client2ModifiedDemoUnit.getIdAsString()
        ] as Set)*.name) {
            !(MODIFIED_PROCESS_NAME in it)
        }
    }

    Client createClient() {
        txTemplate.execute {
            def client = newClient()
            domainTemplateService.createDefaultDomains(client)
            clientRepository.save(client)
            client
        }
    }
}
