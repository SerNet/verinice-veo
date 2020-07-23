/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.rest

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import groovy.json.JsonSlurper

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.*
import org.veo.core.entity.ModelObject.Lifecycle
import org.veo.core.entity.custom.SimpleProperties
import org.veo.core.entity.groups.AssetGroup
import org.veo.core.entity.groups.ControlGroup
import org.veo.core.entity.groups.DocumentGroup
import org.veo.core.entity.groups.ProcessGroup
import org.veo.persistence.access.*
import org.veo.rest.configuration.WebMvcSecurityConfiguration

/**
 * Integration test for the unit controller. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
@SpringBootTest(
webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
classes = [WebMvcSecurityConfiguration]
)

@ComponentScan("org.veo.rest")
class GroupControllerMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private AssetRepositoryImpl assetRepository

    @Autowired
    private DocumentRepositoryImpl documentRepository

    @Autowired
    private ControlRepositoryImpl controlRepository

    @Autowired
    private ProcessRepositoryImpl processRepository

    @Autowired
    TransactionTemplate txTemplate

    private Unit unit
    private Unit unit2
    private Domain domain
    private Domain domain1
    private Key clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)

    def setup() {
        txTemplate.execute {
            domain = newDomain {
                name = "27001"
                description = "ISO/IEC"
                abbreviation = "ISO"
            }

            domain1 = newDomain {
                name = "27002"
                description = "ISO/IEC2"
                abbreviation = "ISO"
            }

            def client = newClient {
                id = clientId
                domains = [domain, domain1] as Set
            }
            unit = newUnit client, {
                name = "Test unit"
            }
            client.units << unit
            unit.client = client
            unit2 = newUnit client, {
                name = "Test unit2"
            }
            client.units << unit2
            unit2.client = client

            clientRepository.save(client)
        }
    }

    @WithUserDetails("user@domain.example")
    def "create an asset group"() {
        given: "a request body"

        Map request = [
            name: 'My Assets',
            type: 'Asset',
            owner: [
                href: "/units/${unit.id.uuidValue()}"
            ]
        ]

        when: "the request is sent"

        def results = post('/groups', request)

        then: "the asset group is created and a status code returned"
        results.andExpect(status().isCreated())

        and: "the location of the new asset group is returned"
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.success == true
        def resourceId = result.resourceId
        resourceId != null
        resourceId != ''
        result.message == 'Group created successfully.'
    }

    @WithUserDetails("user@domain.example")
    def "create a process group"() {
        given: "a request body"

        Map request = [
            name: 'My Processes',
            type: 'Process',
            owner: [
                href: "/units/${unit.id.uuidValue()}"
            ]
        ]

        when: "the request is sent"

        def results = post('/groups', request)

        then: "the process group is created and a status code returned"
        results.andExpect(status().isCreated())

        and: "the location of the new asset group is returned"
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.success == true
        def resourceId = result.resourceId
        resourceId != null
        resourceId != ''
        result.message == 'Group created successfully.'
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a document group"() {
        given: "a saved document group"
        DocumentGroup documentGroup = new DocumentGroup().with{
            id = Key.newUuid()
            name = 'Test document group'
            owner = unit
            state = Lifecycle.CREATING
            it
        }

        documentGroup = txTemplate.execute {
            documentRepository.save(documentGroup)
        }

        when: "the server is queried for the group"
        def results = get("/groups/${documentGroup.id.uuidValue()}?type=Document")

        then: "the document group is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'Test document group'
        result.owner.href == "/units/${unit.id.uuidValue()}"
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a control group with members"() {
        given: "a saved control group with two members"

        Control c1 = newControl  unit, {
            name = "c1"
        }

        Control c2 = newControl  unit, {
            name = "c2"
        }

        ControlGroup controlGroup = new ControlGroup().with {
            id = Key.newUuid()
            name = 'Test control group'
            owner = unit
            members = [c1, c2]
            state = Lifecycle.CREATING
            it
        }
        controlGroup = txTemplate.execute {
            controlRepository.save(controlGroup)
        }

        when: "the server is queried for the group"
        def results = get("/groups/${controlGroup.id.uuidValue()}?type=Control")

        then: "the control group is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'Test control group'
        result.owner.href == "/units/${unit.id.uuidValue()}"
        result.members.size() == 2
        result.members*.displayName as Set == ['c1', 'c2'] as Set
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a control group's members"() {
        given: "a saved control group with two members"

        Control c1 = newControl  unit, {
            name = 'c1'
        }

        Control c2 = newControl  unit, {
            name = 'c2'
        }

        ControlGroup controlGroup = new ControlGroup().with {
            id = Key.newUuid()
            name = 'Test control group'
            owner = unit
            members = [c1, c2]
            state = Lifecycle.CREATING
            it
        }
        controlGroup = txTemplate.execute {
            controlRepository.save(controlGroup)
        }

        when: "the server is queried for the group's members"
        def results = get("/groups/${controlGroup.id.uuidValue()}/members?type=Control")

        then: "the control group is found"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 2
        result.sort{it.name}.first().name == 'c1'
        result.sort{it.name}[1].name == 'c2'
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all process groups for a client"() {
        given: "two saved process grousp"
        ProcessGroup processGroup1 = new ProcessGroup().with{
            id = Key.newUuid()
            name = 'Test process group 1'
            owner = unit
            state = Lifecycle.CREATING
            it
        }
        ProcessGroup processGroup2 = new ProcessGroup().with{
            id = Key.newUuid()
            name = 'Test process group 2'
            state = Lifecycle.CREATING
            owner = unit
            it
        }

        (processGroup1, processGroup2) =
                txTemplate.execute {
                    [processGroup1, processGroup2].collect(processRepository.&save)
                }

        when: "the server is queried for the process groups"
        def results = get("/groups?type=Process")

        then: "the groups are returned"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 2

        result.sort{it.name}.first().name == 'Test process group 1'
        result.sort{it.name}.first().owner.href == "/units/${unit.id.uuidValue()}"

        result.sort{it.name}[1].name == 'Test process group 2'
        result.sort{it.name}[1].owner.href == "/units/${unit.id.uuidValue()}"
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all asset groups for a unit"() {
        given: "a saved asset"

        def assetGroup1 = new AssetGroup().with {
            id = Key.newUuid()
            name = 'Test asset group 1'
            owner = unit
            state = Lifecycle.CREATING
            it
        }

        def assetGroup2 = new AssetGroup().with {
            id = Key.newUuid()
            name = 'Test asset group 2'
            owner = unit2
            state = Lifecycle.CREATING
            it
        }

        (assetGroup1, assetGroup2) =
                txTemplate.execute {
                    [assetGroup1, assetGroup2].collect(assetRepository.&save)
                }
        when: "a request is made to the server for all assets groups of a unit"
        def results = get("/groups?parent=${unit.id.uuidValue()}&type=Asset")

        then: "the respective asset group is returned"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 1

        result.first().name == 'Test asset group 1'
        result.first().owner.href == "/units/${unit.id.uuidValue()}"

        when: "a request is made to the server for all assets groups of another unit"
        results = get("/groups?parent=${unit2.id.uuidValue()}&type=Asset")

        then: "the respective asset group is returned"
        results.andExpect(status().isOk())
        when:
        result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 1

        result.first().name == 'Test asset group 2'
        result.first().owner.href == "/units/${unit2.id.uuidValue()}"
    }

    @WithUserDetails("user@domain.example")
    def "retrieving groups works if there are also non-group entities"() {
        given: "a control and a control group"
        Control control = newControl(unit)
        ControlGroup controlGroup = new ControlGroup().tap{
            name = 'Group 1'
            owner = unit
        }

        txTemplate.execute {
            [control, controlGroup].each(controlRepository.&save)
        }

        when: "the server is queried for the control groups"
        def results = get("/groups?type=Control")

        then: "the groups are returned"
        results.andExpect(status().isOk())
        when:
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        then:
        result.size == 1
        result.first().name == controlGroup.name
    }

    @WithUserDetails("user@domain.example")
    def "put an asset group with custom properties"() {
        given: "a saved asset group"

        CustomProperties cp = new SimpleProperties()
        cp.setType("my.new.type")
        cp.setApplicableTo(['Asset'] as Set)
        cp.setId(Key.newUuid())

        Key<UUID> id = Key.newUuid()
        AssetGroup assetGroup = new AssetGroup().with {
            setId(id)
            name = 'Test asset group'
            setOwner(unit)
            customAspects = [cp]
            domains = [domain1]
            state = Lifecycle.CREATING
            it
        }

        assetGroup = txTemplate.execute {
            assetRepository.save(assetGroup)
        }

        Map request = [
            id: id.uuidValue(),
            name: 'New asset group-2',
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                href: "/units/${unit.id.uuidValue()}",
                displayName: 'test unit'
            ], domains: [
                [
                    href: "/domains/${domain.id.uuidValue()}",
                    displayName: 'test ddd'
                ]
            ], customAspects:
            [
                'my.aspect-test' :
                [
                    id: '00000000-0000-0000-0000-000000000000',
                    type : 'my.aspect-test1',
                    applicableTo: [
                        "Asset"
                    ],
                    domains: [],
                    attributes:  [
                        test1:'value1',
                        test2:'value2'
                    ]
                ]
            ]
        ]

        when: "the new group data is sent to the server"
        def results = put("/groups/${id.uuidValue()}?type=Asset",request)

        then: "the group is found"
        results.andExpect(status().isOk())
        def result = new JsonSlurper().parseText(results.andReturn().response.contentAsString)
        result.name == 'New asset group-2'
        result.abbreviation == 'u-2'
        result.domains.first().displayName == "${domain.abbreviation} ${domain.name}"
        result.owner.href == "/units/${unit.id.uuidValue()}"
        def entity = txTemplate.execute { assetRepository.findById(id).get() }
        entity.name == 'New asset group-2'
        entity.abbreviation == 'u-2'
        entity.customAspects.first().type == 'my.aspect-test1'
        entity.customAspects.first().applicableTo == ['Asset'] as Set
        entity.customAspects.first().stringProperties.test1 == 'value1'
        entity.customAspects.first().stringProperties.test2 == 'value2'
    }

    @WithUserDetails("user@domain.example")
    def "delete an asset group"() {

        given: "an existing asset group"
        AssetGroup assetGroup = new AssetGroup().with {
            setId(Key.newUuid())
            name = 'Test asset-delete'
            owner = unit
            setDomains([domain1] as Set)
            state = Lifecycle.CREATING
            it
        }

        assetGroup = txTemplate.execute {
            assetRepository.save(assetGroup)
        }

        when: "a delete request is sent to the server"

        def results = delete("/groups/${assetGroup.id.uuidValue()}?type=Asset")

        then: "the asset group is deleted"
        results.andExpect(status().isOk())
        assetRepository.findById(assetGroup.id).empty
    }
}