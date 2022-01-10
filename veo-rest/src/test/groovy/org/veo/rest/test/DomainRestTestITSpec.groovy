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

import java.time.Instant

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
}