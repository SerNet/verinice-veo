/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
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
package org.veo.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Key
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory
import org.veo.rest.configuration.WebMvcSecurityConfiguration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [WebMvcSecurityConfiguration])
class BasicCrudITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private EntityDataFactory entityFactory

    @Autowired
    TransactionTemplate txTemplate

    private Key clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)

    Client client

    def setup() {
        client = txTemplate.execute {
            Client client = entityFactory.createClient()
            client.id = clientId

            clientRepository.save(client)
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
                href: "/units/$unitId"
            ]
        ]))
        then:
        result != null
        when:
        def assetId = result.resourceId
        result = parseJson(post('/processes', [
            name : 'My CRUD process',
            owner: [
                href: "/units/$unitId"
            ]
        ]))
        then:
        result != null
        when:
        def processId = result.resourceId
        result = parseJson(post('/groups', [
            name : 'My CRUD group',
            type : 'Asset',
            owner: [
                href: "/units/$unitId"
            ]
        ]))
        then:
        result != null
        when:
        result = delete("/units/$unitId")
        then:
        notThrown(Exception)
    }
}