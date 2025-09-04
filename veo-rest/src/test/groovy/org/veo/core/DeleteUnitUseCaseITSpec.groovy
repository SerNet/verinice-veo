/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade.
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
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

import org.veo.core.entity.Client
import org.veo.core.entity.Unit
import org.veo.core.repository.UnitRepository
import org.veo.core.usecase.UseCase
import org.veo.core.usecase.catalogitem.ApplyProfileIncarnationDescriptionUseCase
import org.veo.core.usecase.catalogitem.GetProfileIncarnationDescriptionUseCase
import org.veo.core.usecase.unit.DeleteUnitUseCase
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.jpa.StoredEventDataRepository
import org.veo.persistence.metrics.DataSourceProxyBeanPostProcessor
import org.veo.rest.security.NoRestrictionAccessRight

import groovy.json.JsonSlurper
import net.ttddyy.dsproxy.QueryCountHolder

@WithUserDetails("user@domain.example")
class DeleteUnitUseCaseITSpec extends AbstractPerformanceITSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private GetProfileIncarnationDescriptionUseCase getProfileIncarnationDescriptionUseCase

    @Autowired
    private ApplyProfileIncarnationDescriptionUseCase applyProfileIncarnationDescriptionUseCase

    @Autowired
    private UnitRepository unitRepository

    @Autowired
    private DeleteUnitUseCase deleteUnitUseCase

    @Autowired
    private StoredEventDataRepository storedEventDataRepository

    @DynamicPropertySource
    static void setRowCount(DynamicPropertyRegistry registry) {
        registry.add("veo.logging.datasource.row_count", { -> true })
    }

    def "delete a unit with example elements"() {
        given: 'a unit with example elements'
        def client = createTestClient()
        def domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        client = clientRepository.getById(client.id)
        def unit = unitRepository.save(newUnit(client))
        executeInTransaction {
            def profileId = domain.profiles.first().id
            def incarnationDescriptions = getProfileIncarnationDescriptionUseCase.execute(
                    new GetProfileIncarnationDescriptionUseCase.InputData(unit.id, domain.id, null, profileId, false), NoRestrictionAccessRight.from(client.idAsString)
                    ).references
            applyProfileIncarnationDescriptionUseCase.execute(
                    new ApplyProfileIncarnationDescriptionUseCase.InputData(unit.id, incarnationDescriptions), NoRestrictionAccessRight.from(client.idAsString))
        }
        QueryCountHolder.clear()
        def rowCountBefore = DataSourceProxyBeanPostProcessor.totalResultSetRowsRead

        when: 'executing the DeleteUnitUseCase'
        runUseCase(unit, NoRestrictionAccessRight.from(client.idAsString))
        def queryCounts = QueryCountHolder.grandTotal

        then: 'query statistics show sensible data'
        verifyAll {
            queryCounts.select == 28
            queryCounts.insert == 1
            queryCounts.update == 1
            queryCounts.delete == 26
            queryCounts.time < 1000
            // 99 is the currently observed count of 84 rows plus an acceptable safety margin
            DataSourceProxyBeanPostProcessor.totalResultSetRowsRead - rowCountBefore <= 99
            storedDeleteEvents.size() == 10
            storedUnitDeleteEvents.size() == 1
        }
    }

    def "delete a large unit"() {
        given: 'a client with many elements'
        def client = createTestClient()
        def testDomain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        def unit = executeInTransaction {
            def unit = unitDataRepository.save( newUnit(client).tap { unit->
                addToDomains(testDomain)
            })
            def documents = documentDataRepository.saveAll((0..99).collect{
                newDocument(unit)
            })
            def incidents = incidentDataRepository.saveAll((0..99).collect{
                newIncident(unit)
            })
            def scenarios = scenarioDataRepository.saveAll((0..99).collect{
                newScenario(unit)
            })
            def persons = personDataRepository.saveAll((0..99).collect{
                newPerson(unit)
            })
            def controls = controlDataRepository.saveAll((0..99).collect{
                newControl(unit)
            })
            def assets = assetDataRepository.saveAll((0..99).collect{ i->
                newAsset(unit).tap {
                    associateWithDomain(testDomain, 'AST_Application', 'NEw')
                    obtainRisk(scenarios[i]).tap {
                        assignDesignator(it)
                        appoint(persons[i])
                        mitigate(controls[i])
                    }
                    implementControl(controls[i]).tap { ci->
                        controls.shuffled().minus(controls[i]).take(10).each {
                            ci.addRequirement(it)
                        }
                        setResponsible(persons[i])
                    }
                }
            })
            def processes = processDataRepository.saveAll((0..99).collect{ i->
                newProcess(unit).tap {
                    associateWithDomain(testDomain, 'PRO_DataProcessing', 'NEw')
                    obtainRisk(scenarios[i]).tap {
                        assignDesignator(it)
                        appoint(persons[i])
                        mitigate(controls[i])
                    }
                    implementControl(controls[i]).tap { ci->
                        controls.shuffled().minus(controls[i]).take(10).each {
                            ci.addRequirement(it)
                        }
                        setResponsible(persons[i])
                    }
                }
            })
            scopeDataRepository.saveAll((0..99).collect{ i->
                newScope(unit) {
                    addMember(assets[i])
                    addMember(documents[i])
                    addMember(incidents[i])
                    addMember(scenarios[i])
                    addMember(processes[i])
                    addMember(persons[i])
                    addMember(controls[i])
                    associateWithDomain(testDomain, 'SCP_ResponsibleBody', 'NEw')
                    obtainRisk(scenarios[i]).tap {
                        assignDesignator(it)
                        appoint(persons[i])
                        mitigate(controls[i])
                    }
                    implementControl(controls[i]).tap { ci->
                        controls.shuffled().minus(controls[i]).take(10).each {
                            ci.addRequirement(it)
                        }
                        setResponsible(persons[i])
                    }
                }
            })
            unit
        }
        QueryCountHolder.clear()
        def rowCountBefore = DataSourceProxyBeanPostProcessor.totalResultSetRowsRead

        when: 'executing the DeleteUnitUseCase'
        runUseCase(unit, NoRestrictionAccessRight.from(client.idAsString))
        def queryCounts = QueryCountHolder.grandTotal

        then: 'query statistics show sensible data'
        verifyAll {
            queryCounts.select == 61
            queryCounts.insert == 37
            queryCounts.update == 1
            queryCounts.delete == 64
            queryCounts.time < 4000
            // 7784 is the currently observed count of 7626 rows plus an acceptable safety margin
            DataSourceProxyBeanPostProcessor.totalResultSetRowsRead - rowCountBefore <= 7784
            storedDeleteEvents.size() == 1100
            storedUnitDeleteEvents.size() == 1
        }
    }

    def getStoredUnitDeleteEvents() {
        storedEventDataRepository.findAll().findAll{
            it.content != null && new JsonSlurper().parseText(it.content).eventType == 'unit_deletion'
        }
    }

    def getStoredDeleteEvents() {
        storedEventDataRepository.findAll().findAll{
            it.content != null && new JsonSlurper().parseText(it.content).type == 'HARD_DELETION'
        }
    }

    def runUseCase(Unit unit, UserAccessRights user) {
        executeInTransaction {
            deleteUnitUseCase.execute(new UseCase.EntityId(unit.id), NoRestrictionAccessRight.from(unit.client.idAsString))
        }
    }

    Client createClient() {
        executeInTransaction {
            def client = newClient()
            defaultDomainCreator.addDefaultDomains(client)
            return clientRepository.save(client)
        }
    }
}