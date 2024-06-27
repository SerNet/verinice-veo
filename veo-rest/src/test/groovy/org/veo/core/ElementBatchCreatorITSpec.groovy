/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
import org.springframework.transaction.annotation.Transactional

import org.veo.core.entity.Unit
import org.veo.core.repository.ClientRepository
import org.veo.core.repository.GenericElementRepository
import org.veo.core.repository.UnitRepository
import org.veo.core.usecase.domain.ElementBatchCreator

@Transactional
class ElementBatchCreatorITSpec extends VeoSpringSpec{

    @Autowired
    ElementBatchCreator elementBatchCreator

    @Autowired
    ClientRepository clientRepository

    @Autowired
    UnitRepository unitRepository

    @Autowired
    GenericElementRepository genericElementRepository

    Unit unit

    def setup() {
        def client = clientRepository.save(newClient {})
        unit = unitRepository.save(newUnit(client))
    }

    def "interrelated elements can be persisted in random order #r"() {
        given:
        def member1 = newProcess(unit) {
            name = "m1"
        }
        def member2 = newControl(unit) {
            name = "m2"
        }
        def comp = newProcess(unit) {
            name = "comp"
            parts = [member1]
        }
        def scope = newScope(unit) {
            addMember(member1)
            addMember(member2)
        }

        when:
        def elements = [member1, scope, comp, member2]
        elements.shuffle(r)
        elementBatchCreator.create(elements, unit)

        then:
        member1.scopes ==~ [scope]
        member1.composites ==~ [comp]
        member2.scopes ==~ [scope]
        comp.parts ==~ [member1]
        scope.members ==~ [member1, member2]

        where:
        r << (1..10).collect { new Random() }
    }
}
