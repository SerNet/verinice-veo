/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion

import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer
import org.veo.core.entity.Client
import org.veo.core.entity.Unit
import org.veo.core.service.EntitySchemaService
import org.veo.core.usecase.unit.CreateDemoUnitUseCase
import org.veo.core.usecase.unit.CreateDemoUnitUseCase.InputData
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

@WithUserDetails("user@domain.example")
class CreateDemoUnitUseCaseITSpec extends VeoSpringSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private CreateDemoUnitUseCase useCase

    @Autowired
    EntityToDtoTransformer entityToDtoTransformer

    @Autowired
    EntitySchemaService entitySchemaService


    def "create a demo unit for a client"() {
        given: 'a client'
        def client = createClient()
        when: 'executing the CreateDemoUnitUseCase'
        def unit = runUseCase(client)
        then: 'the demo unit is created'
        unit != null
        with(unit) {
            it.name == 'Demo'
            it.domains*.name == ['DS-GVO']
        }
        unitRepository.findByClient(client).size() == 1
        when: 'loading the processes'
        def processes = txTemplate.execute{
            def result = processDataRepository.findByUnits([unit.idAsString] as Set)
            //initialize lazy associations
            result*.links*.target*.name
            result
        }
        then: 'the processes are returned'
        processes.size() == 6
        and: 'the processes have the required data'
        with(processes.find{it.name == 'Durchführung Befragungen'}) {
            it.designator.startsWith('DMO-')
            it.links.size() == 12
            with(it.links.findAll{it.type == 'process_controller'}) {
                it.size() == 2
                it*.target*.name.toSorted() == ['FED AG', 'MeD GmbH']
            }
        }

        when: 'loading the scopes'
        def scopes = scopeDataRepository.findByUnits([unit.idAsString] as Set)
        then: 'the scopes are returned'
        scopes.size() == 5
        and: 'the scopes have the required data'
        with(scopes.find{it.name == 'Data GmbH'}) {
            it.designator.startsWith('DMO-')
            it.members.size() == 6
            it.members.find{it.name == 'Durchführung Befragungen'}
        }
        when: 'loading the demo unit elements and converting them to JSON'
        def demoElementsForUnitAsDtos = executeInTransaction{
            def demoElementsForUnit = [
                assetDataRepository,
                controlDataRepository,
                documentDataRepository,
                incidentDataRepository,
                personDataRepository,
                processDataRepository,
                scenarioDataRepository,
                scopeDataRepository
            ].collectMany {
                it.findByUnits([unit.idAsString] as Set).collect {
                    entityToDtoTransformer.transform2Dto(it)
                }
            }
        }
        then: 'the demo unit elements conform to the object schemas'
        ObjectMapper om = new ObjectMapper().tap{
            setSerializationInclusion(Include.NON_NULL)
        }
        demoElementsForUnitAsDtos.each { dto->
            def schema = getSchema(client, dto.type)
            def validationMessages = schema.validate(om.valueToTree(dto))
            assert validationMessages.empty
        }
    }

    def "create multiple demo units for a client"() {
        given:
        def client = createClient()
        when:
        def unit1 = runUseCase(client)
        def unit2 = runUseCase(client)
        def unit3 = runUseCase(client)
        then:
        unit1 != null
        unit2 != null
        unit3 != null
        with(unit1) {
            it.name == 'Demo'
        }
        with(unit2) {
            it.name == 'Demo'
        }
        with(unit3) {
            it.name == 'Demo'
        }
        unitRepository.findByClient(client).size() == 3
    }

    def "create demo units for multiple clients"() {
        given:
        def client1 = createClient()
        def client2 = createClient()
        def client3 = createClient()
        when:
        def unit1 = runUseCase(client1)
        def unit2 = runUseCase(client2)
        def unit3 = runUseCase(client3)
        then:
        unit1 != null
        unit2 != null
        unit3 != null
        with(unit1) {
            it.name == 'Demo'
        }
        with(unit2) {
            it.name == 'Demo'
        }
        with(unit3) {
            it.name == 'Demo'
        }
        unitRepository.findByClient(client1).size() == 1
        unitRepository.findByClient(client2).size() == 1
        unitRepository.findByClient(client3).size() == 1
    }

    def "create demo unit for a client with an unknown domain"() {
        given:
        def client = createClient()
        def domain2 = newDomain(client)
        client = clientRepository.save(client)
        when:
        def unit = runUseCase(client)
        then:
        unit != null
        with(unit) {
            it.name == 'Demo'
        }
    }

    def "create demo unit for a client with an inactive domain"() {
        given:
        def client = createClient()
        client.domains.each {
            it.active = false
        }
        client = clientRepository.save(client)
        when:
        def unit = runUseCase(client)
        then:
        unit != null
        with(unit) {
            it.name == 'Demo'
        }
        processDataRepository.findByUnits([unit.idAsString] as Set).empty
    }

    def "create demo units for a client with a domain with an unknown template"() {
        given:
        def client = createClient()
        def template = domainTemplateDataRepository.save(newDomainTemplate())
        def domain2 = newDomain(client) {
            it.domainTemplate = template
        }
        client = clientRepository.save(client)
        when:
        def unit = runUseCase(client)
        then:
        unit != null
        with(unit) {
            it.name == 'Demo'
        }
    }

    Unit runUseCase(Client client) {
        executeInTransaction {
            useCase.execute(new InputData(client.id)).unit
        }
    }

    Client createClient() {
        executeInTransaction {
            def client = newClient()
            domainTemplateService.createDefaultDomains(client)
            return clientRepository.save(client)
        }
    }

    private JsonSchema getSchema(Client client, String type) {
        def schemaString = entitySchemaService.findSchema(type, client.domains)
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909).getSchema(schemaString)
    }
}