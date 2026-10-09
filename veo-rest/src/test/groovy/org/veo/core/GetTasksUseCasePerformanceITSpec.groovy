/*
 * verinice.veo
 * Copyright (C) 2026  Jonas Jordan
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
 */
package org.veo.core

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.entity.ref.TypedId
import org.veo.core.repository.PagingConfiguration
import org.veo.core.repository.TaskQuery
import org.veo.core.usecase.GetTasksUseCase
import org.veo.persistence.metrics.DataSourceProxyBeanPostProcessor
import org.veo.rest.security.NoRestrictionAccessRight

import net.ttddyy.dsproxy.QueryCountHolder

class GetTasksUseCasePerformanceITSpec extends AbstractPerformanceITSpec {

    @Autowired
    private GetTasksUseCase getTasksUseCase

    private Client client
    private Unit unit
    Domain domain

    @DynamicPropertySource
    static void setRowCount(DynamicPropertyRegistry registry) {
        registry.add("veo.logging.datasource.row_count", { -> true })
    }

    @WithUserDetails("user@domain.example")
    def "SQL performance for retrieving tasks"() {
        given:
        def assetCount = 20
        def controlCount = 10
        def riCount = assetCount * controlCount
        txTemplate.execute {
            client = createTestClient()
            domain = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID)
            unit = unitDataRepository.save(newUnit(client,) {
                domains = [domain]
            })
            def documents = (0..<riCount).collect { i ->
                documentDataRepository.save(newDocument(unit) {
                    name = "document $i"
                    associateWithDomain(domain, "Manual", "CURRENT")
                })
            }
            def responsibles = (0..<riCount).collect { i ->
                personDataRepository.save(newPerson(unit) {
                    name = "responsible $i"
                    associateWithDomain(domain, "Programmer", "CODING")
                })
            }
            def implementers = (0..<riCount).collect { i ->
                personDataRepository.save(newPerson(unit) {
                    name = "implementer $i"
                    associateWithDomain(domain, "Programmer", "CODING")
                })
            }
            def lastRevisionists = (0..<riCount).collect { i ->
                personDataRepository.save(newPerson(unit) {
                    name = "last revisionist $i"
                    associateWithDomain(domain, "Programmer", "CODING")
                })
            }
            def nextRevisionists = (0..<riCount).collect { i ->
                personDataRepository.save(newPerson(unit) {
                    name = "next revisionist $i"
                    associateWithDomain(domain, "Programmer", "CODING")
                })
            }
            def controls = (0..<controlCount).collect { i ->
                controlDataRepository.save(newControl(unit) {
                    name = "control $i"
                    associateWithDomain(domain, "TOM", "NEW")
                })
            }

            (0..<assetCount).collect { assetIdx ->
                assetDataRepository.save(newAsset(unit) {
                    name = "asset $assetIdx"
                    associateWithDomain(domain, "Server", "NEW")
                    controls.eachWithIndex {control, controlIdx->
                        implementControl(control)
                        getRequirementImplementation(control).tap{
                            responsible = responsibles[assetIdx*controlCount + controlIdx]
                            document = documents[assetIdx*controlCount + controlIdx]
                            implementedBy = implementers[assetIdx*controlCount + controlIdx]
                            lastRevisionBy = lastRevisionists[assetIdx*controlCount + controlIdx]
                            nextRevisionBy = nextRevisionists[assetIdx*controlCount + controlIdx]
                        }
                    }
                })
            }
        }
        QueryCountHolder.clear()

        when:
        def rowCountBefore = DataSourceProxyBeanPostProcessor.totalResultSetRowsRead
        def tasks = executeInTransaction {
            getTasksUseCase.execute(
                    new GetTasksUseCase.InputData(
                            TypedId.from(domain),
                            TypedId.from(unit),
                            null,
                            PagingConfiguration.unpaged(TaskQuery.SortCriterion.DEADLINE)
                            ),
                    NoRestrictionAccessRight.from(client.getId().toString()))
                    .page()
                    .resultPage()
        }
        def queryCounts = QueryCountHolder.grandTotal

        then: "relations have been loaded"
        tasks.size() == riCount
        tasks*.assignee()*.name =~ (0..<riCount).collect{"responsible $it"}
        with(tasks*.requirementImplementation()) {
            it*.control*.name =~ (0..<controlCount).collect{"control $it"}
            it*.origin*.name =~ (0..<assetCount).collect{"asset $it"}
            it*.document*.name =~ (0..<riCount).collect{"document $it"}
            it*.responsible*.name =~ (0..<riCount).collect{"responsible $it"}
            it*.implementedBy*.name =~ (0..<riCount).collect{"implementer $it"}
            it*.lastRevisionBy*.name =~ (0..<riCount).collect{
                "last revisionist $it"
            }
            it*.nextRevisionBy*.name =~ (0..<riCount).collect{
                "next revisionist $it"
            }
        }

        and:
        queryCounts.select == 6
        queryCounts.insert == 0
        queryCounts.update == 0
        queryCounts.delete == 0
        queryCounts.time < 1000
        // expect the currently observed count of 1713 rows plus an acceptable safety margin
        DataSourceProxyBeanPostProcessor.totalResultSetRowsRead - rowCountBefore <= 1884
    }
}
