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

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ScopeRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

import jakarta.transaction.Transactional
import jakarta.validation.ConstraintViolationException

@Transactional()
class ScopePersistenceSpec extends VeoSpringSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository
    @Autowired
    private ScopeRepositoryImpl scopeRepository

    private Client client
    private Unit unit
    private Domain domain

    def setup() {
        client = clientRepository.save(newClient())
        domain = newDomain(client)
        unit = unitRepository.save(newUnit(this.client))
    }

    def "save a scope"() {
        given: "a client and a unit"
        def client = clientRepository.save(newClient())
        def unit = unitRepository.save(newUnit(client))

        when:
        def john = newPerson(unit)
        def jane = newPerson(unit)

        def scope = newScope(unit) {
            name = 'My scope'
        }

        scope.with {
            members = [john, jane]
            it
        }
        scope = scopeRepository.save(scope)

        then:
        scope != null
        scope.members  == [john, jane] as Set

        when:
        scope = scopeRepository.findById(scope.id)

        then:
        scope.present
        scope.get().name == 'My scope'
        scope.get().members == [john, jane] as Set
    }

    def "cascading relations are validated"() {
        when:
        scopeRepository.save(newScope(unit) {
            associateWithDomain(newDomain(client), null, null)
            // bypass apply method to sneak in invalid CA
            customAspects = [newCustomAspect(null, domain)]
            applyLink(newCustomLink(null, "goodLink", domain))
            members = [
                newScope(unit) {
                    designator = "super bad designator"
                }
            ]
        })

        then:
        def ex = thrown(ConstraintViolationException)
        ex.constraintViolations*.propertyPath*.toString().sort() == [
            "customAspects[].type",
            "domainAssociations[].status",
            "domainAssociations[].subType",
            "links[].target",
            "members[].designator",
        ]
    }
}
