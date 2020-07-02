/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
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
package org.veo.adapter.presenter.api.response.transformer


import org.veo.adapter.presenter.api.response.ClientDto
import org.veo.adapter.presenter.api.response.DomainDto
import org.veo.adapter.presenter.api.response.UnitDto
import org.veo.core.entity.Key
import org.veo.core.entity.impl.ClientImpl
import org.veo.core.entity.impl.DomainImpl
import org.veo.core.entity.impl.UnitImpl
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

    def UnitDto createUnitDto() {
        def subUnitDto = new UnitDto()
        subUnitDto.setId(subUnitId)
        subUnitDto.setName(subUnitName)

        def unitDto = new UnitDto()
        unitDto.setId(unitId)
        unitDto.setName(unitName)
        unitDto.setUnits([subUnitDto] as Set)

        return unitDto
    }

    def "Transform Unit to UnitDto"() {
        given: "A unit with a sub-unit"
        def unit = createUnit()

        when: "the parent unit is transformed into a DTO"
        def unitDto = UnitDto.from(unit, DtoEntityToTargetContext.getCompleteTransformationContext())

        then: "The DTO contains all required data"
        unitDto.name == "Test unit"
        unitDto.id == unitId

        then: "The subunit was correctly transformed into a DTO"
        unitDto.units.size() == 1
        unitDto.units.first().name == "Test subunit"
        unitDto.units.first().id == subUnitId
    }

    def "Transform UnitDto to Unit"() {
        given: " A unit DTO with a subunit DTO"
        def unitDto = createUnitDto()

        when: "The parent unit DTO is transformed into a unit"
        def unit = unitDto.toUnit(DtoTargetToEntityContext.getCompleteTransformationContext())

        then: "The unit contains all data"
        unit.id.uuidValue() == unitId
        unit.name == unitName

        then: "The subunit was correctly converted into a DTO"
        unit.units.size() == 1
        unit.units.first().name == subUnitName
        unit.units.first().id.uuidValue() == subUnitId
    }

    def "Transform Client to ClientDto"() {
        given: "A Client with a unit"
        def unit = createUnit()
        def domain = new DomainImpl(Key.uuidFrom(domainId), domainName)
        domain.setVersion(1L)
        domain.setDescription(domainDescription)

        def client = new ClientImpl(Key.uuidFrom(clientId), clientName)
        client.setVersion(1L)
        client.setDomains([domain] as Set)
        client.setUnits([unit] as Set)

        when: "the client is transformed into a DTO"
        def clientDto = ClientDto.from(client, DtoEntityToTargetContext.getCompleteTransformationContext())

        then: "The DTO contains all required data"
        unit.id.uuidValue() == unitId
        unit.name == unitName

        then: "The unit subunit was correctly transformed into a DTO"
        def unitDto = clientDto.units.first()

        unitDto.units.size() == 1
        unitDto.units.first().name == "Test subunit"
        unitDto.units.first().id == subUnitId
    }

    def "Transform ClientDto to Client"() {
        given: "A client DTO with a unit and a domain"
        def unitDto = createUnitDto()

        def domainDto = new DomainDto()
        domainDto.setId(domainId)
        domainDto.setName(domainName)

        def clientDto = new ClientDto()
        clientDto.setId(clientId)
        clientDto.setName(clientName)
        clientDto.setUnits([unitDto] as Set)
        clientDto.setDomains([domainDto] as Set)

        when: "the DTO is transformed into a Client"
        def client = clientDto.toClient(DtoTargetToEntityContext.getCompleteTransformationContext())

        then: "the client contains all relevant fields"
        client.id.uuidValue() == clientId
        client.name == clientName

        and: "the unit DTO with subunit was also converted"
        client.units.first().id.uuidValue() == unitId
        client.units.first().name == unitName
        client.units.first().units.first().id.uuidValue() == subUnitId

        and: "the domain DTO was also converted"
        client.domains.first().id.uuidValue() == domainId
        client.domains.first().name == domainName
    }



}
