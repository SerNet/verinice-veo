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

import groovy.transform.CompileStatic

import org.veo.core.entity.impl.ClientImpl
import org.veo.core.entity.impl.DomainImpl
import org.veo.core.entity.impl.UnitImpl

import spock.lang.Ignore
import spock.lang.Specification

@CompileStatic
@Ignore("Activate when we have code to manage the opposite automatically")
class OppositeFeatureSpec extends Specification {

    def "Test opposite feuture of client"() {

        given : "a simple client and unit"

        Domain domain = new DomainImpl(Key.newUuid(), "demoDomain")

        Client client = new ClientImpl(Key.newUuid(), "Demo Client")

        client.addToDomains(domain)


        Unit unit = new UnitImpl(Key.newUuid(), "Demo Unit", null)

        when: "add to client"
        client.addToUnits(unit)
        Client currentClient = unit.getClient()

        then : "client is set in unit"
        currentClient == client

        when: "removed from client"
        client.removeFromUnits(unit)
        currentClient = unit.getClient()

        then: "client is set to null"
        currentClient == null

        when : "The client in unit is set, it is added to client"
        unit.setClient(client)
        boolean contained = client.getUnits().contains(unit)

        then: "The clients contains the units"
        contained == true

        when : "The client in the unit is set to null"
        unit.setClient(null)
        contained = client.getUnits().contains(unit)

        then: "the unit is removed from the client"
        contained == false
    }
}
