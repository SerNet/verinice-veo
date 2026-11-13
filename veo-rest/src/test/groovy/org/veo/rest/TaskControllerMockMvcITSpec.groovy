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
import org.veo.core.entity.ElementType
import org.veo.core.entity.Person
import org.veo.core.entity.TaskConfiguration
import org.veo.core.entity.TranslatedText
import org.veo.core.entity.Unit
import org.veo.core.entity.compliance.ImplementationStatus
import org.veo.core.entity.condition.ConstantExpression
import org.veo.core.entity.condition.EqualsExpression
import org.veo.core.entity.condition.MaxRiskExpression
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.repository.InspectionFindingRepository
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
    private InspectionFindingRepository inspectionFindingRepository

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
        Person personA
        Person personB
        txTemplate.execute {
            personA = personRepository.save(newPerson(unit) {
                name = 'person a'
                username = "user@domain.example"
                associateWithDomain(testDomain, "Programmer", "CODING")
            })
            personB = personRepository.save(newPerson(unit) {
                name = 'person b'
                username = "boozer@domain.example"
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
                    nextRevisionBy = personA
                }
            })
            processDataRepository.save(newProcess(unit) {
                name = "little help"
                associateWithDomain(testDomain, "BusinessProcess", "NEW")
                implementControl(controlA)
                getRequirementImplementation(controlA).tap{
                    status = ImplementationStatus.NO
                    implementationUntil = LocalDate.parse("2027-02-01")
                    responsible = personA
                }
                implementControl(controlB)
                getRequirementImplementation(controlB).tap{
                    status = ImplementationStatus.NO
                    implementationUntil = LocalDate.parse("2027-02-02")
                    responsible = personB
                }
            })
        }

        expect:
        with(parseJson(get("/domains/${testDomain.id}/tasks?unit=${unit.id}"))) {
            totalItemCount == 4
            page == 0
            pageCount == 1
            items*.deadline == [
                "2027-01-01",
                "2027-02-01",
                "2027-02-02",
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
                assignee.name == "person a"
            }
            with(items[2]) {
                type == "requirement-implementation"
                requirementImplementation.origin.name == "little help"
                requirementImplementation.control.name == "control b"
                assignee.name == "person b"
            }
            with(items[3]) {
                type == "requirement-implementation-revision"
                requirementImplementation.origin.name == "big machine"
                requirementImplementation.control.name == "control b"
                assignee.name == "person a"
            }
        }

        and:
        with(parseJson(get("/domains/${testDomain.id}/tasks?unit=${unit.id}&assignees=${personA.id}"))) {
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
                assignee.name == "person a"
            }
            with(items[2]) {
                type == "requirement-implementation-revision"
                requirementImplementation.origin.name == "big machine"
                requirementImplementation.control.name == "control b"
                assignee.name == "person a"
            }
        }
        with(parseJson(get("/domains/${testDomain.id}/tasks?unit=${unit.id}&assignees=${personB.id}"))) {
            totalItemCount == 1
            page == 0
            pageCount == 1
            items*.deadline == [
                "2027-02-02",
            ]
            with(items[0]) {
                type == "requirement-implementation"
                requirementImplementation.origin.name == "little help"
                requirementImplementation.control.name == "control b"
                assignee.name == "person b"
            }
        }
    }

    @WithUserDetails("user@domain.example")
    def "fetch unassigned tasks"() {
        given:
        Person personA
        txTemplate.execute {
            personA = personRepository.save(newPerson(unit) {
                name = 'persona'
                username = "my@persona.example"
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
                name = "way big machine"
                associateWithDomain(testDomain, "Server", "NEW")
                implementControl(controlA)
                getRequirementImplementation(controlA).tap{
                    responsible = personA
                }
                implementControl(controlB)
            })
        }

        expect:
        with(parseJson(get("/domains/${testDomain.id}/tasks?unit=${unit.id}&assignees="))) {
            totalItemCount == 1
            page == 0
            pageCount == 1
            items*.deadline == [
                null
            ]

            with(items[0]) {
                type == "requirement-implementation"
                requirementImplementation.origin.name == "way big machine"
                requirementImplementation.control.name == "control b"
                assignee == null
            }
        }
    }

    @WithUserDetails("content-creator")
    def "fetch inspection tasks"() {
        given:
        txTemplate.execute {
            testDomain.applyInspection("check", newInspection {
                elementType = ElementType.ASSET
                condition = new ConstantExpression(true)
            })
            testDomain.taskConfiguration = new TaskConfiguration([
                check: new TaskConfiguration.InspectionTaskTemplate(
                        TranslatedText.of {"check that asset"},
                        "admin"
                        )
            ])
            testDomain = domainDataRepository.save(testDomain)
            def person = personRepository.save(newPerson(unit) {
                name = 'admin'
                username = "my@persona.example"
                associateWithDomain(testDomain, "Programmer", "CODING")
            })
            assetRepository.save(newAsset(unit) {
                name = "süßtem"
                associateWithDomain(testDomain, "Server", "NEW")
                addLink(newCustomLink(person, "admin", testDomain))
            })
            inspectionFindingRepository.updateInspections(testDomain, false)
        }

        expect:
        with(parseJson(get("/domains/${testDomain.id}/tasks?unit=${unit.id}"))) {
            totalItemCount == 1
            page == 0
            pageCount == 1
            items*.deadline == [
                null
            ]

            with(items[0]) {
                type == "inspection-finding"
                assignee.name == "admin"
                element.name == "süßtem"
                severity == "HINT"
                summary.en == "check that asset"
            }
        }

        when: "changing the task template"
        parseJson(get("/domains/${testDomain.id}/task-configuration")).with{
            it.inspectionTaskTemplates.check.summary.en = "check that asset ASAP"
            put("/content-creation/domains/${owner.testDomain.id}/task-configuration", it,204)
        }

        then: "the task has changed"
        with(parseJson(get("/domains/${testDomain.id}/tasks?unit=${unit.id}"))) {
            totalItemCount == 1
            with(items[0]) {
                type == "inspection-finding"
                summary.en == "check that asset ASAP"
            }
        }
    }

    @WithUserDetails("user@domain.example")
    def "inspection tasks are updated when a relevant risk changes"() {
        given:
        def assetId
        def scenarioId
        txTemplate.execute {
            testDomain.applyInspection("check", newInspection {
                elementType = ElementType.ASSET
                description = TranslatedText.of {"No risk values"}
                condition = new EqualsExpression(
                                new MaxRiskExpression(),
                                new ConstantExpression(null)
                        )
            })
            testDomain = domainDataRepository.save(testDomain)
            def scenario = scenarioDataRepository.save(newScenario(unit) {
                name = "bad things"
                associateWithDomain(testDomain, "Attack", "NEW")
            })
            def asset = assetRepository.save(newAsset(unit) {
                name = "all my IT"
                associateWithDomain(testDomain, "Server", "NEW")
                obtainRisk(scenario).with(this::assignDesignator)
            })
            inspectionFindingRepository.updateInspections(testDomain, false)
            assetId = asset.id
            scenarioId = scenario.id
        }

        expect:
        with(parseJson(get("/domains/${testDomain.id}/tasks?unit=${unit.id}"))) {
            totalItemCount == 1
            with(items[0]) {
                type == "inspection-finding"
                element.name == "all my IT"
                summary.en == "No risk values"
            }
        }

        when: "updating the risk"
        get("/assets/$assetId/risks/$scenarioId").with{
            def body = parseJson(it)
            body.domains[owner.testDomain.idAsString].riskDefinitions = [
                riskyDef: [
                    // TODO #5312 remove pointless impact
                    impactValues: [
                        [
                            category      : "D",
                            specificImpact: 1,
                        ]
                    ],
                    riskValues  : [
                        [
                            category: "D",
                            userDefinedResidualRisk: 1,
                        ]
                    ]
                ]
            ]
            owner.put(body._self, body, ['If-Match': getETag(it)])
        }

        then: "the task is solved"
        with(parseJson(get("/domains/${testDomain.id}/tasks?unit=${unit.id}"))) {
            totalItemCount == 0
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

            def otherClientsPerson = personRepository.save(newPerson(otherClientsUnit) {
                name = 'person'
                username = "user@domain.example" // Our own username in the other client!
                associateWithDomain(otherClientsDomain, "Nerd", "NerdingOut")
            })
            def otherClientsControl = controlRepository.save(newControl(otherClientsUnit) {
                name = 'control'
                associateWithDomain(otherClientsDomain, "TOM", "NEW")
            })
            assetRepository.save(newAsset(otherClientsUnit) {
                name = "asset"
                associateWithDomain(otherClientsDomain, "Server", "NEW")
                implementControl(otherClientsControl)
                getRequirementImplementation(otherClientsControl).tap{
                    responsible = otherClientsPerson
                }
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
