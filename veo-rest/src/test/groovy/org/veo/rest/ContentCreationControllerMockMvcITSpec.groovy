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
import org.springframework.web.bind.MethodArgumentNotValidException

import org.veo.core.entity.Catalog
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.exception.EntityAlreadyExistsException
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.exception.UnprocessableDataException
import org.veo.core.usecase.domain.DomainInUseException
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.jpa.DomainTemplateDataRepository

import groovy.json.JsonSlurper

/**
 * Integration test for the content creation controller. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
class ContentCreationControllerMockMvcITSpec extends ContentSpec {

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
                        elementType = "control"
                        subType = "CTL_TOM"
                        status = "NEW"
                        name = 'c1'
                    })
                    newCatalogItem(c,{
                        elementType = "control"
                        subType = "CTL_TOM"
                        status = "NEW"
                        name = 'c2'
                    })
                    newCatalogItem(c,{
                        elementType = "control"
                        subType = "CTL_TOM"
                        status = "NEW"
                        name = 'c3'
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

    @WithUserDetails("content-creator")
    def "create and delete a domain"() {
        when: "a request is made to the server"
        def result = post("/content-creation/domains", [name:'myd1',authority:"myAuthority"], 201)

        then:
        result.andReturn().response.status == 201

        when:"get the domains"
        def domainId = parseJson(result).resourceId
        result = get("/domains/${domainId}")

        then:
        with(parseJson(result)) {
            name == 'myd1'
            authority == 'myAuthority'
        }

        when:
        result = delete("/content-creation/domains/${domainId}", 204)

        then:
        result.andReturn().response.status == 204

        when:
        def client = txTemplate.execute {
            clientRepository.findById(client.id).get()
        }

        then:
        client.getDomains().size() == 3

        when:"get the domains"
        result = get("/domains/${domainId}",404)

        then:
        thrown(NotFoundException)

        when: "a domain is created"
        result = post("/content-creation/domains", [name:'myd1',authority:"myAuthority"], 201)
        domainId = parseJson(result).resourceId

        then:
        result.andReturn().response.status == 201

        when: "create a unit using the the domain"
        def unitId = parseJson(post("/units", [
            name   : "you knit",
            domains: [
                [targetUri: "http://localhost/domains/$domainId"]
            ]
        ])).resourceId

        and:
        delete("/content-creation/domains/${domainId}", 409)

        then:
        thrown(DomainInUseException)

        when:
        delete("/units/${unitId}")

        and:
        delete("/content-creation/domains/${domainId}", 204)
        get("/domains/${domainId}",404)

        then:
        thrown(NotFoundException)
    }

    @WithUserDetails("content-creator")
    def "update the element type schema in a domain with an object schema"() {
        given:
        def schemaJson = DomainControllerMockMvcITSpec.getResourceAsStream('/os_scope.json').withCloseable {
            new JsonSlurper().parse(it)
        }

        when: "a request is made to the server"
        def result = post("/content-creation/domains/${testDomain.id.uuidValue()}/element-type-definitions/scope/object-schema", schemaJson, 204)

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
    def "trying to update the element type schema with incomplete data results in a HTTP 400"() {
        given:
        def schemaJson = [:]

        when:
        def result = post("/content-creation/domains/${testDomain.id.uuidValue()}/element-type-definitions/scope/object-schema", schemaJson, 400)

        then:
        thrown(IllegalArgumentException)
    }

    @WithUserDetails("content-creator")
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
        def result = put("/content-creation/domains/${testDomain.id.uuidValue()}/element-type-definitions/scope", schemaJson, 204)

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
    def "invalid attribute names are rejected"() {
        when: "updating the scope definition with space in attribute name is rejected"
        put("/content-creation/domains/${testDomain.idAsString}/element-type-definitions/scope", [
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
        def status = postUnauthorized("/content-creation/domains/${testDomain.id.uuidValue()}/elementtypedefinitions/scope/object-schema", schemaJson)

        then: "it is forbidden"
        status.andReturn().response.status == 403
    }

    @WithUserDetails("user@domain.example")
    def "cannot update element type definition as regular user"() {
        when:
        def response = putUnauthorized("/content-creation/domains/${testDomain.id.uuidValue()}/element-type-definitions/scope", [:])

        then:
        response.andReturn().response.status == 403
    }

    @WithUserDetails("content-creator")
    def "create a DomainTemplate"() {
        given: "a number of existing templates"
        def initialTemplateCount = txTemplate.execute {
            domainTemplateDataRepository.count()
        }

        when: "a template is created"
        def result = parseJson(post("/content-creation/domains/${testDomain.id.uuidValue()}/template",[version : "1.0.0"]))

        then: "a result is returned"
        result != null

        and: "there is one more template in the repo"
        domainTemplateDataRepository.count() == initialTemplateCount + 1

        when: "loading the domaintemplates from the database"
        def dt = txTemplate.execute {
            domainTemplateRepository.findAll().find{ it.name == "Domain 1" }
        }

        then: "the version is set"
        dt.templateVersion == "1.0.0"

        when: "trying to create another domain template with the same version"
        post("/content-creation/domains/${testDomain.id.uuidValue()}/template", [version : "1.0.0"], 409)

        then:
        thrown(EntityAlreadyExistsException)

        when: "trying to create another domain template with a lower version"
        post("/content-creation/domains/${testDomain.id.uuidValue()}/template", [version : "0.5.3"], 422)

        then:
        thrown(UnprocessableDataException)

        when: "trying to create another domain template with an invalid version"
        post("/content-creation/domains/${testDomain.id.uuidValue()}/template", [version : "1.1"], 400)

        then:
        thrown(MethodArgumentNotValidException)

        when: "trying to create another domain template with a prerelease label"
        post("/content-creation/domains/${testDomain.id.uuidValue()}/template", [version : "1.0.1-prerelease3"], 400)

        then:
        thrown(MethodArgumentNotValidException)

        when: "trying to create another domain template with a higher version"
        post("/content-creation/domains/${testDomain.id.uuidValue()}/template", [version : "1.0.1"])

        then:
        notThrown(Exception)
    }

    @WithUserDetails("content-creator")
    def "create a DomainTemplate with unit"() {
        Domain domain = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
        def (unitId, assetId, scenarioId, processId) = createUnitWithElements(domain.idAsString)

        given: "a number of existing templates"
        def initialTemplateCount = txTemplate.execute {
            domainTemplateDataRepository.count()
        }

        when: "a template is created"
        def result = parseJson(post("/content-creation/domains/${domain.id.uuidValue()}/template", [
            version : "1.2.3",
            profiles: [
                exampleOrganization: [
                    unitId: unitId,
                    name: 'Example elements',
                    description: 'All the good stuff',
                    language: 'de_DE'
                ]
            ]
        ]))

        then: "a result is returned"
        result != null

        and: "there is one more template in the repo"
        domainTemplateDataRepository.count() == initialTemplateCount + 1

        when: "loading the domaintemplates from the database"
        def dt = txTemplate.execute {
            domainTemplateRepository.findAll()
                    .find{ it.name == domain.name && it.templateVersion == "1.2.3"}
                    .tap{it.profiles  } // init proxy
        }

        then: "the template is found, the version is set"
        dt.templateVersion == "1.2.3"

        and: "the example profile exists"
        with(dt.profiles.exampleOrganization) {
            name == 'Example elements'
            description == 'All the good stuff'
            language == 'de_DE'

            elements != null
            risks != null

            elements*.type.sort() == [
                "asset",
                "control",
                "document",
                "incident",
                "person",
                "process",
                "scenario",
                "scope"
            ]
            risks*._self.size() == 2
        }

        when: "creating and exporting the domain"
        Domain newDomain = createTestDomain(client, dt.id.uuidValue())
        def results = get("/domains/${newDomain.id.uuidValue()}/export")
        def exportedDomain = parseJson(results)

        then:" the export file contains the profile data"
        exportedDomain.name == newDomain.name
        exportedDomain.profiles.exampleOrganization.elements*.type ==~ [
            "asset",
            "control",
            "document",
            "incident",
            "person",
            "process",
            "scenario",
            "scope"
        ]
        exportedDomain.profiles.exampleOrganization.risks.size() == 2

        when: "we create a new domain template from the export"
        exportedDomain.templateVersion = "1.2.4"
        def domainTemplateId = parseJson(post("/domaintemplates", exportedDomain)).resourceId

        then:" the domain template is created"
        UUID.fromString(domainTemplateId)

        when: "we create a domain from the domain template"
        newDomain = createTestDomain(client, domainTemplateId)

        then: "the domain contains the profiles"
        newDomain.name == "DSGVO-test"
        newDomain.profiles.exampleOrganization.elements*.type ==~ [
            "asset",
            "control",
            "document",
            "incident",
            "person",
            "process",
            "scenario",
            "scope"
        ]
        newDomain.profiles.exampleOrganization.risks.size() == 2
    }

    @WithUserDetails("content-creator")
    def "Profile metadata are optional"() {
        given:
        Domain domain = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
        def (unitId, assetId, scenarioId, processId) = createUnitWithElements(domain.idAsString)

        when: "a template is created"
        def result = parseJson(post("/content-creation/domains/${domain.id.uuidValue()}/template", [
            version : "1.2.3",
            profiles: [
                exampleOrganization: [
                    unitId: unitId
                ]
            ]
        ]))

        then: "a result is returned"
        noExceptionThrown()
    }

    @WithUserDetails("user@domain.example")
    def "create a DomainTemplate forbidden for user"() {
        given: "a saved domain"

        when: "a request is made to the server"
        def status = postUnauthorized("/content-creation/domains/${testDomain.id.uuidValue()}/template", [version : "1.0.0"])

        then: "it is forbidden"
        status.andReturn().response.status == 403
    }
}