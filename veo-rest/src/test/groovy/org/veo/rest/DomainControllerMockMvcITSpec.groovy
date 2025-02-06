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
import org.veo.core.entity.ElementType
import org.veo.core.entity.TemplateItemAspects
import org.veo.core.entity.condition.Condition
import org.veo.core.entity.condition.GreaterThanMatcher
import org.veo.core.entity.condition.PartCountExpression
import org.veo.core.entity.definitions.attribute.ExternalDocumentAttributeDefinition
import org.veo.core.entity.risk.RiskDefinitionRef
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
                applyElementTypeDefinition(newElementTypeDefinition(ElementType.PERSON, it) {
                    subTypes = [
                        Team: newSubTypeDefinition {
                            sortKey = 1
                        },
                        Employee: newSubTypeDefinition {
                            sortKey = 2
                        },
                    ]
                })
                applyDecision('isBigTeam', newDecision(ElementType.PERSON, 'Team') {
                    it.name = newTranslatedText("Big team")
                    it.rules.add(newRule(true) {
                        description = newTranslatedText("Team has more than 10 members")
                        conditions.add(new Condition(
                                new PartCountExpression("Employee"),
                                new GreaterThanMatcher(BigDecimal.valueOf(10))
                                ))
                    })
                })
            }
            newDomain(client) {
                name = "Domain 2"
                applyElementTypeDefinition(newElementTypeDefinition(ElementType.PERSON, it) {
                    subTypes = [
                        Team: newSubTypeDefinition {},
                        Member: newSubTypeDefinition {},
                    ]
                })
            }
            newDomain(client) { d->
                name = "Domain-complete"
                applyElementTypeDefinition(newElementTypeDefinition(ElementType.SCOPE, d) {
                    subTypes = [
                        S1: newSubTypeDefinition {
                            sortKey = 1
                        }
                    ]
                })

                newCatalogItem(d,{
                    elementType = ElementType.CONTROL
                    subType = "CTL_TOM"
                    status = "NEW"
                    name = 'c1'
                })
                newCatalogItem(d,{
                    elementType = ElementType.CONTROL
                    subType = "CTL_TOM"
                    status = "NEW"
                    name = 'c2'
                })
                newCatalogItem(d,{
                    elementType = ElementType.CONTROL
                    subType = "CTL_TOM"
                    status = "NEW"
                    name = 'c3'
                })
                newCatalogItem(d,{
                    elementType = ElementType.SCOPE
                    subType = "S1"
                    status = "NEW"
                    name = 'scp1'
                    aspects = new TemplateItemAspects(null, null, RiskDefinitionRef.from(rd))
                })
                riskDefinitions = ["id1":rd] as Map
                newProfile(d,{p->
                    name = "test-profile"
                    description = "my description"
                    language = "de_DE"
                    newProfileItem(p,{
                        elementType = ElementType.CONTROL
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
        def results = get("/domains/${testDomain.idAsString}")

        then: "the eTag is set"
        getETag(results) != null

        and:
        def result = parseJson(results)
        result._self == "http://localhost/domains/${testDomain.idAsString}"
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
        with(result.elementTypeDefinitions.person) {
            subTypes.Team.sortKey == 1
        }
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
        get("/domains/${domainSecondClient.idAsString}", 404)

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

    @WithUserDetails("content-creator")
    def "export a Domain"() {
        given: "a saved domain"

        when: "a request is made to the server"
        def results = get("/domains/${completeDomain.idAsString}/export")
        def result = parseJson(results)

        then: "the domain is exported"
        result.name == completeDomain.name
        result.elementTypeDefinitions != null
        result.riskDefinitions !=null
        result.catalogItems*.name ==~ ["c1", "c2", "c3", "scp1"]

        with(result.catalogItems.find { it.name == "scp1" }) {
            aspects.scopeRiskDefinition == "id1"
        }

        when: "we delete the riskdefinition"
        delete("/content-creation/domains/${completeDomain.idAsString}/risk-definitions/id1")

        result = parseJson(get("/domains/${completeDomain.idAsString}/export"))

        then:
        with(result.catalogItems.find { it.name == "scp1" }) {
            aspects.scopeRiskDefinition == null
        }
    }

    @WithUserDetails("user@domain.example")
    def "get all Profiles and items"() {
        when: "get the profiles"
        def result = parseJson(get("/domains/${completeDomain.idAsString}/profiles"))
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
        result = parseJson(get("/domains/${completeDomain.idAsString}/profiles/${orderedProfiles[0].id}/items"))

        then:
        result.size() == 1
        with(result[0]) {
            name == "profile-tom"
            subType == "CTL_TOM"
            elementType == "control"
        }

        when: "get a single profile item"
        result = parseJson(get("/domains/${completeDomain.idAsString}/profiles/${orderedProfiles[0].id}/items/${result[0].id}"))

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
    def "element statistics are correct for multi-domain elements"() {
        given: 'a unit with some single- and multi-domain elements'
        def client = testDomain.owner
        def unit = executeInTransaction {
            def unit = unitDataRepository.save(newUnit(client))
            personDataRepository.save(newPerson(unit) {
                associateWithDomain(testDomain, 'Team', 'NEW')
                associateWithDomain(secondDomain, 'Team', 'NEW')
            })
            personDataRepository.save(newPerson(unit) {
                associateWithDomain(testDomain, 'Employee', 'NEW')
                associateWithDomain(secondDomain, 'Member', 'NEW')
            })
            personDataRepository.save(newPerson(unit) {
                associateWithDomain(testDomain, 'Employee', 'NEW')
            })
            unit
        }

        expect: "correct status counts for both domains"
        with(parseJson(get("/domains/${testDomain.idAsString}/element-status-count?unit=${unit.idAsString}")).person) {
            size() == 2
            get('Team').NEW == 1
            get('Employee').NEW == 2
        }
        with(parseJson(get("/domains/${secondDomain.idAsString}/element-status-count?unit=${unit.idAsString}")).person) {
            size() == 2
            get('Team').NEW == 1
            get('Member').NEW == 1
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
        def result = parseJson(get("/domains/${domain.idAsString}/profiles"))

        then:
        result.size() == 1
        with(result.find { it.name == "Beispielorganisation" }) {
            description == "So wird's gemacht"
            language == "de_DE"
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

    @WithUserDetails("user@domain.example")
    def "retrieve inspections"() {
        given:
        def client = testDomain.owner
        def domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)

        when:
        def inspections = parseJson(get("/domains/${domain.idAsString}/inspections"))

        then:
        inspections.size() == 1
        inspections[0].id == "dpiaMissing"
        inspections[0].severity == "WARNING"
        inspections[0]._self != null
        inspections[0].condition == null
        inspections[0].suggestions == null

        when:
        def dpiaMissing = parseJson(get(inspections[0]._self))

        then:
        dpiaMissing.severity == "WARNING"
        dpiaMissing.description.en ==~ /Data Protection Impact Assessment was .+/
        dpiaMissing.condition.type == "and"
        dpiaMissing.condition.operands[1].type == "equals"
        dpiaMissing.suggestions[0].type == "addPart"
    }

    @WithUserDetails("user@domain.example")
    def "retrieve breaking changes"() {
        given:
        def client = testDomain.owner
        def domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)

        when:
        def breakingChanges = parseJson(get("/domains/${domain.idAsString}/breaking-changes"))

        then:
        breakingChanges.empty

        when:
        domainDataRepository.save(domain.tap {
            getElementTypeDefinition(ElementType.ASSET).tap {
                customAspects.remove('asset_details')
            }
            getElementTypeDefinition(ElementType.SCOPE).customAspects.get('scope_contactInformation').tap {
                attributeDefinitions.remove('scope_contactInformation_fax')
                def oldWebSite = attributeDefinitions.get('scope_contactInformation_website')
                attributeDefinitions.put('scope_contactInformation_website', new ExternalDocumentAttributeDefinition())
            }
        })
        breakingChanges = parseJson(get("/domains/${domain.idAsString}/breaking-changes"))

        then:
        breakingChanges.size() == 4

        when:
        def breakingChangesByAttribute = breakingChanges.collectEntries {[it.attribute, it]}

        then:
        breakingChangesByAttribute.keySet() ==~ [
            'scope_contactInformation_fax',
            'scope_contactInformation_website',
            'asset_details_number',
            'asset_details_operatingStage'
        ]
        with(breakingChangesByAttribute.scope_contactInformation_fax) {
            it.change == 'REMOVAL'
            it.elementType == 'scope'
            it.customAspect == 'scope_contactInformation'
            it.oldValue == [type: 'text']
        }
        with(breakingChangesByAttribute.scope_contactInformation_website) {
            it.change == 'MODIFICATION'
            it.elementType == 'scope'
            it.customAspect == 'scope_contactInformation'
            it.oldValue == [type: 'text']
            it.value == [type: 'externalDocument']
        }
    }
}