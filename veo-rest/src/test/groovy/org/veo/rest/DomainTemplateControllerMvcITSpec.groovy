/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade
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

import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.repository.ClientRepository
import org.veo.core.repository.DocumentRepository
import org.veo.core.repository.UnitRepository

@WithUserDetails("admin")
class DomainTemplateControllerMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepository clientRepo
    @Autowired
    private UnitRepository unitRepo
    @Autowired
    private DocumentRepository documentRepo

    def setup() {
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_UUID)
    }

    def "create DSGVO domain for a single client"() {
        given: "a client with some units and a document"
        def client1 = clientRepo.save(newClient {})
        def client2 = clientRepo.save(newClient {})

        when: "creating the DSGVO domain for the client"
        post("/domaintemplates/$DSGVO_DOMAINTEMPLATE_UUID/createdomains?clientids=${client1.id.uuidValue()}", [:], HttpStatus.SC_NO_CONTENT)
        client1 = loadClientAndInitializeDomains(client1.id)
        then: "the client gets the new domain"
        with(client1.domains) {
            size() == 1
            first().name == 'DS-GVO'
        }
        and: "the other client is not affected"
        with(client2.domains) {
            size() == 0
        }
    }

    def "create DSGVO domain for multiple clients"() {
        given: "a client with some units and a document"
        def client1 = clientRepo.save(newClient {})
        def client2 = clientRepo.save(newClient {})
        def client3 = clientRepo.save(newClient {})

        when: "creating the DSGVO domain for the client"
        post("/domaintemplates/$DSGVO_DOMAINTEMPLATE_UUID/createdomains?clientids=${client1.id.uuidValue()},${client2.id.uuidValue()}", [:], HttpStatus.SC_NO_CONTENT)
        client1 = loadClientAndInitializeDomains(client1.id)
        client2 = loadClientAndInitializeDomains(client2.id)
        client3 = loadClientAndInitializeDomains(client3.id)
        then: "the clients get the new domain"
        with(client1.domains) {
            size() == 1
            first().name == 'DS-GVO'
        }
        with(client2.domains) {
            size() == 1
            first().name == 'DS-GVO'
        }
        and: "the other client is not affected"
        with(client3.domains) {
            size() == 0
        }
    }

    def "create DSGVO domain for all clients"() {
        given: "a client with some units and a document"
        def client1 = clientRepo.save(newClient {})
        def client2 = clientRepo.save(newClient {})
        def client3 = clientRepo.save(newClient {})

        when: "creating the DSGVO domain for the client"
        post("/domaintemplates/$DSGVO_DOMAINTEMPLATE_UUID/createdomains", [:], HttpStatus.SC_NO_CONTENT)
        client1 = loadClientAndInitializeDomains(client1.id)
        client2 = loadClientAndInitializeDomains(client2.id)
        client3 = loadClientAndInitializeDomains(client3.id)
        then: "the clients get the new domain"
        client1.domains.size() == 1
        client2.domains.size() == 1
        client3.domains.size() == 1
    }

    Client loadClientAndInitializeDomains(clientId) {
        txTemplate.execute{
            clientRepo.findById(clientId).get().tap {
                //initialize lazy associations
                domains.each {
                    it.name
                }
            }
        }
    }
}
