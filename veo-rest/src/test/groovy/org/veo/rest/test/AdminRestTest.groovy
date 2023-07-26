/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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
package org.veo.rest.test

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j

@Slf4j
class AdminRestTest extends VeoRestTest{
    def "get unit dump"() {
        given:
        def unitId = post("/units", [
            name: "my little unit"
        ]).body.resourceId

        when:
        def dump = get("/admin/unit-dump/$unitId", 200, UserType.ADMIN).body

        then:
        dump.unit.name == "my little unit"
    }

    def "get a unit dump from created elements"() {
        given:
        def unitId = postNewUnit("my catalog unit").resourceId
        def catalogItemIds = getCatalogItems(dsgvoDomainId)*.id
        log.info("==> catalogItemIds: {}", catalogItemIds)

        def incarnationDescription = get("/units/${unitId}/incarnations?itemIds=${catalogItemIds.join(',')}").body
        log.info("==> incarnationDescription: {}", JsonOutput.toJson(incarnationDescription))
        post("/units/${unitId}/incarnations", incarnationDescription)

        when:
        def dump = get("/admin/unit-dump/$unitId", 200, UserType.ADMIN).body
        log.info("===> {}",JsonOutput.toJson(dump))

        then:
        dump.unit.name == "my catalog unit"

        dump.domains.size() >= 2
        dump.elements.size() == catalogItemIds.size()
        with (dump.elements.find { it.abbreviation == "VVT" }) {
            description == "VVT-Prozess"
            links.size() == 1
            links.process_tom.size() == 8
            domains[owner.dsgvoDomainId].subType == "PRO_DataProcessing"
            domains[owner.dsgvoDomainId].status == "NEW"
        }
        with (dump.elements.find { it.abbreviation == "TOM-I" }) {
            customAspects.size() == 1
            customAspects.control_dataProtection.domains.size() == 1
            customAspects.control_dataProtection.attributes.size() == 1
            customAspects.control_dataProtection.attributes.control_dataProtection_objectives == [
                "control_dataProtection_objectives_integrity"
            ]
            domains[owner.dsgvoDomainId].subType == "CTL_TOM"
            domains[owner.dsgvoDomainId].status == "NEW"
        }
    }
}
