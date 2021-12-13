/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman
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
package org.veo.jobs

import org.h2.engine.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder

import org.veo.core.VeoSpringSpec
import org.veo.core.entity.AccountProvider
import org.veo.core.entity.Client
import org.veo.core.entity.Key
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.rest.security.ApplicationUser
import org.veo.test.VeoSpec

import spock.lang.AutoCleanup

class AsSystemUserSpec extends VeoSpringSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @AutoCleanup('revokeUser')
    private UserSwitcher userSwitcher

    def setup() {
        userSwitcher = new UserSwitcher()
    }

    def "System user works in configured client"() {
        given: "three clients"
        def client1 = createClient(UUID.randomUUID().toString())
        def client2 = createClient(UUID.randomUUID().toString())
        def client3 = createClient(UUID.randomUUID().toString())

        expect: "system user runs in correct client during loops"
        [client1, client2, client3].each { currentClient ->
            AsSystemUser.runInClient( currentClient, {
                def currentUser = ApplicationUser.authenticatedUser(SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal())
                assert currentUser.clientId == currentClient.getIdAsString()
            })
        }
    }

    Client createClient(String clientId) {
        userSwitcher.switchToUser("testuser", clientId)
        def client = null
        txTemplate.execute {
            client = newClient {
                id = Key.uuidFrom(clientId)
            }
            domainTemplateService.createDefaultDomains(client)
            clientRepository.save(client)
        }
        userSwitcher.revokeUser()
        client
    }
}
