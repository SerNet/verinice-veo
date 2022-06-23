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
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.service.EntitySchemaService
import org.veo.core.usecase.unit.CreateDemoUnitUseCase
import org.veo.core.usecase.unit.CreateDemoUnitUseCase.InputData
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.ReferenceSerializationModule

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
        Domain domain
        def client = executeInTransaction {
            createClient().tap {
                domain = createTestDomain(it, DSGVO_DOMAINTEMPLATE_UUID)
            }
        }
        when: 'executing the CreateDemoUnitUseCase'
        def unit = runUseCase(client)
        then: 'the demo unit is created'
        unit != null
        with(unit) {
            it.name == 'Demo'
            it.domains*.name == ['DS-GVO']
        }
        unitRepository.findByClient(client).size() == 1
        when: 'loading the controls'
        def controls = txTemplate.execute{
            controlDataRepository.findByUnits([unit.idAsString] as Set).each {
                //initialize lazy associations
                it.riskValuesAspects.each {
                    it.values.each { k,v->
                        v.implementationStatus
                    }
                }
                it.domains*.name
            }
        }
        then: 'the controls are returned'
        controls.size() == 1

        with(controls.first()) {
            it.domains*.name == ['DS-GVO']
            riskValuesAspects.size() == 1
            with(riskValuesAspects.first()) {
                domain.name == 'DS-GVO'
                values.size() == 1
                with(values.entrySet().first()) {
                    key.idRef == 'DSRA'
                    value.implementationStatus.ordinalValue == 2
                }
            }
        }
        when: 'loading the scenarios'
        def scenarios = txTemplate.execute{
            scenarioDataRepository.findByUnits([unit.idAsString] as Set).each {
                //initialize lazy associations
                it.riskValuesAspects.each {
                    it.potentialProbability.values()
                }
            }
        }
        then: 'the scenarios are returned'
        scenarios.size() == 1

        with(scenarios.first()) {
            it.domains*.name == ['DS-GVO']
            riskValuesAspects.size() == 1
            with(riskValuesAspects.first()) {
                domain.name == 'DS-GVO'
                potentialProbability.size() == 1
                with(potentialProbability.entrySet().first()) {
                    key.idRef == 'DSRA'
                    value.potentialProbability.idRef == 1
                }
            }
        }
        when: 'loading the processes'
        def processes = txTemplate.execute{
            processDataRepository.findByUnits([unit.idAsString] as Set).each {
                //initialize lazy associations
                it.riskValuesAspects.each {
                    it.values.each { k,v->
                        v.potentialImpacts.values()
                    }
                }
                it.links.target*.name
                it.risks.each {
                    it.getRiskDefinitions(domain)
                    it.mitigation?.id
                    it.scenario?.id
                    it.riskOwner?.id
                }
            }
        }
        then: 'the processes are returned'
        processes.size() == 1
        with(processes.first()) {
            it.domains*.name == ['DS-GVO']
            it.links.find{it.type == "process_PIAOOtherOrganisationsInvolved"}.target.name == "Data GmbH"
            decisionResultsAspects.size() == 1
            with(decisionResultsAspects.first()) {
                domain.name == 'DS-GVO'
                results.keySet()*.keyRef ==~ ['piaMandatory']
                results.values().first().value == true
            }
        }

        when: 'loading the persons'
        def persons = txTemplate.execute{
            personDataRepository.findByUnits([unit.idAsString] as Set).tap{
                it*.parts*.name
                it*.links*.target*.name
            }
        }
        then: 'the persons are returned'
        persons.size() == 3
        with(persons.find{it.name == "Personal"}) {
            it.domains*.name == ['DS-GVO']
            it.parts*.name ==~ ["Jürgen Toast", "Hans Meiser"]
        }
        with(persons.find{it.name == "Jürgen Toast"}) {
            it.links.find{it.type == "person_favoriteScope"}.target.name == "Data GmbH"
        }

        with(processes.first()) {
            riskValuesAspects.size() == 1
            with(riskValuesAspects.first()) {
                domain.name == 'DS-GVO'
                values.size() == 1
                with(values.entrySet().first()) {
                    key.idRef == 'DSRA'
                    with(value.potentialImpacts) {
                        size() == 4
                        entrySet().find{
                            it.key.idRef == 'A'
                        }.value.idRef == 2
                    }
                }
            }
            risks.size() == 1
            with(risks.first()) { risk->
                it.domains*.name == ['DS-GVO']
                scenario == scenarios.first()
                mitigation == controls.first()
                getRiskDefinitions(domain).size() == 1
                with(getRiskDefinitions(domain).first()) {
                    idRef == 'DSRA'
                    with(risk.getProbabilityProvider(it, domain).probability) {
                        verifyAll {
                            potentialProbability.idRef == 1
                            specificProbability.idRef == 2
                            effectiveProbability.idRef == 2
                        }
                    }
                    with(risk.getImpactProvider(it, domain)) {
                        availableCategories.size() == 4
                        def integrity = availableCategories.find{
                            it.idRef == 'I'
                        }

                        verifyAll {
                            getPotentialImpact(integrity).idRef == 1
                            getSpecificImpact(integrity).idRef == 0
                            getEffectiveImpact(integrity).idRef == 0
                        }
                    }
                    with(risk.getRiskProvider(it, domain)) {
                        availableCategories.size() == 4
                        def confidentiality = availableCategories.find{
                            it.idRef == 'C'
                        }
                        verifyAll {
                            getInherentRisk(confidentiality).idRef == 2
                            getUserDefinedResidualRisk(confidentiality).idRef == 2
                        }
                    }
                }
            }
        }
        when: 'loading the scopes'
        def scopes = txTemplate.execute{
            scopeDataRepository.findByUnits([unit.idAsString] as Set).tap{
                //initialize lazy associations
                it*.links*.target*.name
                it*.riskValuesAspects*.each {
                    it.riskDefinitionRef
                }
            }
        }
        then: 'the scope is returned'
        scopes.size() == 1
        with(scopes.first()) {
            it.domains*.name == ['DS-GVO']
            it.name == "Data GmbH"
            it.designator.startsWith('DMO-')
            it.members.size() == 1
            it.members.find{
                it.name == 'Durchführung Befragungen'
            }
            it.links.size() == 2
            with(it.links.find{
                it.type == 'scope_informationSecurityOfficer'
            }) {
                target.name == 'Jürgen Toast'
            }
            riskValuesAspects.size() == 1
            with(riskValuesAspects.first()) {
                domain.name == 'DS-GVO'
                riskDefinitionRef.idRef == 'DSRA'
            }
        }


        when: 'loading the demo unit elements and converting them to JSON'
        def demoElementsForUnitAsDtos = executeInTransaction{
            [
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
            registerModule(new ReferenceSerializationModule())
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
        newDomain(client)
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
        newDomain(client) {
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
            defaultDomainCreator.addDefaultDomains(client)
            return clientRepository.save(client)
        }
    }

    private JsonSchema getSchema(Client client, String type) {
        def schemaString = entitySchemaService.findSchema(type, client.domains)
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909).getSchema(schemaString)
    }
}