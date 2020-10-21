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


import java.time.Instant

import org.veo.adapter.presenter.api.common.ReferenceAssembler
import org.veo.adapter.presenter.api.dto.AbstractUnitDto
import org.veo.adapter.presenter.api.dto.full.FullDocumentDto
import org.veo.adapter.presenter.api.dto.full.FullUnitDto
import org.veo.core.entity.Document
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.entity.transform.ClassKey
import org.veo.core.entity.transform.EntityFactory

import spock.lang.Specification

//@CompileStatic
class DtoTransformerContextSpec extends Specification {
    def unitName = "Test unit"
    def unitId = "2e63d3f8-b326-4304-84e6-c12efbbcaaa4"
    def subUnitName = "Test subunit"
    def subUnitId = "fb329c3e-b87b-44d2-a680-e2d12539f3f7"
    def personName = "new Person"
    def assetName = "new Asset"
    def docName = "new Document"

    def createUnit() {
        Unit subUnit = Mock()

        subUnit.getClient() >> null
        subUnit.getDomains() >> []
        subUnit.getName() >> unitName
        subUnit.getId() >> Key.uuidFrom(unitId)
        subUnit.getModelInterface() >> Unit.class
        subUnit.getCreatedAt() >> Instant.now()
        subUnit.getDisplayName() >> unitName

        Unit unit = Mock()
        unit.getClient() >> null
        unit.getDomains() >> []
        unit.getParent() >> null
        unit.getName() >> unitName
        unit.getDisplayName() >> unitName
        unit.getId() >> Key.uuidFrom(unitId)
        unit.getUnits() >> [subUnit]
        unit.getModelInterface() >> Unit.class
        unit.getCreatedAt() >> Instant.now()

        subUnit.getParent() >> unit
        return unit
    }

    def AbstractUnitDto createUnitDto() {
        def subUnitDto = new FullUnitDto()
        subUnitDto.setId(subUnitId)
        subUnitDto.setName(subUnitName)

        def unitDto = new FullUnitDto()
        unitDto.setId(unitId)
        unitDto.setName(unitName)
        unitDto.setUnits([subUnitDto])

        return unitDto
    }

    def "Transform DocumentDto to document with unit recplaced by context"() {
        given: "A person in a unit"
        Unit unit = createUnit()

        Document doc = Mock()
        doc.getName() >> docName
        doc.getId() >> Key.newUuid()
        doc.getDomains() >> []
        doc.getLinks() >> []
        doc.getLinks() >> []
        doc.getCustomAspects() >> []
        doc.getOwner() >> unit
        doc.getModelInterface() >> Document.class
        doc.getCreatedAt() >> Instant.now()
        doc.getUpdatedAt() >> Instant.now()

        def newDoc = Mock(Document)

        EntityFactory factory = Mock() {
            createDocument(_,_,_) >> newDoc
        }

        Unit replacementUnit = Mock()
        ReferenceAssembler assembler = Mock()

        replacementUnit.getClient() >> null
        replacementUnit.getDomains() >> []
        replacementUnit.getName() >> "replaced unit"
        replacementUnit.getId() >> Key.uuidFrom(unitId)
        replacementUnit.getModelInterface() >> Unit.class
        replacementUnit.getCreatedAt() >> Instant.now()
        replacementUnit.getUpdatedAt() >> Instant.now()

        when: "the document is transformed into a DTO"

        def docDto = FullDocumentDto.from(doc, assembler)

        then: "Test the Dto"
        docDto.id == doc.id.uuidValue()
        docDto.name == docName
        docDto.owner.id == unitId
        docDto.owner.displayName == unitName

        when: "replace the unit in context"

        def tcontext = Mock(DtoToEntityContext) {
            it.loader >> factory
            it.context >> new HashMap<>()
            it.context.put(new ClassKey<Key<UUID>>(Unit, replacementUnit.id), replacementUnit)
        }

        doc = docDto.toEntity(tcontext)

        then: "the unit is replaced by the context unit"
        1 * newDoc.setOwner(replacementUnit)
        1 * newDoc.setName(docName)
    }
}
