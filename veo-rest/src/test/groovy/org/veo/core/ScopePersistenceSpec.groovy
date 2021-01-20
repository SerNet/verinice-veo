/*******************************************************************************
 * Copyright (c) 2021 Jochen Kemnade.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core

import javax.transaction.Transactional

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan

import org.veo.core.entity.Key
import org.veo.core.entity.Versioned.Lifecycle
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ScopeRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

@SpringBootTest(classes = ScopePersistenceSpec.class)
@Transactional()
@ComponentScan("org.veo")
class ScopePersistenceSpec extends VeoSpringSpec {


    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private ScopeRepositoryImpl scopeRepository

    @Autowired
    private EntityFactory factory



    def "save a scope"() {
        given: "a client and a unit"
        def client = newClient()
        def unit = newUnit(client)
        clientRepository.save(client)
        unitRepository.save(unit)

        when:
        def scopeId = Key.newUuid()

        def john = newPerson(unit)
        def jane = newPerson(unit)

        def scope = factory.createScope(scopeId, 'My scope', unit)
        scope.version("user", null)

        scope.with {
            members = [john, jane]
            state = Lifecycle.CREATING
            it
        }
        scope = scopeRepository.save(scope)
        then:
        scope != null
        scope.members  == [john, jane] as Set

        when:
        scope = scopeRepository.findById(scopeId)
        then:
        scope.present
        scope.get().name == 'My scope'
        scope.get().members == [john, jane] as Set
    }
}
