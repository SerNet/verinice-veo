/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.adapter.presenter.api.response.transformer

import java.time.Instant

import org.veo.adapter.presenter.api.common.ReferenceAssembler
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.usecase.service.IdRefResolver

import spock.lang.Specification

//@CompileStatic
class TransformerSpec extends Specification {

    def unitName = "Test unit"
    def unitId = "2e63d3f8-b326-4304-84e6-c12efbbcaaa4"
    def subUnitName = "Test subunit"
    def subUnitId = "fb329c3e-b87b-44d2-a680-e2d12539f3f7"

    def entityToDtoTransformer = new EntityToDtoTransformer(Mock(ReferenceAssembler), Mock(DomainAssociationTransformer))
    def idRefResolver = Mock(IdRefResolver)

    def createUnit() {
        Unit subUnit = Mock()

        subUnit.getClient() >> null
        subUnit.getDomains() >> []
        subUnit.getName() >> subUnitName
        subUnit.getIdAsString() >> subUnitId
        subUnit.getUnits() >> []
        subUnit.getModelInterface() >> Unit.getClass()

        Unit unit = Mock()
        unit.getClient() >> null
        unit.getDomains() >> []
        unit.getParent() >> null
        unit.getName() >> unitName
        unit.getIdAsString() >> unitId
        unit.getUnits() >> [subUnit]
        unit.getModelInterface() >> Unit.getClass()
        unit.createdAt >> Instant.now()
        unit.updatedAt >> Instant.now()

        subUnit.getParent() >> unit
        return unit
    }

    def "Transform Unit to UnitDto"() {
        given: "A unit with a sub-unit"
        def unit = createUnit()

        when: "the parent unit is transformed into a DTO"
        def unitDto = entityToDtoTransformer.transformUnit2Dto(unit)

        then: "The DTO contains all required data"
        unitDto.name == unitName
        unitDto.id == unitId
    }
}
