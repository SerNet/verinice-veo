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
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.entity.Client
import org.veo.core.entity.Unit
import org.veo.core.usecase.unit.CreateDemoUnitUseCase
import org.veo.core.usecase.unit.CreateDemoUnitUseCase.InputData
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

@WithUserDetails("user@domain.example")
class CreateDemoUnitUseCaseITSpec extends VeoSpringSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private CreateDemoUnitUseCase useCase

    def "create a demo unit for a client"() {
        given:
        def client = createClient()
        when:
        def unit = runUseCase(client)
        then:
        unit != null
        with(unit) {
            it.name == 'Demo Unit'
        }
        unitRepository.findByClient(client).size() == 1
    }

    def "create multiple demo units for a client"() {
        given:
        def client = createClient()
        when:
        def unit1 = runUseCase(client)
        def unit2 = runUseCase(client)
        def unit3 = runUseCase(client)
        then:
        unit1 != null
        unit2 != null
        unit3 != null
        with(unit1) {
            it.name == 'Demo Unit'
        }
        with(unit2) {
            it.name == 'Demo Unit'
        }
        with(unit3) {
            it.name == 'Demo Unit'
        }
        unitRepository.findByClient(client).size() == 3
    }

    def "create demo units for multiple clients"() {
        given:
        def client1 = createClient()
        def client2 = createClient()
        def client3 = createClient()
        when:
        def unit1 = runUseCase(client1)
        def unit2 = runUseCase(client2)
        def unit3 = runUseCase(client3)
        then:
        unit1 != null
        unit2 != null
        unit3 != null
        with(unit1) {
            it.name == 'Demo Unit'
        }
        with(unit2) {
            it.name == 'Demo Unit'
        }
        with(unit3) {
            it.name == 'Demo Unit'
        }
        unitRepository.findByClient(client1).size() == 1
        unitRepository.findByClient(client2).size() == 1
        unitRepository.findByClient(client3).size() == 1
    }

    Unit runUseCase(Client client) {
        executeInTransaction {
            useCase.execute(new InputData(client.id)).unit
        }
    }

    Client createClient() {
        executeInTransaction {
            def client = newClient()
            domainTemplateService.createDefaultDomains(client)
            clientRepository.save(client)
        }
    }



    def executeInTransaction(Closure cl) {
        txTemplate.execute {
            cl.call()
        }
    }
}