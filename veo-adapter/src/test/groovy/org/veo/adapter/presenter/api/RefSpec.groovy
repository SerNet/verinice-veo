/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
package org.veo.adapter.presenter.api

import org.veo.adapter.presenter.api.common.IdRef
import org.veo.adapter.presenter.api.common.ReferenceAssembler
import org.veo.core.entity.Asset
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.entity.ref.TypedId

import spock.lang.Specification

class RefSpec extends Specification{

    ReferenceAssembler referenceAssembler = Mock()

    def "ref equals & hashcode is implemented correctly"() {
        given:
        def unit1 = Spy(Unit) {
            id >> Key.newUuid()
            displayName >> ""
        }
        def unit1Doppelganger = Spy(Unit) {
            id >> unit1.id
            displayName >> ""
        }
        def unit2 = Spy(Unit) {
            id >> Key.newUuid()
            displayName >> ""
        }
        def assetWithSameId = Spy(Asset) {
            id >> unit1.id
            displayName >> ""
        }

        expect:
        IdRef.from(unit1, referenceAssembler) == IdRef.from(unit1, referenceAssembler)
        IdRef.from(unit1, referenceAssembler).hashCode() == IdRef.from(unit1, referenceAssembler).hashCode()

        IdRef.from(unit1Doppelganger, referenceAssembler) == IdRef.from(unit1, referenceAssembler)
        IdRef.from(unit1Doppelganger, referenceAssembler).hashCode() == IdRef.from(unit1, referenceAssembler).hashCode()

        IdRef.from(unit1, referenceAssembler) != IdRef.from(unit2, referenceAssembler)
        IdRef.from(unit1, referenceAssembler).hashCode() != IdRef.from(unit2, referenceAssembler).hashCode()

        IdRef.from(unit1, referenceAssembler) != IdRef.from(assetWithSameId, referenceAssembler)
        IdRef.from(unit1, referenceAssembler).hashCode() != IdRef.from(assetWithSameId, referenceAssembler).hashCode()

        and:
        TypedId.from(unit1) == TypedId.from(unit1)
        TypedId.from(unit1).hashCode() == TypedId.from(unit1).hashCode()

        TypedId.from(unit1Doppelganger) == TypedId.from(unit1)
        TypedId.from(unit1Doppelganger).hashCode() == TypedId.from(unit1).hashCode()

        TypedId.from(unit1) != TypedId.from(unit2)
        TypedId.from(unit1).hashCode() != TypedId.from(unit2).hashCode()

        TypedId.from(unit1) != TypedId.from(assetWithSameId)
        TypedId.from(unit1).hashCode() != TypedId.from(assetWithSameId).hashCode()

        and:
        IdRef.from(unit1, referenceAssembler) == TypedId.from(unit1)
        TypedId.from(unit1) == IdRef.from(unit1, referenceAssembler)
        TypedId.from(unit1).hashCode() == IdRef.from(unit1, referenceAssembler).hashCode()

        IdRef.from(unit1, referenceAssembler) == TypedId.from(unit1Doppelganger)
        TypedId.from(unit1Doppelganger) == IdRef.from(unit1, referenceAssembler)
        TypedId.from(unit1Doppelganger).hashCode() == IdRef.from(unit1, referenceAssembler).hashCode()

        IdRef.from(unit2, referenceAssembler) != TypedId.from(unit1)
        TypedId.from(unit1) != IdRef.from(unit2, referenceAssembler)
        TypedId.from(unit1).hashCode() != IdRef.from(unit2, referenceAssembler).hashCode()

        IdRef.from(assetWithSameId, referenceAssembler) != TypedId.from(unit1)
        TypedId.from(unit1) != IdRef.from(assetWithSameId, referenceAssembler)
        TypedId.from(unit1).hashCode() != IdRef.from(assetWithSameId, referenceAssembler).hashCode()
    }
}
