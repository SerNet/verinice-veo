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

@WithUserDetails("admin")
class DomainTemplateControllerMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepository clientRepo

    def setup() {
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_UUID)
    }

    def "list all domain templates"() {
        given:
        createTestDomainTemplate(TEST_DOMAIN_TEMPLATE_ID)

        when:
        def templates = parseJson(get("/domain-templates"))

        then:
        templates.size() == 2
        with(templates.find {
            it.name == "DS-GVO"
        }) {
            id instanceof String
            templateVersion == "1.4.0"
            createdAt instanceof String
            _self.endsWith("/domain-templates/$id")
        }
        with(templates.find {
            it.name == "test-domain"
        }) {
            id instanceof String
            templateVersion == "1.0.0"
            createdAt instanceof String
            _self.endsWith("/domain-templates/$id")
        }
    }

    def "create DSGVO domain for a single client"() {
        given: "two clients"
        def client1 = clientRepo.save(newClient {})
        def client2 = clientRepo.save(newClient {})

        when: "creating the DSGVO domain for the client"
        post("/domain-templates/$DSGVO_DOMAINTEMPLATE_UUID/createdomains?clientids=${client1.idAsString}", [:], HttpStatus.SC_NO_CONTENT)
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
        given: "three clients"
        def client1 = clientRepo.save(newClient {})
        def client2 = clientRepo.save(newClient {})
        def client3 = clientRepo.save(newClient {})

        when: "creating the DSGVO domain for the client"
        post("/domain-templates/$DSGVO_DOMAINTEMPLATE_UUID/createdomains?clientids=${client1.idAsString},${client2.idAsString}", [:], HttpStatus.SC_NO_CONTENT)
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
        given: "three clients"
        def client1 = clientRepo.save(newClient {})
        def client2 = clientRepo.save(newClient {})
        def client3 = clientRepo.save(newClient {})

        when: "creating the DSGVO domain for the client"
        post("/domain-templates/$DSGVO_DOMAINTEMPLATE_UUID/createdomains?restrictToClientsWithExistingDomain=false", [:], HttpStatus.SC_NO_CONTENT)
        client1 = loadClientAndInitializeDomains(client1.id)
        client2 = loadClientAndInitializeDomains(client2.id)
        client3 = loadClientAndInitializeDomains(client3.id)

        then: "the clients get the new domain"
        client1.domains.size() == 1
        client2.domains.size() == 1
        client3.domains.size() == 1
    }

    def "create DSGVO domain for all clients with previous domaintemplate"() {
        given: "three clients"
        def client1 = clientRepo.save(newClient {})
        def client2 = clientRepo.save(newClient {})
        def client3 = clientRepo.save(newClient {})

        when: "creating the DSGVO domain for two clients"
        post("/domain-templates/$DSGVO_DOMAINTEMPLATE_UUID/createdomains?clientids=${client1.id},${client2.id}", [:], HttpStatus.SC_NO_CONTENT)
        client1 = loadClientAndInitializeDomains(client1.id)
        client2 = loadClientAndInitializeDomains(client2.id)
        client3 = loadClientAndInitializeDomains(client3.id)

        then: "the clients get the new domain"
        client1.domains.size() == 1
        client2.domains.size() == 1
        client3.domains.size() == 0

        when: "creating the DSGVO_V2 domain for all clients with previous DSGVO"
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_V2_UUID)
        post("/domain-templates/$DSGVO_DOMAINTEMPLATE_V2_UUID/createdomains?restrictToClientsWithExistingDomain=true", [:], HttpStatus.SC_NO_CONTENT)
        client1 = loadClientAndInitializeDomains(client1.id)
        client2 = loadClientAndInitializeDomains(client2.id)
        client3 = loadClientAndInitializeDomains(client3.id)

        then: "the clients get the new domain except for the client without previous version"
        client1.domains.size() == 2
        client2.domains.size() == 2
        client3.domains.size() == 0
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
