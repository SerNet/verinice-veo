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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder

import org.veo.core.VeoSpringSpec
import org.veo.core.entity.Client
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.rest.security.ApplicationUser

class AsSystemUserSpec extends VeoSpringSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    private UserSwitcher userSwitcher = new UserSwitcher()

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
        userSwitcher.runAsUser("testuser", clientId) {
            txTemplate.execute {
                newClient {
                    id = UUID.fromString(clientId)
                }.tap {
                    defaultDomainCreator.addDomain(it, "ISO", false)
                    clientRepository.save(it)
                }
            }
        }
    }
}
