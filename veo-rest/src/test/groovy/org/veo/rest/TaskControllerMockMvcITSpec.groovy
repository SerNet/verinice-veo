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
package org.veo.rest

import java.time.LocalDate

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.MissingServletRequestParameterException

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.entity.compliance.ImplementationStatus
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.usecase.TemplateItems
import org.veo.persistence.access.AssetRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ControlRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

class TaskControllerMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private AssetRepositoryImpl assetRepository

    @Autowired
    private PersonRepositoryImpl personRepository

    @Autowired
    private ControlRepositoryImpl controlRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private DomainRepositoryImpl domainRepository

    @Autowired
    TransactionTemplate txTemplate

    private Client client
    private Unit unit
    private Domain testDomain

    def setup() {
        txTemplate.execute {
            client = createTestClient()
            testDomain = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID)

            unit = unitRepository.save(newUnit(client) {
                name = "Test unit"
            })
        }
    }

    @WithUserDetails("user@domain.example")
    def "fetch tasks for RIs"() {
        given:
        txTemplate.execute {
            def personA = personRepository.save(newPerson(unit) {
                name = 'person a'
                associateWithDomain(testDomain, "Programmer", "CODING")
            })
            def personB = personRepository.save(newPerson(unit) {
                name = 'person b'
                associateWithDomain(testDomain, "Programmer", "CODING")
            })
            def controlA = controlRepository.save(newControl(unit) {
                name = 'control a'
                associateWithDomain(testDomain, "TOM", "NEW")
            })
            def controlB = controlRepository.save(newControl(unit) {
                name = 'control b'
                associateWithDomain(testDomain, "TOM", "NEW")
            })

            assetRepository.save(newAsset(unit) {
                name = "big machine"
                associateWithDomain(testDomain, "Server", "NEW")
                implementControl(controlA)
                getRequirementImplementation(controlA).tap{
                    status = ImplementationStatus.NO
                    implementationUntil = LocalDate.parse("2027-01-01")
                    nextRevisionDate = LocalDate.parse("2028-01-01")
                    responsible = personA
                }
                implementControl(controlB)
                getRequirementImplementation(controlB).tap{
                    status = ImplementationStatus.YES
                    implementationUntil = LocalDate.parse("2027-01-02")
                    nextRevisionDate = LocalDate.parse("2028-01-02")
                }
            })
            processDataRepository.save(newProcess(unit) {
                name = "little help"
                associateWithDomain(testDomain, "BusinessProcess", "NEW")
                implementControl(controlA)
                getRequirementImplementation(controlA).tap{
                    status = ImplementationStatus.NO
                    implementationUntil = LocalDate.parse("2027-02-01")
                    responsible = personB
                }
            })
        }

        expect:
        with(parseJson(get("/domains/${testDomain.id}/tasks?unit=${unit.id}"))) {
            totalItemCount == 3
            page == 0
            pageCount == 1
            items*.deadline == [
                "2027-01-01",
                "2027-02-01",
                "2028-01-02",
            ]

            with(items[0]) {
                type == "requirement-implementation"
                requirementImplementation.origin.name == "big machine"
                requirementImplementation.control.name == "control a"
                assignee.name == "person a"
            }
            with(items[1]) {
                type == "requirement-implementation"
                requirementImplementation.origin.name == "little help"
                requirementImplementation.control.name == "control a"
                assignee.name == "person b"
            }
            with(items[2]) {
                type == "requirement-implementation-revision"
                requirementImplementation.origin.name == "big machine"
                requirementImplementation.control.name == "control b"
                assignee == null
            }
        }
    }

    @WithUserDetails("user@domain.example")
    def "clients boundaries are respected"() {
        given:
        Domain otherClientsDomain
        Unit otherClientsUnit
        txTemplate.execute {
            def otherClient = clientRepository.save(newClient {})
            otherClientsDomain = createTestDomain(otherClient, TEST_DOMAIN_TEMPLATE_ID, TemplateItems.NONE)
            otherClientsUnit = unitRepository.save(newUnit(otherClient))

            def otherClientsControl = controlRepository.save(newControl(otherClientsUnit) {
                name = 'control'
                associateWithDomain(otherClientsDomain, "TOM", "NEW")
            })
            assetRepository.save(newAsset(otherClientsUnit) {
                name = "asset"
                associateWithDomain(otherClientsDomain, "Server", "NEW")
                implementControl(otherClientsControl)
            })
        }

        when:
        get("/domains/${otherClientsDomain.id}/tasks?unit=${otherClientsUnit.id}", 404)

        then:
        def ex = thrown(NotFoundException)
        ex.message == "domain ${otherClientsDomain.id} not found"

        when:
        get("/domains/${testDomain.id}/tasks?unit=${otherClientsUnit.id}", 404)

        then:
        ex = thrown(NotFoundException)
        ex.message == "unit ${otherClientsUnit.id} not found"

        when:
        get("/domains/${otherClientsDomain.id}/tasks?unit=${unit.id}", 404)

        then:
        ex = thrown(NotFoundException)
        ex.message == "domain ${otherClientsDomain.id} not found"

        when:
        get("/domains/${otherClientsDomain.id}/tasks", 400)

        then:
        ex = thrown(MissingServletRequestParameterException)
        ex.message == "Required request parameter 'unit' for method parameter type UUID is not present"
    }
}
