/*******************************************************************************
 * Copyright (c) 2020 Jonas Jordan.
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
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Key
import org.veo.core.repository.ClientRepository
import org.veo.rest.configuration.WebMvcSecurityConfiguration

/**
 * Tests the client creation process. Note that this one tests the Unit controller but doesn't have the setup where the
 * client is created manually.
 */
class ClientCreationMvcITSpec extends VeoMvcSpec {
    @Autowired
    ClientRepository clientRepository

    @WithUserDetails("user@domain.example")
    def "create a new client"() {
        when: "posting a unit as a new client"
        post("/units", ["name": "nova"])
        then: "the client has been created"
        clientRepository.exists(Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID))
    }
}
