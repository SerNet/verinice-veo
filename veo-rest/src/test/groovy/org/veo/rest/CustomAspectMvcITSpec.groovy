/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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

import org.veo.core.VeoMvcSpec
import org.veo.core.repository.UnitRepository
import org.veo.persistence.access.ClientRepositoryImpl

@WithUserDetails("user@domain.example")
class CustomAspectMvcITSpec extends VeoMvcSpec {

    @Autowired
    ClientRepositoryImpl clientRepository

    @Autowired
    UnitRepository unitRepository

    @Autowired
    TransactionTemplate txTemplate

    String unitId
    String domainId

    def setup() {
        txTemplate.execute {
            def client = createTestClient()
            domainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).idAsString
            unitId = unitRepository.save(newUnit(client)).idAsString
        }
    }

    def "save multiple links"() {
        when: "creating a person with different aspects"
        def personId = parseJson(post("/persons", [
            customAspects: [
                person_generalInformation: [
                    attributes: [
                        person_generalInformation_salutation: "Mr.",
                        person_generalInformation_familyName: "Mick"
                    ],
                    domains: [],
                ],
                person_dataProtectionOfficer: [
                    attributes: [
                        person_dataProtectionOfficer_expertise: true
                    ],
                    domains: [],
                ]
            ],
            name : "person",
            owner: [targetUri: "http://localhost/units/$unitId"],
            domains: [
                (domainId): [
                    subType: "PER_Person",
                    status: "NEW",
                ]
            ],
        ])).resourceId
        def retrievedPerson = parseJson(get("/persons/$personId"))

        then:
        with(retrievedPerson.customAspects) {
            person_generalInformation.attributes.person_generalInformation_salutation == "Mr."
            person_generalInformation.attributes.person_generalInformation_familyName == "Mick"
            person_dataProtectionOfficer.attributes.person_dataProtectionOfficer_expertise == true
        }
    }
}
