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
package org.veo.persistence.entity.jpa.transformer
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



import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.entity.impl.ClientImpl
import org.veo.core.entity.impl.DomainImpl
import org.veo.core.entity.impl.UnitImpl
import org.veo.persistence.entity.jpa.ClientData
import org.veo.persistence.entity.jpa.DomainData
import org.veo.persistence.entity.jpa.UnitData
import spock.lang.Specification

//@CompileStatic
class TransformerSpec extends Specification {
    def unitName = "Test unit"
    def unitId = "2e63d3f8-b326-4304-84e6-c12efbbcaaa4"
    def subUnitName = "Test subunit"
    def subUnitId = "fb329c3e-b87b-44d2-a680-e2d12539f3f7"
    def clientName = "New Client"
    def clientId = "c6960c88-1c71-4e0f-b429-0746d362f12b"
    def domainName = "New Domain"
    def domainId = "202ef4bc-102b-4feb-bbec-1366bcbdac0f"
    def domainDescription = "This is a domain."


    def createUnit() {
        def unit = new UnitImpl(Key.uuidFrom(unitId), unitName, null)

        def subUnit = new UnitImpl(Key.uuidFrom(subUnitId), subUnitName, null)

        unit.setUnits([subUnit] as Set)
        subUnit.setParent(unit)
        return unit
    }


    def "Transform Unit to UnitData and back"() {
        given: "A unit with a sub-unit"
        Unit unit = createUnit()

        when: "the parent unit is transformed into a Data Object"
        UnitData unitData = UnitData.from(unit)

        then: "The Data contains all required data"
        unitData.name == "Test unit"
        unitData.id == unitId

        then: "The subunit was correctly transformed into a data"
        unitData.units.size() == 1
        unitData.units.first().name == "Test subunit"
        unitData.units.first().id == subUnitId

        when: "transformed back"

        unit = unitData.toUnit()
        then: "The subunit was correctly converted back"

        unit.units.size() == 1
        unit.units.first().name == subUnitName
        unit.units.first().id.uuidValue() == subUnitId
    }

    def "Transform Client to ClientData and back"() {
        given: "A Client with a unit"
        Unit unit = createUnit()
        Domain domain = new DomainImpl(Key.uuidFrom(domainId), domainName)
        domain.setVersion(1L)
        domain.setDescription(domainDescription)

        Client client = new ClientImpl(Key.uuidFrom(clientId), clientName)
        client.setVersion(1L)
        client.setDomains([domain] as Set)
        client.setUnits([unit] as Set)

        when: "the client is transformed into a Data"
        ClientData clientData = ClientData.from(client)
        UnitData unitData = clientData.units.first()
        DomainData domainData = clientData.domains.first()

        then: "The client was transformed correcly with all subelements"
        clientData.id == clientId
        clientData.name == clientName
        clientData.version == 1
        clientData.units.size() == 1
        clientData.domains.first().id == domainId

        unitData.units.size() == 1
        unitData.units.first().name == "Test subunit"
        unitData.units.first().id == subUnitId

        domainData.name == domainName
        domainData.description == domainDescription
        domainData.id == domainId

        when: "transformed back"
        client = clientData.toClient()

        then: "The client was correctly converted back"

        client.name == clientName
        client.version == 1
        client.units.size() == 1
        client.domains.first().id.uuidValue() == domainId
        client.domains.first().description == domainDescription

        client.units.first().units.size() == 1
        client.units.first().units.first().name == "Test subunit"
        client.units.first().units.first().id.uuidValue() == subUnitId
    }

}
