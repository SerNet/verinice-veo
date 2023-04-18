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

import java.nio.charset.StandardCharsets

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.Catalog
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.exception.UnprocessableDataException
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.jpa.DomainTemplateDataRepository

import groovy.json.JsonSlurper

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
    private DomainTemplateDataRepository domainTemplateRepository

    @Autowired
    TransactionTemplate txTemplate

    private Domain testDomain
    private Domain completeDomain
    private Domain secondDomain
    private Catalog catalog
    private Domain domainSecondClient
    private Client client

    def setup() {
        txTemplate.execute {
            def rd = createRiskDefinition("id1")

            this.client = createTestClient()
            newDomain(client) {
                name = "Domain 1"
                newCatalog(it) {
                    name = 'a'
                }
            }
            newDomain(client) {
                name = "Domain 2"
            }
            newDomain(client) { d->
                name = "Domain-complete"
                newCatalog(d) {c->
                    name = 'a'
                    newCatalogItem(c,{
                        newControl(it) {
                            name = 'c1'
                            it.associateWithDomain(d, "CTL_Control", 'NEW')
                        }
                    })
                    newCatalogItem(c,{
                        newControl(it) {
                            name = 'c2'
                            it.associateWithDomain(d, "CTL_Control", 'NEW')
                        }
                    })
                    newCatalogItem(c,{
                        newControl(it) {
                            name = 'c3'
                            it.associateWithDomain(d, "CTL_Control", 'NEW')
                        }
                    })
                }
                riskDefinitions = ["id":rd] as Map
            }

            client = clientRepository.save(client)

            testDomain = client.domains.find{it.name == "Domain 1"}
            completeDomain = client.domains.find{it.name == "Domain-complete"}
            secondDomain = client.domains.find{it.name == "Domain 2"}
            catalog = testDomain.catalogs.first()

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
        result.catalogs.size() == 1
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
        with(result.decisions.piaMandatory) {
            name.en == "Data Protection Impact Assessment mandatory"
            elementSubType == "PRO_DataProcessing"
            rules[5].description.en == "Processing on list of the kinds of processing operations subject to a Data Protection Impact Assessment"
            rules[5].conditions[0].inputProvider.type == "customAspectAttributeValue"
            rules[5].conditions[0].inputProvider.customAspect == "process_privacyImpactAssessment"
            rules[5].conditions[0].inputProvider.attribute == "process_privacyImpactAssessment_listed"
            rules[5].conditions[0].inputMatcher.type == "equals"
            rules[5].conditions[0].inputMatcher.comparisonValue == "process_privacyImpactAssessment_listed_positive"
        }

        when:
        def firstCatalog = result.catalogs.first()

        then:
        firstCatalog.displayName == 'a'
        firstCatalog.targetUri == "http://localhost/catalogs/${catalog.dbId}"
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

    @WithUserDetails("content-creator")
    // TODO VEO-2000: remove this test
    def "update the element type schema in a domain with an object schema"() {
        given:
        def schemaJson = DomainControllerMockMvcITSpec.getResourceAsStream('/os_scope.json').withCloseable {
            new JsonSlurper().parse(it)
        }

        when: "a request is made to the server"
        def result = post("/domains/${testDomain.id.uuidValue()}/elementtypedefinitions/scope/updatefromobjectschema", schemaJson, 204)

        then: "the domains are returned"
        result.andReturn().response.getContentAsString(StandardCharsets.UTF_8) == ''

        when: 'reloading the updated domain from the database'
        def updatedDomain = txTemplate.execute {
            def client = clientRepository.findById(testDomain.owningClient.get().id).get()
            def d = client.domains.find{it.id == testDomain.id}
            //initialize lazy associations
            d.elementTypeDefinitions.each {
                it.subTypes.each {
                    it.value.statuses
                }
            }
            d
        }

        then: 'the entity schemas are updated'
        with(updatedDomain.getElementTypeDefinition('scope')) {
            with(it.subTypes) {
                it.keySet() == [
                    'SCP_Scope',
                    'SCP_Processor',
                    'SCP_Controller',
                    'SCP_JointController',
                    'SCP_ResponsibleBody'
                ] as Set
            }
            with(it.translations) {
                it.size() == 2
                with(it.get(Locale.forLanguageTag("en"))) {
                    it.scope_management == 'Head of the responsible body'
                }
            }
        }
    }

    @WithUserDetails("content-creator")
    // TODO VEO-2000: remove this test
    def "update an element type definition in a domain"() {
        given:
        def schemaJson = [
            subTypes:[
                SCP_Container:[
                    statuses:['Empty', 'Full']
                ]
            ],
            customAspects:[
                container_lid:[
                    attributeDefinitions: [
                        container_lid_present : [type: 'boolean']
                    ]
                ]
            ],
            links:[
                container_owner:[
                    targetType: 'person'
                ]
            ],
            translations:[
                en:[
                    scope_SCP_Container_status_Empty: 'Empty',
                    scope_SCP_Container_status_Full: 'Full',
                    container_lid_present: 'Lid present?',
                    container_owner: 'Owner'
                ]
            ],
        ]

        when: "updating the scope definition"
        def result = put("/domains/${testDomain.id.uuidValue()}/element-type-definitions/scope", schemaJson, 204)

        and: 'reloading the updated domain from the database'
        def updatedDomain = txTemplate.execute {
            def client = clientRepository.findById(testDomain.owningClient.get().id).get()
            def d = client.domains.find{it.id == testDomain.id}
            //initialize lazy associations
            d.elementTypeDefinitions.each {
                it.subTypes.each {
                    it.value.statuses
                }
            }
            d
        }

        then: 'the entity schemas are updated'
        with(updatedDomain.getElementTypeDefinition('scope')) {
            with(it.subTypes) {
                it.keySet() ==~ [
                    'SCP_Container'
                ]
            }
            with(it.translations) {
                it.size() == 1
                with (it[Locale.ENGLISH]) {
                    it.scope_SCP_Container_status_Empty == 'Empty'
                }
            }
        }
    }

    @WithUserDetails("content-creator")
    // TODO VEO-2000: remove this test
    def "invalid attribute names are rejected"() {
        when: "updating the scope definition with space in attribute name is rejected"
        put("/domains/${testDomain.idAsString}/element-type-definitions/scope", [
            subTypes: [
                SCP_Container: [
                    statuses: ['Empty']
                ]
            ],
            customAspects: [
                container_lid: [
                    attributeDefinitions: [
                        'container_lid present': [type: 'boolean']
                    ]
                ]
            ],
            translations: [
                en: [
                    scope_SCP_Container_status_Empty: 'Empty',
                    'container_lid present': 'Lid present?',
                ]
            ],
        ], 422)

        then:
        UnprocessableDataException ex = thrown()
        ex.message ==~ /Invalid key 'container_lid present' - .*/
    }

    @WithUserDetails("user@domain.example")
    def "cannot update element type schema as regular user"() {
        given:
        def schemaJson = DomainControllerMockMvcITSpec.getResourceAsStream('/os_scope.json').withCloseable {
            new JsonSlurper().parse(it)
        }

        when: "a request is made to the server"
        def status = postUnauthorized("/domains/${testDomain.id.uuidValue()}/elementtypedefinitions/scope/updatefromobjectschema", schemaJson)

        then: "it is forbidden"
        status.andReturn().response.status == 403
    }

    @WithUserDetails("user@domain.example")
    def "cannot update element type definition as regular user"() {
        when:
        def response = putUnauthorized("/domains/${testDomain.id.uuidValue()}/element-type-definitions/scope", [:])

        then:
        response.andReturn().response.status == 403
    }

    @WithUserDetails("user@domain.example")
    def "export a Domain"() {
        given: "a saved domain"

        when: "a request is made to the server"
        def results = get("/domains/${completeDomain.id.uuidValue()}/export")
        def result = parseJson(results)

        then: "the domain is exported"
        result.name == completeDomain.name
        result.catalogs.size() == 1
        result.elementTypeDefinitions != null
        result.riskDefinitions !=null

        when:
        def firstCatalog = result.catalogs.first()

        then:
        with(firstCatalog) {
            name == 'a'
            catalogItems.size() == 3
            catalogItems[0].element.domains.keySet() ==~ [result.id]
            domainTemplate !=null
        }
    }

    @WithUserDetails("user@domain.example")
    // TODO VEO-2000: remove this test
    def "create a DomainTemplate forbidden for user"() {
        given: "a saved domain"

        when: "a request is made to the server"
        def status = postUnauthorized("/domains/${testDomain.id.uuidValue()}/createdomaintemplate", [version : "1.0.0"])

        then: "it is forbidden"
        status.andReturn().response.status == 403
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
    def "retrieve demo profile metadata"() {
        given:
        def client = testDomain.owner
        def domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)

        when:
        def result = parseJson(get("/domains/${domain.idAsString}"))

        then:
        result.profiles.size() == 1
        with(result.profiles.demoUnit) {
            name == 'Demo'
            description == 'Beispieldaten für den Datenschutz'
            language == 'de_DE'
        }
    }
}