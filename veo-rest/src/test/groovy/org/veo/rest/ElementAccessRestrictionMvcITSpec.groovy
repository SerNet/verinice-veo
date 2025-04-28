/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Urs Zeidler.
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

import static org.springframework.http.MediaType.APPLICATION_JSON

import java.util.stream.Stream

import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.TestUserRights
import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Element
import org.veo.core.entity.ElementType
import org.veo.core.entity.Unit
import org.veo.core.entity.definitions.LinkDefinition
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.specification.NotAllowedException

@WithUserDetails("user@domain.example")
class ElementAccessRestrictionMvcITSpec extends VeoMvcSpec {
    Client client
    Unit unit
    UUID unitId
    Domain domain
    String domainId

    def setup() {
        client = createTestClient()
        domain = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
        Stream.of(ElementType.values()).forEach { et ->
            def s = ElementType.DOCUMENT.getSingularTerm()
            def ed = domain.getElementTypeDefinition(et)
            ed.getLinks().put("dok", new LinkDefinition().tap {
                targetType = ElementType.DOCUMENT
                targetSubType = "DOC_Document"
            })
        }
        domain = domainDataRepository.save(domain)
        domainId = domain.idAsString
        client = clientRepository.getById(client.id)
        unit = unitDataRepository.save(newUnit(client) {
            name = "restricted unit"
        })
        unitId = unit.id
    }

    def "read access allowed for #type with #rights"() {
        given:
        updateUser("user@domain.example", rights, unitId)

        and:
        Element element = saveNewElement(type, unit, domain)
        def elementId = element.idAsString
        def elementType = type.pluralTerm

        expect: "that elements can be fetched (domain context)"
        with(parseJson(get("/domains/$domainId/$elementType", 200, APPLICATION_JSON))) {
            totalItemCount == 1
        }

        and: "elements can be fetched"
        with(parseJson(get("/$elementType", 200))) {
            totalItemCount == 1
        }

        when: "trying to fetch the element (domain context)"
        get("/domains/$domainId/$elementType/$elementId", 200)

        then:
        noExceptionThrown()

        when: "trying to fetch the element"
        get("/$elementType/$elementId", 200)

        then:
        noExceptionThrown()

        when: "fetching the element's links"
        get("/domains/$domainId/$elementType/$elementId/links", 200)

        then:
        noExceptionThrown()

        when: "inspecting the element"
        get("/$elementType/$elementId/inspection?domain=$domainId", 200)

        then:
        noExceptionThrown()

        when: "fetching parts / members"
        if (type == ElementType.SCOPE) {
            get("/$elementType/$elementId/members", 200)
        } else {
            get("/$elementType/$elementId/parts", 200)
        }

        then:
        noExceptionThrown()

        when: "fetching parts / members (domain context)"
        if (type == ElementType.SCOPE) {
            get("/domains/$domainId/$elementType/$elementId/members", 200)
        } else {
            get("/domains/$domainId/$elementType/$elementId/parts", 200)
        }

        then:
        noExceptionThrown()

        when: "evaluating the existing element (legacy endpoint)"
        post("/$elementType/evaluation?domain=$domainId", [
            id: elementId,
            name: 'My CRUD element',
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ]
        ], 200)

        then:
        noExceptionThrown()

        when: "evaluating the existing element"
        post("/domains/$domainId/$elementType/evaluation", [
            id: elementId,
            name: 'My CRUD element',
            subType: element.getSubType(domain),
            status: "NEW",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ]
        ], 200)

        then:
        noExceptionThrown()

        where:
        [type, rights] << ElementType.values().collectMany {
            [
                [type: it, rights: new TestUserRights(restrictUnitAccess: false)],
                [type: it, rights: new TestUserRights(restrictUnitAccess: true, accessAllUnits:  true)],
                [type: it, rights: new TestUserRights(restrictUnitAccess: true, testUnitReadable:  true)],
            ]
        }
    }

    def "read access forbidden for #type with #rights"() {
        given:
        updateUser("user@domain.example", rights, unitId)

        and:
        Element element = saveNewElement(type, unit, domain)
        def elementId = element.idAsString
        def elementType = type.pluralTerm

        expect: "that the element is hidden in the list (domain context)"
        with(parseJson(get("/domains/$domainId/$elementType", 200))) {
            totalItemCount == 0
        }

        and: "that the element is hidden in the list"
        with(parseJson(get("/$elementType", 200))) {
            totalItemCount == 0
        }

        when: "trying to fetch the element"
        get("/$elementType/$elementId", 404)

        then:
        thrown(NotFoundException)

        when: "trying to fetch the element (domain context)"
        get("/domains/$domainId/$elementType/$elementId", 404)

        then:
        thrown(NotFoundException)

        when: "trying to fetch the element's links"
        get("/domains/$domainId/$elementType/$elementId/links", 404)

        then:
        thrown(NotFoundException)

        when: "inspecting the element"
        get("/$elementType/$elementId/inspection?domain=$domainId", 404)

        then:
        thrown(NotFoundException)

        when: "trying to fetch parts / members"
        if (type == ElementType.SCOPE) {
            get("/$elementType/$elementId/members", 404)
        } else {
            get("/$elementType/$elementId/parts", 404)
        }

        then:
        thrown(NotFoundException)

        when: "trying to fetch parts / members (domain context)"
        if (type == ElementType.SCOPE) {
            get("/domains/$domainId/$elementType/$elementId/members", 404)
        } else {
            get("/domains/$domainId/$elementType/$elementId/parts", 404)
        }

        then:
        thrown(NotFoundException)

        when: "trying to evaluate the existing element (legacy endpoint)"
        post("/$elementType/evaluation?domain=$domainId", [
            id: elementId,
            name: 'My CRUD element',
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ]
        ], 404)

        then:
        thrown(NotFoundException)

        when: "trying to evaluate the existing element"
        post("/domains/$domainId/$elementType/evaluation", [
            id: elementId,
            name: 'My CRUD element',
            subType: element.getSubType(domain),
            status: "NEW",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ]
        ], 404)

        then:
        thrown(NotFoundException)

        where:
        [type, rights] << ElementType.values().collectMany {
            [
                [type: it, rights: new TestUserRights(restrictUnitAccess: true)],
                [type: it, rights: new TestUserRights(restrictUnitAccess: true, testUnitWritable: true)],
            ]
        }
    }

    def "write access allowed for #type with #rights"() {
        given:
        updateUser("user@domain.example", rights, unitId)

        and:
        Element element = saveNewElement(type, unit, domain)
        def elementId = element.idAsString
        def elementType = type.pluralTerm

        when: "updating the element"
        get("/domains/$domainId/$elementType/$elementId").with {
            def body = parseJson(it)
            body.name = "new name"
            put(body._self, body, ["If-Match": getETag(it)], 200)
        }

        then:
        noExceptionThrown()

        when: "creating an element"
        post("/domains/$domainId/$elementType", [
            name: 'My CRUD element',
            subType: element.getSubType(domain),
            status: "NEW",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ]
        ], 201)

        then:
        noExceptionThrown()

        when: "adding a link"
        def target = saveNewElement(ElementType.DOCUMENT, unit, domain)
        post("/domains/$domainId/$elementType/$elementId/links", [
            dok: [
                [target: [targetUri: "/documents/${target.idAsString}"]],
            ]
        ], 204)

        then:
        noExceptionThrown()

        when: "deleting the element"
        delete("/$elementType/$elementId", 204)

        then:
        noExceptionThrown()

        where:
        [type, rights] << ElementType.values().collectMany {
            [
                [type: it, rights: new TestUserRights(restrictUnitAccess: false, )],
                [type: it, rights: new TestUserRights(restrictUnitAccess: true, testUnitReadable: true, testUnitWritable: true)],
                [type: it, rights: new TestUserRights(restrictUnitAccess: true, accessAllUnits: true)],
            ]
        }
    }

    def "write access forbidden for #type with #rights"() {
        given:
        updateUser("user@domain.example", rights, unitId)

        and:
        Element element = saveNewElement(type, unit, domain)
        def elementId = element.idAsString
        def elementType = type.pluralTerm

        when: "trying to update the element"
        get("/domains/$domainId/$elementType/$elementId").with {
            def body = parseJson(it)
            body.name = "new name"
            put(body._self, body, ["If-Match": getETag(it)], 403)
        }

        then:
        thrown(NotAllowedException)

        when: "trying to create another element"
        post("/domains/$domainId/$elementType", [
            name: 'My CRUD process',
            subType: element.getSubType(domain),
            status: "NEW",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ]
        ], 403)

        then:
        thrown(NotAllowedException)

        when: "trying to add a link"
        def target = saveNewElement(ElementType.DOCUMENT, unit, domain)
        post("/domains/$domainId/$elementType/$elementId/links", [
            dok: [
                [target: [targetUri: "/documents/${target.idAsString}"]],
            ]
        ], 403)

        then:
        thrown(NotAllowedException)

        when: "trying to delete the element"
        delete("/$elementType/$elementId", 403)

        then:
        thrown(NotAllowedException)

        where:
        [type, rights] << ElementType.values().collectMany {
            [
                [type: it, rights: new TestUserRights(restrictUnitAccess: true, testUnitReadable: true)],
            ]
        }
    }
}
