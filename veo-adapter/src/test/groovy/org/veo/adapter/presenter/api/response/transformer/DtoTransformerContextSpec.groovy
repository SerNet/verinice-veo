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
package org.veo.adapter.presenter.api.response.transformer


import org.veo.adapter.presenter.api.response.AssetDto
import org.veo.adapter.presenter.api.response.DocumentDto
import org.veo.adapter.presenter.api.response.PersonDto
import org.veo.adapter.presenter.api.response.UnitDto
import org.veo.core.entity.Asset
import org.veo.core.entity.Document
import org.veo.core.entity.Key
import org.veo.core.entity.Person
import org.veo.core.entity.Unit
import org.veo.core.entity.impl.AssetImpl
import org.veo.core.entity.impl.DocumentImpl
import org.veo.core.entity.impl.PersonImpl
import org.veo.core.entity.impl.UnitImpl
import spock.lang.Specification

//@CompileStatic
class DtoTransformerContextSpec extends Specification {
    def unitName = "Test unit"
    def unitId = "2e63d3f8-b326-4304-84e6-c12efbbcaaa4"
    def subUnitName = "Test subunit"
    def subUnitId = "fb329c3e-b87b-44d2-a680-e2d12539f3f7"

    def createUnit() {
        def unit = new UnitImpl(Key.uuidFrom(unitId), unitName, null);
        def subUnit = new UnitImpl(Key.uuidFrom(subUnitId), subUnitName, null);

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
        unitDto.setUnits([subUnitDto])

        return unitDto
    }

    def "Transform Person to PersonDto with unit partial"() {
        given: "A person in a unit"
        Unit unit = createUnit()

        Person person = new PersonImpl(Key.newUuid(), "new person", unit)

        DtoEntityToTargetContext tcontext = DtoEntityToTargetContext.getCompleteTransformationContext()
        tcontext.partialUnit()

        when: "the person is transformed into a DTO"
        PersonDto personDto = PersonDto.from(person, tcontext)

        then: "The the unit has only attributes"
        personDto.name == "new person"
        personDto.owner.id == unitId
        personDto.owner.displayName == unitName
        personDto.owner.type == Unit.class
    }

    def "Transform several to Dto with unit partial"() {
        given: "A person in a unit"
        Unit unit = createUnit()

        Person person = new PersonImpl(Key.newUuid(), "new person", unit)

        Asset asset = new AssetImpl(Key.newUuid(), "new Asset", unit)


        DtoEntityToTargetContext tcontext = DtoEntityToTargetContext.getCompleteTransformationContext()
        tcontext.partialUnit()

        when: "the person is transformed into a DTO"
        PersonDto personDto = PersonDto.from(person, tcontext)

        then: "The the unit has only attributes"
        personDto.name == "new person"
        personDto.owner.id == unitId
        personDto.owner.displayName == unitName
        personDto.owner.type == Unit.class

        when: "the asset is transformed into a DTO"
        AssetDto assetDto = AssetDto.from(asset, tcontext)

        then: "The the unit has only attributes"
        assetDto.name == "new Asset"
        assetDto.owner.id == unitId
        assetDto.owner.displayName == unitName
        assetDto.owner.type == Unit.class
    }

    def "Transform DocumentDto to document with unit recplaced by context"() {
        given: "A person in a unit"
        Unit unit = createUnit()

        Document doc = new DocumentImpl(Key.newUuid(), "new document", unit)


        when: "the document is transformed into a DTO"

        DocumentDto docDto = DocumentDto.from(doc, DtoEntityToTargetContext.getCompleteTransformationContext())

        then: "Test the Dto"
        docDto.id == doc.id.uuidValue()
        docDto.name == "new document"
        docDto.owner.id == unitId
        docDto.owner.displayName == unitName

        when: "replace the unit in context"

        Unit replacementUnit = new UnitImpl(Key.uuidFrom(unitId), "replaced unit", null)

        DtoTargetToEntityContext tcontext = DtoTargetToEntityContext.getCompleteTransformationContext()
        tcontext.addEntity(replacementUnit)

        doc = docDto.toDocument(tcontext)

        then: "the unit is replaced by the context unit"

        doc.name == "new document"
        doc.owner.id.uuidValue() == unitId
        doc.owner.name == "replaced unit"
        doc.owner.units.size() == 0
    }
}
