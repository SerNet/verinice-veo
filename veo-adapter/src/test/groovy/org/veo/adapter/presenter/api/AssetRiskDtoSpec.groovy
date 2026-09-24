/*
 * verinice.veo
 * Copyright (C) 2026  Jochen Kemnade
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
 */
package org.veo.adapter.presenter.api

import org.veo.adapter.presenter.api.common.IdRef
import org.veo.adapter.presenter.api.common.ReferenceAssembler
import org.veo.adapter.presenter.api.dto.full.AssetRiskDto
import org.veo.core.entity.Asset
import org.veo.core.entity.Scenario
import org.veo.core.entity.ref.TypedId

import spock.lang.Specification

class AssetRiskDtoSpec extends Specification {

    def "Two risks for different assets are not equal"() {
        given:
        ReferenceAssembler referenceAssembler = Stub()

        def scenarioId = UUID.randomUUID()
        def asset1Id = UUID.randomUUID()
        def asset2Id = UUID.randomUUID()

        def dto1 = new AssetRiskDto().tap {
            asset = new IdRef<>(TypedId.from(asset1Id, Asset), null, null, null, null)
            scenario = new IdRef<>(TypedId.from(scenarioId, Scenario), null, null, null, null)
        }
        def dto2 = new AssetRiskDto().tap {
            asset = new IdRef<>(TypedId.from(asset2Id, Asset), null, null, null, null)
            scenario = new IdRef<>(TypedId.from(scenarioId, Scenario), null, null, null, null)
        }

        expect:
        dto1 != dto2
        dto2 != dto1
    }
}