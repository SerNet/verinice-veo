/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
package org.veo.core

import javax.transaction.Transactional
import javax.validation.ConstraintViolationException

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

@Transactional()
class PersonRepositoryITSpec extends VeoSpringSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository
    @Autowired
    private PersonRepositoryImpl personRepository
    private Client client
    private Unit unit
    private Domain domain

    def setup() {
        client = clientRepository.save(newClient())
        domain = newDomain(client)
        unit = unitRepository.save(newUnit(this.client))
    }

    def "save a composite person"() {
        when:
        def john = newPerson(unit)
        def jane = newPerson(unit)

        def compositePerson = newPerson(unit) {
            name = 'My composite person'
        }

        compositePerson.with {
            parts = [john, jane]
            it
        }
        compositePerson = personRepository.save(compositePerson)

        then:
        compositePerson != null
        compositePerson.parts  == [john, jane] as Set

        when:
        compositePerson = personRepository.findById(compositePerson.id)

        then:
        compositePerson.present
        compositePerson.get().name == 'My composite person'
        compositePerson.get().parts == [john, jane] as Set
    }

    def "cascading relations are validated"() {
        when:
        personRepository.save(newPerson(unit) {
            associateWithDomain(newDomain(client), null, null)
            // bypass setters to sneak in invalid values
            customAspects = [newCustomAspect(null, domain)]
            links = [
                newCustomLink(null, null, domain)
            ]
            parts = [
                newPerson(unit) {
                    designator = "super bad designator"
                }
            ]
        })

        then:
        def ex = thrown(ConstraintViolationException)
        ex.constraintViolations*.propertyPath*.toString() ==~ [
            "customAspects[].type",
            "links[].target",
            "links[].type",
            "parts[].designator",
            "subTypeAspects[].status",
            "subTypeAspects[].subType",
        ]
    }

    def "Element counts can be queried"() {
        given:
        def domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        executeInTransaction{
            2.times {
                personRepository.save(newPerson(unit) {
                    associateWithDomain(domain, 'PER_Person', 'NEW')
                })
            }
            personRepository.save( newPerson(unit) {
                associateWithDomain(domain, 'PER_Person', 'IN_PROGRESS')
            })
            personRepository.save(newPerson(unit) {
                associateWithDomain(domain, 'PER_DataProtectionOfficer', 'RELEASED')
            })
        }

        when:
        def counts = personRepository.getCountsBySubType(unit, domain).toSorted{[it.subType, it.status]}

        then:
        counts.size() == 3
        with(counts[0]) {
            subType == 'PER_Person'
            status == 'IN_PROGRESS'
            count == 1
        }
        with(counts[1]) {
            subType == 'PER_Person'
            status == 'NEW'
            count == 2
        }
        with(counts[2]) {
            subType == 'PER_DataProtectionOfficer'
            status == 'RELEASED'
            count == 1
        }
    }
}
