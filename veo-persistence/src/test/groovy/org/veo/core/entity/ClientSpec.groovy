/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
package org.veo.core.entity

import org.veo.test.VeoSpec

class ClientSpec extends VeoSpec{
    def "Create a new Client"() {
        given: "a Client name"
        String domainName = 'Test domain'
        String clientName = 'Test Client'

        when : "Client is created"
        Client client = newClient() {
            name = clientName
        }
        Domain domain = newDomain(client) {
            name = domainName
        }

        then: "domain is correctly initialized"
        client.getName() == clientName
        client.getDomains().size() == 1
        client.getDomains().first().getName() == domainName
        domain.owner == client
    }
}
