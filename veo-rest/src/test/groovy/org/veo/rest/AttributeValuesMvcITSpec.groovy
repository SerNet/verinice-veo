/*
 * verinice.veo
 * Copyright (C) 2026  Aziz Khalledi.
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
 */
package org.veo.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
import org.veo.core.entity.definitions.CustomAspectDefinition
import org.veo.core.entity.definitions.attribute.DurationAttributeDefinition
import org.veo.persistence.access.ClientRepositoryImpl

class AttributeValuesMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private TransactionTemplate txTemplate

    private Client client
    private Domain domain
    private String unitId

    def setup() {
        txTemplate.execute {
            client = createTestClient()
            domain = newDomain(client) {
                applyElementTypeDefinition(newElementTypeDefinition(ElementType.PERSON, it) {
                    subTypes = [
                        PER_Person: newSubTypeDefinition {
                            sortKey = 1
                        }
                    ]
                    customAspects = [
                        person_duration: new CustomAspectDefinition().tap {
                            attributeDefinitions = [estimatedDuration: new DurationAttributeDefinition()]
                        }
                    ]
                })
            }
            client = clientRepository.save(client)
            domain = client.domains.first()
            def unit = unitDataRepository.save(newUnit(client))
            unitId = unit.idAsString
            personDataRepository.save(newPerson(unit) {
                associateWithDomain(domain, 'PER_Person', 'NEW')
                customAspects = [
                    newCustomAspect('person_duration', domain) {
                        attributes['estimatedDuration'] = 'PT8H'
                    }
                ]
            })
            personDataRepository.save(newPerson(unit) {
                associateWithDomain(domain, 'PER_Person', 'NEW')
                customAspects = [
                    newCustomAspect('person_duration', domain) {
                        attributes['estimatedDuration'] = 'P3D'
                    }
                ]
            })
        }
    }

    @WithUserDetails("user@domain.example")
    def "queries used duration attribute values"() {
        when:
        def result = parseJson(
                get("/domains/${domain.idAsString}/attribute-values?type=duration&unit=${unitId}"))

        then:
        result.values == ['PT8H', 'P3D']
        result.truncated == false
    }

    @WithUserDetails("user@domain.example")
    def "rejects an unknown attribute type with 400"() {
        when:
        get("/domains/${domain.idAsString}/attribute-values?type=bogus&unit=${unitId}", 400)

        then:
        thrown(IllegalArgumentException)
    }
}
