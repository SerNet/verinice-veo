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

import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.TestUserRights
import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Control
import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
import org.veo.core.entity.RiskAffected
import org.veo.core.entity.Scenario
import org.veo.core.entity.Unit
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.specification.NotAllowedException

@WithUserDetails("user@domain.example")
class RiskAffectedAccessRestrictionITSpec extends VeoMvcSpec {

    Client client
    Unit unit
    UUID unitId
    Domain domain
    String domainId
    Control control
    Scenario scenario1
    Scenario scenario2

    def setup() {
        client = createTestClient()
        domain = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
        domainId = domain.idAsString
        client = clientRepository.getById(client.id)
        unit = unitDataRepository.save(newUnit(client) {
            name = "restricted unit"
        })
        unitId = unit.id
        control = saveNewElement(ElementType.CONTROL, unit, domain)
        scenario1 = saveNewElement(ElementType.SCENARIO, unit, domain)
        scenario2 = saveNewElement(ElementType.SCENARIO, unit, domain)
    }

    def "read access allowed for #type with #rights"() {
        given:
        updateUser("user@domain.example", rights, unitId)

        and:
        RiskAffected element = saveNewElement(type, unit, domain) {
            implementControl(control)
            obtainRisk(scenario1).tap {
                assignDesignator(it)
            }
        }
        def elementId = element.idAsString
        def elementType = type.pluralTerm

        when: "fetching all risks"
        get("/$elementType/$elementId/risks", 200)

        then:
        noExceptionThrown()

        expect: "that CIs can be fetched"
        with(parseJson(get("/domains/$domainId/$elementType/${element.idAsString}/control-implementations"))) {
            totalItemCount == 1
        }

        and: "RIs can be fetched in domain context"
        with(parseJson(get("/domains/$domainId/$elementType/${element.idAsString}/control-implementations/${control.idAsString}/requirement-implementations"))) {
            totalItemCount == 1
        }

        and: "RIs can be fetched"
        with(parseJson(get("/$elementType/${element.idAsString}/control-implementations/${control.idAsString}/requirement-implementations"))) {
            totalItemCount == 1
        }

        where:
        [type, rights] << ElementType.RISK_AFFECTED_TYPES.collectMany {
            [
                [type: it, rights: new TestUserRights(restrictUnitAccess: false)],
                [type: it, rights: new TestUserRights(restrictUnitAccess: true, accessAllUnits:  true)],
                [type: it, rights: new TestUserRights(restrictUnitAccess: true, testUnitReadable:  true)],
            ]
        }
    }

    def "read access denied for #type with #rights"() {
        given:
        updateUser("user@domain.example", rights, unitId)

        and:
        RiskAffected element = saveNewElement(type, unit, domain) {
            implementControl(control)
            obtainRisk(scenario1).tap {
                assignDesignator(it)
            }
        }
        def elementId = element.idAsString
        def elementType = type.pluralTerm

        when:
        get("/$elementType/$elementId/risks", 404)

        then:
        thrown(NotFoundException)

        when: "fetching the risk"
        get("/$elementType/$elementId/risks/${scenario1.idAsString}", 404)

        then:
        thrown(NotFoundException)

        when: "fetching CIs"
        get("/domains/$domainId/$elementType/$elementId/control-implementations", 404)

        then:
        thrown(NotFoundException)

        when: "fetching RIs (domain context)"
        get("/domains/$domainId/$elementType/${element.idAsString}/control-implementations/${control.idAsString}/requirement-implementations", 404)

        then:
        thrown(NotFoundException)

        when: "fetching RIs"
        get("/$elementType/${element.idAsString}/control-implementations/${control.idAsString}/requirement-implementations", 404)

        then:
        thrown(NotFoundException)

        where:
        [type, rights] << ElementType.RISK_AFFECTED_TYPES.collectMany {
            [
                [type: it, rights: new TestUserRights(restrictUnitAccess: true)],
            ]
        }
    }

    def "write access allowed for #type with #rights"() {
        given:
        updateUser("user@domain.example", rights, unitId)

        and:
        RiskAffected element = saveNewElement(type, unit, domain) {
            implementControl(control)
        }
        def elementId = element.idAsString
        def elementType = type.pluralTerm

        when: "trying to create a new risk"
        parseJson(post("/$elementType/$elementId/risks", [
            scenario: [targetUri: "/scenarios/${scenario1.idAsString}"],
            domains: [
                (domain.idAsString) : [
                    reference: [targetUri: '/domains/'+ domain.idAsString ]
                ]
            ]
        ]))

        then:
        noExceptionThrown()

        when: "deleting a risk"
        delete("/$elementType/$elementId/risks/${scenario1.idAsString}")

        then:
        noExceptionThrown()

        expect: "that an RI can be updated"
        get("/$elementType/${element.idAsString}/requirement-implementations/${control.idAsString}").with {
            def ri = parseJson(it)
            def etag = getETag(it)
            put(ri._self, ri, ['If-Match': etag], 204)
        }

        when: "trying to execute a missing action"
        post("/domains/$domainId/$elementType/${element.idAsString}/actions/NOP/execution", [:], 404)

        then:
        thrown(NotFoundException)

        where:
        [type, rights] << ElementType.RISK_AFFECTED_TYPES.collectMany {
            [
                [type: it, rights: new TestUserRights(restrictUnitAccess: false)],
                [type: it, rights: new TestUserRights(restrictUnitAccess: true, accessAllUnits:  true)],
                [type: it, rights: new TestUserRights(restrictUnitAccess: true, testUnitReadable:  true, testUnitWritable: true)],
            ]
        }
    }

    def "write access denied for #type with #rights"() {
        given:
        updateUser("user@domain.example", rights, unitId)

        and:
        RiskAffected element = saveNewElement(type, unit, domain) {
            implementControl(control)
            obtainRisk(scenario1).tap {
                assignDesignator(it)
            }
        }
        def elementId = element.idAsString
        def elementType = type.pluralTerm

        when: "trying to add another risk"
        post("/$elementType/$elementId/risks", [
            scenario: [ targetUri: '/scenarios/'+ scenario2.idAsString ],
            domains: [
                (domain.idAsString) : [
                    reference: [targetUri: '/domains/'+ domain.idAsString ]
                ]
            ]
        ], 403)

        then:
        thrown(NotAllowedException)

        when: "trying to delete a risk"
        delete("/$elementType/$elementId/risks/${scenario1.idAsString}", 403)

        then:
        thrown(NotAllowedException)

        when: "trying to update an RI"
        get("/$elementType/${element.idAsString}/requirement-implementations/${control.idAsString}").with{
            def  ri = parseJson(it)
            put(ri._self, ri, ['If-Match': getETag(it)], 403)
        }

        then:
        thrown(NotAllowedException)

        when: "trying to execute an action"
        post("/domains/$domainId/$elementType/${element.idAsString}/actions/NOP/execution", [:], 403)

        then:
        thrown(NotAllowedException)

        where:
        [type, rights] << ElementType.RISK_AFFECTED_TYPES.collectMany {
            [
                [type: it, rights: new TestUserRights(restrictUnitAccess: true, testUnitReadable: true)],
            ]
        }
    }
}
