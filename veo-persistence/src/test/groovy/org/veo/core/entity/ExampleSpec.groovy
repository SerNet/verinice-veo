/*******************************************************************************
 * Copyright (c) 2020 Urs Zeidler.
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
package org.veo.core.entity

import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory

import spock.lang.Specification

class ExampleSpec extends Specification {

    private EntityFactory entityFactory = new EntityDataFactory()

    def "Create a new Domain"() {
        given: "a domain name"
        String name = 'Test domain'

        when : "Domain is created"

        Domain domain = entityFactory.createDomain(Key.newUuid(), name)

        then: "domain is correct initatlized"

        domain.getName().equals(name)
    }

    def "Create a new Client"() {
        given: "a Client name"
        String domainname = 'Test domain'
        String clientname = 'Test Client'

        when : "Cient is created"

        Domain domain = entityFactory.createDomain(Key.newUuid(), domainname)

        Client client = entityFactory.createClient(Key.newUuid(), clientname)
        client.setDomains([domain] as Set)
        then: "domain is correct initatlized"

        client.getName().equals(clientname)
        client.getDomains().size() == 1
        client.getDomains().first().getName().equals(domainname)
    }
}
