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
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Key
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl

@ActiveProfiles(["test", "stats", "local"])
class BasicCrudITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private ProcessRepositoryImpl processRepository

    @Autowired
    TransactionTemplate txTemplate

    Client client
    String domainId

    def setup() {
        client = createTestClient()
        domainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).idAsString
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
            domains: [
                (domainId): [
                    subType: "AST_Datatype",
                    status: "NEW"
                ]
            ],
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ]
        ]))
        then:
        result != null
        when:
        def assetId = result.resourceId
        result = parseJson(post('/processes', [
            name : 'My CRUD process',
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            links: [
                'process_dataType':[
                    [
                        target:
                        [
                            targetUri: "http://localhost/assets/$assetId"
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
        def memberScopeId = parseJson(post('/scopes', [
            name : 'My CRUD memeber scope',
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            members: []
        ])).resourceId

        result = parseJson(post('/scopes', [
            name : 'My CRUD scope',
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ],
            members: [
                [
                    targetUri: "http://localhost/assets/$assetId"
                ],
                [
                    targetUri: "http://localhost/processes/$processId"
                ],
                [
                    targetUri: "http://localhost/scopes/$memberScopeId"
                ]
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
