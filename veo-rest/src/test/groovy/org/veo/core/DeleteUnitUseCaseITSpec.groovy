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
import org.veo.core.usecase.catalogitem.ApplyProfileIncarnationDescriptionUseCase
import org.veo.core.usecase.catalogitem.GetProfileIncarnationDescriptionUseCase
import org.veo.core.usecase.unit.DeleteUnitUseCase
import org.veo.core.usecase.unit.DeleteUnitUseCase.InputData
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.metrics.DataSourceProxyBeanPostProcessor

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

    @DynamicPropertySource
    static void setRowCount(DynamicPropertyRegistry registry) {
        registry.add("veo.logging.datasource.row_count", { -> true })
    }

    def "delete a unit with example elements"() {
        given: 'a unit with example elements'
        def client = createTestClient()
        var domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        client = clientRepository.getById(client.id)
        def unit = unitRepository.save(newUnit(client))
        executeInTransaction {
            def profileId = domain.profiles.first().id
            var incarnationDescriptions = getProfileIncarnationDescriptionUseCase.execute(
                    new GetProfileIncarnationDescriptionUseCase.InputData(client, unit.id, domain.id, null, profileId, false)
                    ).references
            applyProfileIncarnationDescriptionUseCase.execute(
                    new ApplyProfileIncarnationDescriptionUseCase.InputData(client, unit.id, incarnationDescriptions))
        }
        QueryCountHolder.clear()
        def rowCountBefore = DataSourceProxyBeanPostProcessor.totalResultSetRowsRead

        when: 'executing the DeleteUnitUseCase'
        runUseCase(unit)
        def queryCounts = QueryCountHolder.grandTotal

        then: 'query statistics show sensible data'
        verifyAll {
            queryCounts.select == 33
            queryCounts.insert == 2
            queryCounts.update == 1
            queryCounts.delete == 41
            queryCounts.time < 1000
            // 115 is the currently observed count of 108 rows plus an acceptable safety margin
            DataSourceProxyBeanPostProcessor.totalResultSetRowsRead - rowCountBefore <= 115
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
                    obtainRisk(scenarios[i], testDomain).tap {
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
                    obtainRisk(scenarios[i], testDomain).tap {
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
                    obtainRisk(scenarios[i], testDomain).tap {
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
        runUseCase(unit)
        def queryCounts = QueryCountHolder.grandTotal

        then: 'query statistics show sensible data'
        verifyAll {
            queryCounts.select == 65
            queryCounts.insert == 38
            queryCounts.update == 1
            queryCounts.delete == 1452
            queryCounts.time < 6000
            // 16500 is the currently observed count of 15824 rows plus an acceptable safety margin
            DataSourceProxyBeanPostProcessor.totalResultSetRowsRead - rowCountBefore <= 16500
        }
    }

    def runUseCase(Unit unit) {
        executeInTransaction {
            deleteUnitUseCase.execute(new InputData(unit.id, unit.client))
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