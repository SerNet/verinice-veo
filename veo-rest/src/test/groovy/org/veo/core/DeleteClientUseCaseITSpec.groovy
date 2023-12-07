/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade.
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
import org.veo.core.repository.UnitRepository
import org.veo.core.usecase.client.DeleteClientUseCase
import org.veo.core.usecase.client.DeleteClientUseCase.InputData
import org.veo.persistence.access.ClientRepositoryImpl

@WithUserDetails("admin")
class DeleteClientUseCaseITSpec extends VeoSpringSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepository unitRepository

    @Autowired
    private DeleteClientUseCase deleteClientUseCase

    def "delete a client"() {
        given:
        def client = createTestClient()
        def domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        unitRepository.save(newUnit(client) {
            addToDomains(domain)
        })

        when:
        runUseCase(client)

        then:
        clientRepository.findAll().empty
    }

    def "delete a client with an inactive domain"() {
        given:
        def client = createTestClient()
        def domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID).with {
            active = false
            domainDataRepository.save(it)
        }
        unitRepository.save(newUnit(client) {
            addToDomains(domain)
        })

        when:
        runUseCase(client)

        then:
        clientRepository.findAll().empty
    }

    def runUseCase(Client client) {
        executeInTransaction {
            deleteClientUseCase.execute(new InputData(client.id))
        }
    }

    Client createClient() {
        executeInTransaction {
            def client = newClient()
            defaultDomainCreator.addDefaultDomains(client)
            return clientRepository.save(client)
        }
    }
}