/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Urs Zeidler.
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

import groovy.util.logging.Slf4j

@Slf4j
class DomainRestTestITSpec extends VeoRestTest {

    public static final String UNIT_NAME = 'Testunit'

    def postResponse
    String unitId

    def "export the test domain"() {
        log.info("Create a unit and export domain")

        given:
        postResponse = postNewUnit(UNIT_NAME)
        unitId = postResponse.resourceId

        when: "the catalog is retrieved"
        def dsgvoId = getDomains().find { it.name == "test-domain" }.id
        def domainDto = exportDomain(dsgvoId)
        log.info("==> exported domain: {}", domainDto)

        then: "the domain is exported"
        with(domainDto.catalogs[0]) {
            catalogItems.size() == 6
            name == "TEST-Controls"
            domainTemplate.displayName == "td test-domain"
        }
    }

    def "export the dsgvo domain"() {
        log.info("Create a unit and export dsgvo domain")

        given:
        postResponse = postNewUnit(UNIT_NAME)
        unitId = postResponse.resourceId

        when: "the catalog is retrieved"
        def dsgvoId = getDomains().find { it.name == "DS-GVO" }.id
        def domainDto = exportDomain(dsgvoId)
        log.info("==> exported domain: {}", domainDto)

        def catalog = domainDto.catalogs[0]
        def vvt = catalog.catalogItems.find { it.element.abbreviation == "VVT" }
        def tomi = catalog.catalogItems.find { it.element.abbreviation == "TOM-I" }
        def dsg23 = catalog.catalogItems.find { it.element.abbreviation == "DS-G.23" }
        log.info("==> catalogItems tomi: {}", tomi)
        log.info("==> catalogItems VVT: {}", vvt)

        then: "the domain is exported"
        with(catalog) {
            catalogItems.size() == 65
            name == "DS-GVO"
        }
        with (vvt) {
            namespace == "TOM.VVT"
            tailoringReferences.size() == 8
            with (element) {
                description == "VVT-Prozess"
                domains[dsgvoId].subType == "PRO_DataProcessing"
                domains[dsgvoId].status == "NEW"
            }
        }
        with (tomi.element) {
            customAspects.size() == 1
            customAspects.control_dataProtection.attributes.size() == 1
            customAspects.control_dataProtection.attributes.control_dataProtection_objectives == "control_dataProtection_objectives_integrity"
            domains[dsgvoId].subType == "CTL_TOM"
            domains[dsgvoId].status == "NEW"
        }
        with (dsg23) {
            with (element) {
                name == "Keine Widerspruchsmöglichkeit für Betroffene gegen die Datenverarbeitung"
                domains[dsgvoId].subType == "SCN_Scenario"
                domains[dsgvoId].status == "NEW"
            }
        }
    }
}