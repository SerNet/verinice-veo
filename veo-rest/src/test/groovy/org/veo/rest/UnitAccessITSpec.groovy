/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Urs Zeidler
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

import org.veo.core.TestUserRights
import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
import org.veo.core.entity.EntityType
import org.veo.core.entity.Unit
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.specification.NotAllowedException
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.GenericElementRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.access.jpa.ProfileDataRepository
import org.veo.rest.security.CustomUserDetailsManager

@WithUserDetails("user@domain.example")
class UnitAccessITSpec extends VeoMvcSpec{
    @Autowired
    ClientRepositoryImpl clientRepository

    @Autowired
    UnitRepositoryImpl unitRepository
    @Autowired
    GenericElementRepositoryImpl elementRepository

    @Autowired
    TransactionTemplate txTemplate

    @Autowired
    CustomUserDetailsManager userDetailsService

    @Autowired
    ProfileDataRepository profileRepository

    Client client
    Unit unit
    UUID unitId
    Domain domain
    String domainId
    UUID profileId

    def setup() {
        client = createTestClient()
        domain = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
        domainId = domain.idAsString
        unit = unitRepository.save(newUnit(client) {
            name = "restricted unit"
            domains = [domain]
        })
        unitId = unit.id

        profileId = txTemplate.execute {
            profileRepository.save(newProfile(domain) {
                def item1 = newProfileItem(it) {
                    name = "target"
                    elementType = ElementType.ASSET
                    subType = "AST_Application"
                    status = "NEW"
                }
                def item2 = newProfileItem(it) {
                    name = "source"
                    elementType = ElementType.ASSET
                    subType = "AST_Application"
                    status = "NEW"
                }
                items = [
                    item1,
                    item2
                ]
            }).id
        }
    }

    def "read access allowed for #rights"() {
        given:
        updateUser("user@domain.example", rights, unitId)

        when: "get all units"
        def ret = parseJson(get("/units", 200))

        then: "the unit is found"
        ret.size() == 1
        ret[0].id == unitId.toString()

        when: "get all units"
        ret = parseJson(get("/units/$unitId", 200))

        then: "the unit is found"
        ret.id == unitId.toString()

        when: "we are allowed to get the status"
        get("/domains/${domain.idAsString}/element-status-count?unit=${unit.idAsString}", 200)

        then:
        noExceptionThrown()

        where:
        rights << [
            new TestUserRights(restrictUnitAccess: false),
            new TestUserRights(restrictUnitAccess: true, accessAllUnits:  true),
            new TestUserRights(restrictUnitAccess: true, testUnitReadable:  true)
        ]
    }

    def "read access denied with #rights"() {
        given:
        updateUser("user@domain.example", rights, unitId)

        when: "get all units"
        def ret = parseJson(get("/units", 200))

        then: "the unit is found"
        ret.size() == 0

        when: "get a unit"
        parseJson(get("/units/$unitId", 404))

        then: "the unit is found"
        thrown(NotFoundException)

        when: "get a unit export"
        parseJson(get("/units/$unitId/export", 404))

        then: "the unit is found"
        thrown(NotFoundException)

        when: "we are not allowed to change the unit"
        get("/units/$unitId", 404).with{
            def body = parseJson(it)
            body.name = "new name"
            put(body._self, body, ["If-Match": getETag(it)], 404)
        }

        then:
        thrown(NotFoundException)

        when: "we are not allowed to get the status"
        get("/domains/${domain.idAsString}/element-status-count?unit=${unit.idAsString}", 404)

        then:
        thrown(NotFoundException)

        when: "we can't incarnate in the unit"
        get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=387b0990-10f7-4ef2-8ee5-13579f2cfe84", 404)

        then:
        thrown(NotFoundException)

        when: "post a incarnation description to the unit"
        post("/units/$unit.idAsString/incarnations",
                ["parameters": []]
                ,404)

        then:
        thrown(NotFoundException)

        when:
        post("/domains/$domainId/profiles/${profileId}/incarnation?unit=$unit.idAsString",
                ["parameters": []]
                ,404)

        then:
        thrown(NotFoundException)

        when: "not allowed create a new unit"
        post("/units",[
            name : 'My CRUD unit',
        ], 403)

        then:
        thrown(NotAllowedException)

        when: "not allowed to delete"
        delete("/units/$unitId", 404)

        then:
        thrown(NotFoundException)

        where:
        rights << [
            new TestUserRights(restrictUnitAccess: true)
        ]
    }

    def "write access allowed for with #rights"() {
        given:
        updateUser("user@domain.example", rights, unitId)

        when: "we can incanate in the unit"
        parseJson(post("/units/$unit.idAsString/incarnations",
                parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=387b0990-10f7-4ef2-8ee5-13579f2cfe84"))
                ))

        and: "also a profile"
        post("/domains/$domainId/profiles/${profileId}/incarnation?unit=$unit.idAsString",
                ["parameters": []]
                ,204)

        then:
        noExceptionThrown()

        where:
        rights << [
            new TestUserRights(restrictUnitAccess: false),
            new TestUserRights(restrictUnitAccess: true, accessAllUnits:  true),
            new TestUserRights(restrictUnitAccess: true, testUnitReadable:  true, testUnitWritable: true)
        ]
    }

    def "update allowed for with #rights and roles #additonalRoles"() {
        given:
        updateUser("user@domain.example", rights, unitId, additonalRoles)

        when: "we are allowed to change the unit"
        get("/units/$unitId").with{
            def body = parseJson(it)
            body.name = "new name"
            put(body._self, body, ["If-Match": getETag(it)], 200)
        }

        and: "we can incanate in the unit"
        parseJson(post("/units/$unit.idAsString/incarnations",
                parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=387b0990-10f7-4ef2-8ee5-13579f2cfe84"))
                ))

        and: "also a profile"
        post("/domains/$domainId/profiles/${profileId}/incarnation?unit=$unit.idAsString",
                ["parameters": []]
                ,204)

        then:
        noExceptionThrown()

        where:
        [rights, additonalRoles] << [
            [
                new TestUserRights(restrictUnitAccess: false),
                []
            ],
            [
                new TestUserRights(restrictUnitAccess: true, accessAllUnits:  true),
                []
            ],
            [
                new TestUserRights(restrictUnitAccess: true, testUnitReadable:  true, testUnitWritable: true),
                ["unit:update"]
            ]
        ]
    }

    def "delete allowed for with #rights and roles #additonalRoles"() {
        given:
        def user = updateUser("user@domain.example", rights, unitId, additonalRoles)

        when: "allowed to delete"
        delete("/units/$unitId", 204)

        then:
        noExceptionThrown()

        where:
        [rights, additonalRoles] << [
            [
                new TestUserRights(restrictUnitAccess: false),
                []
            ],
            [
                new TestUserRights(restrictUnitAccess: true, accessAllUnits:  true),
                []
            ],
            [
                new TestUserRights(restrictUnitAccess: true, testUnitReadable:  true, testUnitWritable: true),
                ["unit:delete"]
            ]
        ]
    }

    def "create allowed for with #rights and roles #additonalRoles"() {
        given:
        updateUser("user@domain.example", rights, unitId, additonalRoles)

        when: "create a new unit"
        post("/units",[
            name : 'My CRUD unit',
        ], 201)

        then:
        noExceptionThrown()

        where:
        [rights, additonalRoles] << [
            [
                new TestUserRights(restrictUnitAccess: false),
                []
            ],
            [
                new TestUserRights(restrictUnitAccess: true, accessAllUnits:  true),
                []
            ],
            [
                new TestUserRights(restrictUnitAccess: true, testUnitReadable:  true, testUnitWritable: true),
                ["unit:create"]
            ]
        ]
    }

    def "all allowed for with #rights and roles #additonalRoles"() {
        given:
        updateUser("user@domain.example", rights, unitId, additonalRoles)

        when: "create a new unit"
        post("/units",[
            name : 'My CRUD unit',
        ], 201)

        then:
        noExceptionThrown()

        when: "we are allowed to change the unit"
        get("/units/$unitId").with{
            def body = parseJson(it)
            body.name = "new name"
            put(body._self, body, ["If-Match": getETag(it)], 200)
        }

        then:
        noExceptionThrown()

        when: "allowed to delete"
        delete("/units/$unitId", 204)

        then:
        noExceptionThrown()

        where:
        [rights, additonalRoles] << [
            [
                new TestUserRights(restrictUnitAccess: false),
                []
            ],
            [
                new TestUserRights(restrictUnitAccess: true, accessAllUnits:  true),
                []
            ],
            [
                new TestUserRights(restrictUnitAccess: true, testUnitReadable:  true, testUnitWritable: true),
                [
                    "unit:create",
                    "unit:delete",
                    "unit:update"
                ]
            ]
        ]
    }
}
