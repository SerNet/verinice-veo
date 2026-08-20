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
package org.veo.persistence.access

import java.time.LocalDate

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.UserAccessRights
import org.veo.core.entity.Domain
import org.veo.core.entity.Task
import org.veo.core.entity.TaskType
import org.veo.core.entity.Unit
import org.veo.core.entity.compliance.ImplementationStatus
import org.veo.core.repository.PagedResult
import org.veo.core.repository.PagingConfiguration
import org.veo.core.repository.TaskQuery
import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.ControlDataRepository
import org.veo.persistence.access.jpa.DomainDataRepository
import org.veo.persistence.access.jpa.PersonDataRepository
import org.veo.persistence.access.jpa.RequirementImplementationDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.persistence.access.query.TaskQueryImpl
import org.veo.persistence.entity.jpa.AbstractJpaSpec
import org.veo.persistence.entity.jpa.ClientData
import org.veo.rest.security.NoRestrictionAccessRight

import jakarta.persistence.EntityManager

class TaskQuerySpec extends AbstractJpaSpec {
    @Autowired
    ClientDataRepository clientDataRepository
    @Autowired
    DomainDataRepository domainDataRepository
    @Autowired
    UnitDataRepository unitRepository
    @Autowired
    ControlDataRepository controlDataRepository
    @Autowired
    AssetDataRepository assetDataRepository
    @Autowired
    PersonDataRepository personDataRepository
    @Autowired
    RequirementImplementationDataRepository riDataRepository
    @Autowired
    TransactionTemplate txTemplate
    @Autowired
    EntityManager em

    ClientData client
    Domain domain
    Unit unit

    def setup() {
        client = clientDataRepository.save(newClient {
            newDomain(it)
        })
        domain = client.domains.first()
        unit = unitRepository.save(newUnit(client))
    }

    def "fetch tasks for RIs"() {
        given:
        txTemplate.execute {
            def personA = personDataRepository.save(newPerson(unit) {
                name = 'person a'
                associateWithDomain(domain, "Programmer", "CODING")
            })
            def personB = personDataRepository.save(newPerson(unit) {
                name = 'person b'
                associateWithDomain(domain, "Programmer", "CODING")
            })
            def controlA = controlDataRepository.save(newControl(unit) {
                name = 'control a'
                associateWithDomain(domain, "TOM", "NEW")
            })
            def controlB = controlDataRepository.save(newControl(unit) {
                name = 'control b'
                associateWithDomain(domain, "TOM", "NEW")
            })
            def controlC = controlDataRepository.save(newControl(unit) {
                name = 'control c'
                associateWithDomain(domain, "TOM", "NEW")
            })
            def controlD = controlDataRepository.save(newControl(unit) {
                name = 'control d'
                associateWithDomain(domain, "TOM", "NEW")
            })
            def controlE = controlDataRepository.save(newControl(unit) {
                name = 'control e'
                associateWithDomain(domain, "TOM", "NEW")
            })
            def superControl = controlDataRepository.save(newControl(unit) {
                name = 'super control'
                addParts([
                    controlA,
                    controlB,
                    controlC,
                    controlD,
                    controlE,
                ] as Set)
                associateWithDomain(domain, "TOM", "NEW")
            })

            assetDataRepository.save(newAsset(unit) {
                name = "servicable"
                associateWithDomain(domain, "Server", "NEW")
                implementControl(superControl)
                getRequirementImplementation(controlA).tap {
                    status = ImplementationStatus.NO
                    implementationUntil = LocalDate.parse("2027-01-01")
                    nextRevisionDate = LocalDate.parse("2028-01-01")
                    nextRevisionBy = personB
                    responsible = personA
                }
                getRequirementImplementation(controlB).tap {
                    status = ImplementationStatus.PARTIAL
                    implementationUntil = LocalDate.parse("2027-01-02")
                    nextRevisionDate = LocalDate.parse("2028-01-02")
                    nextRevisionBy = personB
                }
                getRequirementImplementation(controlC).tap {
                    status = ImplementationStatus.YES
                    implementationUntil = LocalDate.parse("2027-01-03")
                    nextRevisionDate = LocalDate.parse("2028-01-03")
                    nextRevisionBy = personB
                    responsible = personA
                }
                getRequirementImplementation(controlD).tap {
                    status = ImplementationStatus.YES
                    implementationUntil = LocalDate.parse("2027-01-04")
                }
                getRequirementImplementation(controlE).tap {
                    responsible = personB
                }
            })
            processDataRepository.save(newProcess(unit) {
                name = "processional"
                associateWithDomain(domain, "BusinessProcess", "NEW")
                implementControl(controlA)
                getRequirementImplementation(controlA).tap {
                    status = ImplementationStatus.NO
                    implementationUntil = LocalDate.parse("2027-02-01")
                    responsible = personA
                }
            })

            expect:
            with(query()) {
                totalResults() == 5
                totalPages() == 1
                resultPage()*.deadline() == [
                    LocalDate.parse("2027-01-01"),
                    LocalDate.parse("2027-01-02"),
                    LocalDate.parse("2027-02-01"),
                    LocalDate.parse("2028-01-03"),
                    null,
                ]

                with(resultPage()[0]) {
                    type() == TaskType.REQUIREMENT_IMPLEMENTATION
                    requirementImplementation().origin.name == "servicable"
                    requirementImplementation().control.name == "control a"
                    assignee().name == "person a"
                }
                with(resultPage()[1]) {
                    type() == TaskType.REQUIREMENT_IMPLEMENTATION
                    requirementImplementation().origin.name == "servicable"
                    requirementImplementation().control.name == "control b"
                    assignee() == null
                }
                with(resultPage()[2]) {
                    type() == TaskType.REQUIREMENT_IMPLEMENTATION
                    requirementImplementation().origin.name == "processional"
                    requirementImplementation().control.name == "control a"
                    assignee().name == "person a"
                }
                with(resultPage()[3]) {
                    type() == TaskType.REQUIREMENT_IMPLEMENTATION_REVISION
                    requirementImplementation().origin.name == "servicable"
                    requirementImplementation().control.name == "control c"
                    assignee().name == "person b"
                }
                with(resultPage()[4]) {
                    type() == TaskType.REQUIREMENT_IMPLEMENTATION
                    requirementImplementation().origin.name == "servicable"
                    requirementImplementation().control.name == "control e"
                    assignee().name == "person b"
                }
            }
        }
    }

    def "tasks are filtered by unit"() {
        given:
        Unit unit2
        txTemplate.execute {
            def control1 = controlDataRepository.save(newControl(unit) {
                name = 'control 1'
                associateWithDomain(domain, "TOM", "NEW")
            })
            assetDataRepository.save(newAsset(unit) {
                name = "asset 1"
                associateWithDomain(domain, "Server", "NEW")
                implementControl(control1)
            })

            unit2 = unitRepository.save(newUnit(client) {
                name = "unit 2"
            })
            def control2 = controlDataRepository.save(newControl(unit2) {
                name = 'control 2'
                associateWithDomain(domain, "TOM", "NEW")
            })
            assetDataRepository.save(newAsset(unit2) {
                name = "asset 2"
                associateWithDomain(domain, "Server", "NEW")
                implementControl(control2)
            })
        }

        expect:
        with(query(unit)) {
            totalResults() == 1
            resultPage()*.requirementImplementation()*.origin*.name == ["asset 1"]
            resultPage()*.requirementImplementation()*.control*.name == ["control 1"]
        }
        with(query(unit2)) {
            totalResults() == 1
            resultPage()*.requirementImplementation()*.origin*.name == ["asset 2"]
            resultPage()*.requirementImplementation()*.control*.name == ["control 2"]
        }
    }

    def "tasks are filtered by domain"() {
        given:
        Domain domain2
        txTemplate.execute {
            domain2 = newDomain(client) {name = "no 2"}
            client = clientDataRepository.save(client)
            domain2 = client.domains.find{it.name == "no 2"}
            unit.addToDomains(domain2)
            unit = unitRepository.save(unit)

            def control1 = controlDataRepository.save(newControl(unit) {
                name = 'control 1'
                associateWithDomain(domain, "TOM", "NEW")
            })
            def control2 = controlDataRepository.save(newControl(unit) {
                name = 'control 2'
                associateWithDomain(domain2, "TOM", "NEW")
            })

            assetDataRepository.save(newAsset(unit) {
                name = "asset"
                associateWithDomain(domain, "Server", "NEW")
                associateWithDomain(domain2, "AST_IT-System", "NEW")
                implementControl(control1)
                implementControl(control2)
            })
        }

        expect:
        with(query(unit,domain)) {
            totalResults() == 1
            resultPage()*.requirementImplementation()*.origin*.name == ["asset"]
            resultPage()*.requirementImplementation()*.control*.name == ["control 1"]
        }
        with(query(unit,domain2)) {
            totalResults() == 1
            resultPage()*.requirementImplementation()*.origin*.name == ["asset"]
            resultPage()*.requirementImplementation()*.control*.name == ["control 2"]
        }
    }

    PagedResult<Task, TaskQuery.SortCriterion> query(
            Unit unit = this.unit,
            Domain domain = this.domain,
            UserAccessRights rights = NoRestrictionAccessRight.from(client.idAsString),
            PagingConfiguration<TaskQuery.SortCriterion> paging = PagingConfiguration.unpaged(TaskQuery.SortCriterion.DEADLINE)) {
        txTemplate.execute {
            new TaskQueryImpl(em, personDataRepository, riDataRepository, rights, domain, unit).execute(paging)
        }
    }
}