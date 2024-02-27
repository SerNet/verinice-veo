/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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

import static org.veo.rest.configuration.WebMvcSecurityConfiguration.TESTCLIENT_UUID

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Key
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.specification.ContentTooLongException
import org.veo.core.entity.specification.ExceedLimitException
import org.veo.core.repository.ClientRepository
import org.veo.core.repository.UserConfigurationRepository
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UserConfigurationRepositoryImpl

class UserConfigurationControllerMvcITSpec extends VeoMvcSpec {
    @Autowired
    ClientRepositoryImpl clientRepository
    @Autowired
    UserConfigurationRepositoryImpl configurationRepository

    Client client
    def setup() {
        executeInTransaction {
            client= createTestClient()
        }
    }

    @WithUserDetails("user@domain.example")
    def "create and manage user configuration"() {
        given: "a user configuration"
        Map applicationData = [
            name: 'New Document',
            other: 4
        ]

        when: "we create a user configuration"
        def result = parseJson(put('/user-configurations/appId1', applicationData, 201))

        then:
        result.message == "configuration created"

        when: "we get the configuration"
        def stored = parseJson(get('/user-configurations/appId1'))

        then: "the data can be received"
        applicationData == stored

        when: "we update the data"
        applicationData = [
            name: 'New Document1',
            other: 5,
            another: "dd"
        ]
        result = parseJson(put('/user-configurations/appId1', applicationData))

        then:
        result.message == "configuration updated"

        when: "we get the updated configuration"
        stored = parseJson(get('/user-configurations/appId1'))

        then: "the data can be received"
        applicationData == stored

        when: "we delete the configuration"
        delete('/user-configurations/appId1')
        get('/user-configurations/appId1', 404)

        then: "an exception is thrown"
        NotFoundException ex = thrown()

        and: "the reason is given"
        ex.message == "no configuration found for user 'user@domain.example' and application id: 'appId1'"
    }

    @WithUserDetails("user@domain.example")
    def "delete client delete all configuration of the client users"() {
        given: "a user configuration"
        Map applicationData = [
            name: 'New Document',
            other: 4
        ]

        when: "we create some user configurations"
        def result = parseJson(put('/user-configurations/appId1', applicationData, 201))
        put('/user-configurations/appId2', [
            name: 'New Document',
            other: 1
        ], 201)
        put('/user-configurations/appId3', [
            name: 'New Document',
            other: 2
        ], 201)

        then:
        result.message == "configuration created"

        when: "we get the configurations"
        def configs = configurationRepository.findAllByClient(Key.uuidFrom(TESTCLIENT_UUID))

        then: "the data can is present"
        configs.size() == 3

        when: "we delete the whole client"
        executeInTransaction {
            clientRepository.delete(client)
        }
        configs = configurationRepository.findAllByClient(Key.uuidFrom(TESTCLIENT_UUID))

        then: "all configurations are gone"
        configs.empty
    }

    @WithUserDetails("user@domain.example")
    def "do not store a configuration with more than 4000 bytes"() {
        given: "a user configuration with more than 4000 bytes"
        Map applicationData = [
            name: "a".repeat(4000),
            other: 4
        ]

        when: "we create a user configuration"
        def result = parseJson(put('/user-configurations/appId1', applicationData, 413))

        then:
        def ex = thrown(ContentTooLongException)
        ex.message == "Exceeds the configuration size limit. (4000 bytes)"
    }

    @WithUserDetails("user@domain.example")
    def "do not store more configurations than 10 per user"() {
        given: "a user configuration"
        Map applicationData = [
            name: "a",
            other: 4
        ]

        when: "we create a user configuration"
        (1..10).each {index->
            parseJson(put('/user-configurations/appId-'+index, applicationData, 201))
        }
        def result = parseJson(put('/user-configurations/appId-11', applicationData, 409))

        then:
        def ex = thrown(ExceedLimitException)
        ex.message == "Exceeds the configuration per user limit. (10 allowed)"
    }
}
