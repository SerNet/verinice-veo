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
package org.veo.core

import javax.transaction.Transactional

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan

import org.veo.core.entity.*
import org.veo.persistence.access.ClientRepositoryImpl

@SpringBootTest(classes = ClientRepositorySpec.class)
@Transactional()
@ComponentScan("org.veo")
class ClientRepositorySpec extends VeoSpringSpec {


    @Autowired
    private ClientRepositoryImpl repository


    def "create a simple client and a domain together"() {
        given: "a domain and a client"

        Key clientId = Key.newUuid()
        Domain domain = newDomain {
            name = "27001"
            description = "ISO/IEC"
            abbreviation = "ISO"
        }

        Client client = newClient {
            id = clientId
            name = "Demo Client"
        }
        client.setDomains([domain] as Set)

        repository.save(client)

        when: "loaded from db"

        Optional<Client> newClient = repository.findById(clientId)
        boolean isPresent = newClient.isPresent()

        Client c = newClient.get()

        then : "is all ok"
        isPresent == true
        c.name == "Demo Client"
        c.domains.size()==1
        c.domains.first().name == "27001"


        when: "The client is persisted"
        repository.save(c)

        and: "The client is retrieved"
        newClient = repository.findById(clientId)
        isPresent = newClient.isPresent()
        c = newClient.get()

        then : "is all ok"
        isPresent == true
        c.name == "Demo Client"
        c.domains.size()==1
        c.domains.first().name == "27001"
        c.domains.first().description == "ISO/IEC"
        c.domains.first().abbreviation == "ISO"
    }

    def "create a simple client with unit"() {
        given: "a domain and a client"

        Key clientId = Key.newUuid()
        Domain domain = newDomain {
            name = "27001"
            description = "ISO/IEC"
            abbreviation = "ISO"
        }

        Client client = newClient {
            id = clientId
        }

        client.setDomains(([domain] as Set))

        repository.save(client)

        when: "loaded from db"

        Optional<Client> newClient = repository.findById(clientId)
        boolean isPresent = newClient.isPresent()

        Client c = newClient.get()
        Unit unit = newUnit c, {
            name = "new Unit"
        }

        c.addToUnits(unit)
        repository.save(newClient.get())
        newClient = repository.findById(clientId)

        isPresent = newClient.isPresent()
        c = newClient.get()

        then: "test"

        isPresent == true
        c.units.size()==1
        c.domains.first().description == "ISO/IEC"
        c.domains.first().abbreviation == "ISO"
    }

    def "create simple client, unit and some objects"() {
        given: "a domain and a client"

        Key clientId = Key.newUuid()
        Domain domain = newDomain{
            name = "27001"
            description = "ISO/IEC"
            abbreviation = "ISO"
        }

        Client client = newClient {
            id = clientId
            domains = ([domain] as Set)
        }

        Unit unit = newUnit client, {
            domains = [domain] as Set
        }
        client.addToUnits(unit)

        Person person = newPerson unit, {
            domains = [domain] as Set
        }

        Asset asset = newAsset unit, {
            domains = [domain] as Set
        }

        Process process = newProcess unit, {
            domains = [domain] as Set
        }

        Document document = newDocument unit, {
            domains = [domain] as Set
        }

        when:"save and load the client"

        repository.save(client)
        Client newClient = repository.findById(clientId).get()

        then:"all data is present"

        newClient.name == client.name
        newClient.domains == client.domains
        newClient.units == client.units
        newClient.domains.first() == client.domains.first()
        newClient.domains.first().name == client.domains.first().name
        newClient.domains.first().abbreviation == client.domains.first().abbreviation
        newClient.domains.first().description == client.domains.first().description
        newClient.units.first().name == client.units.first().name
        newClient.units.first().description == client.units.first().description
        newClient.units.first().abbreviation == client.units.first().abbreviation
    }
}
