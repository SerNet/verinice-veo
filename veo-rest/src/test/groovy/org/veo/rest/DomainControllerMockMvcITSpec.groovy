/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.condition.Condition
import org.veo.core.entity.condition.GreaterThanMatcher
import org.veo.core.entity.condition.PartCountProvider
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.persistence.access.ClientRepositoryImpl

/**
 * Integration test for the domain controller. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
class DomainControllerMockMvcITSpec extends ContentSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    TransactionTemplate txTemplate

    private Domain testDomain
    private Domain completeDomain
    private Domain secondDomain
    private Domain domainSecondClient
    private Client client

    def setup() {
        txTemplate.execute {
            def rd = createRiskDefinition("id1")

            this.client = createTestClient()
            newDomain(client) {
                name = "Domain 1"
                applyElementTypeDefinition(newElementTypeDefinition("person", it) {
                    subTypes = [
                        Team: newSubTypeDefinition {},
                        Employee: newSubTypeDefinition {},
                    ]
                })
                applyDecision('isBigTeam', newDecision('person', 'Team') {
                    it.name = newTranslatedText("Big team")
                    it.rules.add(newRule(true) {
                        description = newTranslatedText("Team has more than 10 members")
                        conditions.add(new Condition(
                                new PartCountProvider("Employee"),
                                new GreaterThanMatcher(BigDecimal.valueOf(10))
                                ))
                    })
                })
            }
            newDomain(client) {
                name = "Domain 2"
            }
            newDomain(client) { d->
                name = "Domain-complete"
                newCatalogItem(d,{
                    elementType = "control"
                    subType = "CTL_TOM"
                    status = "NEW"
                    name = 'c1'
                })
                newCatalogItem(d,{
                    elementType = "control"
                    subType = "CTL_TOM"
                    status = "NEW"
                    name = 'c2'
                })
                newCatalogItem(d,{
                    elementType = "control"
                    subType = "CTL_TOM"
                    status = "NEW"
                    name = 'c3'
                })
                riskDefinitions = ["id":rd] as Map
                newProfile(d,{p->
                    name = "test-profile"
                    description = "my description"
                    language = "de_DE"
                    newProfileItem(p,{
                        elementType = "control"
                        subType = "CTL_TOM"
                        status = "NEW"
                        name = "profile-tom"
                    })
                })
                newProfile(d,{
                    name = "test-profile1"
                    description = "another description"
                    language = "de_DE"
                })
            }

            client = clientRepository.save(client)

            testDomain = client.domains.find{it.name == "Domain 1"}
            completeDomain = client.domains.find{it.name == "Domain-complete"}
            secondDomain = client.domains.find{it.name == "Domain 2"}

            def secondClient = clientRepository.save(newClient() {
                newDomain(it)
            })
            domainSecondClient = secondClient.domains.first()
        }
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a Domain"() {
        given: "a saved domain"

        when: "a request is made to the server"
        def results = get("/domains/${testDomain.id.uuidValue()}")

        then: "the eTag is set"
        getETag(results) != null

        and:
        def result = parseJson(results)
        result._self == "http://localhost/domains/${testDomain.id.uuidValue()}"
        result.name == testDomain.name
        result.elementTypeDefinitions.size() == 8
        result.elementTypeDefinitions.keySet() =~ [
            'asset',
            'control',
            'process',
            'scope',
            'scenario',
            'person',
            'document',
            'incident'
        ]
        with(result.decisions.isBigTeam) {
            name.en == "Big team"
            elementSubType == "Team"
            rules[0].description.en == "Team has more than 10 members"
            rules[0].conditions[0].inputProvider.type == "partCount"
            rules[0].conditions[0].inputProvider.partSubType == "Employee"
            rules[0].conditions[0].inputMatcher.type == "greaterThan"
            rules[0].conditions[0].inputMatcher.comparisonValue == 10
        }
    }

    @WithUserDetails("user@domain.example")
    def "cannot retrieve other client's domain"() {
        given: "a saved domain"

        when: "trying to retrieve the other client's domain"
        get("/domains/${domainSecondClient.id.uuidValue()}", 404)

        then: "a client boundary violation is detected"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all domains for a client"() {
        when: "a request is made to the server"
        def result = parseJson(get("/domains?"))

        then: "the domains are returned"
        result.size() == 3
        result*.name.sort().first() == 'Domain 1'
    }

    @WithUserDetails("user@domain.example")
    def "export a Domain"() {
        given: "a saved domain"

        when: "a request is made to the server"
        def results = get("/domains/${completeDomain.id.uuidValue()}/export")
        def result = parseJson(results)

        then: "the domain is exported"
        result.name == completeDomain.name
        result.elementTypeDefinitions != null
        result.riskDefinitions !=null
        result.catalogItems*.name ==~ ["c1", "c2", "c3"]
    }

    @WithUserDetails("user@domain.example")
    def "get all Profiles and items"() {
        when: "get the profiles"
        def result = parseJson(get("/domains/${completeDomain.id.uuidValue()}/profiles"))
        def orderedProfiles = result.sort{ it.name }

        then:
        result.size() == 2
        with(orderedProfiles[0]) {
            name == 'test-profile'
            description == 'my description'
            language == 'de_DE'
        }
        with(orderedProfiles[1]) {
            name == 'test-profile1'
            description == 'another description'
            language == 'de_DE'
        }

        when: "get the profile items"
        result = parseJson(get("/domains/${completeDomain.id.uuidValue()}/profiles/${orderedProfiles[0].id}/items"))

        then:
        result.size() == 1
        with(result[0]) {
            name == "profile-tom"
            subType == "CTL_TOM"
            elementType == "control"
        }

        when: "get a single profile item"
        result = parseJson(get("/domains/${completeDomain.id.uuidValue()}/profiles/${orderedProfiles[0].id}/items/${result[0].id}"))

        then:
        with(result) {
            name == "profile-tom"
            subType == "CTL_TOM"
            elementType == "control"
        }
    }

    @WithUserDetails("user@domain.example")
    def "retrieve element statistics for a domain"() {
        given: 'a test domain with some elements'
        def client = testDomain.owner
        def domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        def unit = executeInTransaction{
            def unit = unitDataRepository.save(newUnit(client))
            processDataRepository.save(newProcess(unit) {
                associateWithDomain(domain, 'PRO_DPIA', 'FOR_REVIEW')
            })
            processDataRepository.save(newProcess(unit) {
                associateWithDomain(domain, 'PRO_DataProcessing', 'NEW')
            })
            processDataRepository.save(newProcess(unit) {
                associateWithDomain(domain, 'PRO_DataProcessing', 'ARCHIVED')
            })
            scopeDataRepository.save(newScope(unit) {
                associateWithDomain(domain, 'SCP_ResponsibleBody', 'IN_PROGRESS')
            })
            unit
        }

        when:
        def result = parseJson(get("/domains/${domain.idAsString}/element-status-count?unit=${unit.idAsString}"))

        then: "the counts for each element are returned"
        result.size() == 8
        with(result.process) {
            size() == 3
            get('PRO_DPIA') == [IN_PROGRESS:0, NEW:0, RELEASED:0, FOR_REVIEW:1, ARCHIVED:0]
            get('PRO_DataProcessing') == [IN_PROGRESS:0, NEW:1, RELEASED:0, FOR_REVIEW:0, ARCHIVED:1]
        }
        with(result.scope) {
            size() == 5
            get('SCP_ResponsibleBody') == [IN_PROGRESS:1, NEW:0, RELEASED:0, FOR_REVIEW:0, ARCHIVED:0]
        }
        with(result.asset) {
            size() == 3
            get('AST_Application') == [IN_PROGRESS:0, NEW:0, RELEASED:0, FOR_REVIEW:0, ARCHIVED:0]
        }
    }

    @WithUserDetails("user@domain.example")
    def "retrieve element statistics for a unit with invalid elements"() {
        given:
        def client = testDomain.owner
        def domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        def unit = executeInTransaction{
            def unit = unitDataRepository.save(newUnit(client))
            processDataRepository.save(newProcess(unit) {
                associateWithDomain(domain, 'PRO_DPIA', 'FOR_REVIEW')
            })
            processDataRepository.save(newProcess(unit) {
                associateWithDomain(domain, 'PRO_DataProcessing', 'NEW')
            })
            processDataRepository.save(newProcess(unit) {
                associateWithDomain(domain, 'PRO_DoesNotExist', 'ARCHIVED')
            })

            unit
        }

        when:
        def result = parseJson(get("/domains/${domain.idAsString}/element-status-count?unit=${unit.idAsString}"))

        then:
        result.size() == 8
        with(result.process) {
            size() == 3
            get('PRO_DPIA') == [IN_PROGRESS:0, NEW:0, RELEASED:0, FOR_REVIEW:1, ARCHIVED:0]
            get('PRO_DataProcessing') == [IN_PROGRESS:0, NEW:1, RELEASED:0, FOR_REVIEW:0, ARCHIVED:0]
            !containsKey('PRO_DoesNotExist')
        }
    }

    @WithUserDetails("user@domain.example")
    def "retrieve example profile metadata"() {
        given:
        def client = testDomain.owner
        def domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)

        when:
        def result = parseJson(get("/domains/${domain.idAsString}"))

        then:
        result.profiles.size() == 1
        with(result.profiles.exampleOrganization) {
            name == 'Beispieldaten'
            description == 'Beispieldaten für den Datenschutz'
            language == 'de_DE'
        }
    }

    @WithUserDetails("user@domain.example")
    def "retrieve sub type statistic for the stored DS-GVO catalog items"() {
        given:
        def client = testDomain.owner
        def domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)

        when:
        def result = parseJson(get("/domains/${domain.idAsString}/catalog-items/type-count"))

        then:
        result.size() == 3
        with(result.process) {
            PRO_DataProcessing == 1
        }
        with(result.scenario) {
            SCN_Scenario == 56
        }
        with(result.control) {
            CTL_TOM == 8
        }
    }
}