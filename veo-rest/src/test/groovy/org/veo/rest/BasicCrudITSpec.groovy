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
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Key
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.rest.configuration.WebMvcSecurityConfiguration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [WebMvcSecurityConfiguration])
class BasicCrudITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private ProcessRepositoryImpl processRepository

    @Autowired
    TransactionTemplate txTemplate

    private Key clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)

    Client client

    def setup() {
        def domain1 = newDomain {name = 'Domain 1'}
        client = txTemplate.execute {
            clientRepository.save(newClient {
                id = clientId
                domains = [domain1]
            })
        }
    }

    @WithUserDetails("user@domain.example")
    def "Basic CRUD example"() {
        when:
        def result = parseJson(post('/units', [name: 'My CRUD unit']))

        then:
        result != null
        when:
        def unitId = result.resourceId
        result = parseJson(post('/assets', [
            name : 'My CRUD asset',
            owner: [
                targetUri: "/units/$unitId"
            ]
        ]))
        then:
        result != null
        when:
        def assetId = result.resourceId
        result = parseJson(post('/processes', [
            name : 'My CRUD process',
            owner: [
                targetUri: "/units/$unitId"
            ],
            links: [
                'process_dataType':[
                    [
                        target:
                        [
                            targetUri: "/assets/$assetId"
                        ]
                    ]
                ]
            ]
        ]))
        then:
        result != null
        when:
        def processId = result.resourceId
        def process = txTemplate.execute{
            processRepository.findById(Key.uuidFrom(processId)).tap {
                // initialize hibernate proxies
                it.ifPresent{ it.links.first().target.id }
            }
        }
        then:
        process.present
        when:
        def links = process.get().links
        then:
        links.size() == 1
        links.first().type == 'process_dataType'
        links.first().target.id.uuidValue() == assetId
        when:

        result = parseJson(post('/scopes', [
            name : 'My CRUD scope',
            owner: [
                targetUri: "/units/$unitId"
            ],
            members: [
                [
                    targetUri: "/assets/$assetId"
                ],
                [
                    targetUri: "/processes/$processId"
                ],
            ]
        ]))
        then:
        result != null
        when:
        delete("/units/$unitId")
        then:
        notThrown(Exception)
    }
}
