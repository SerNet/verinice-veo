/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jochen Kemnade.
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
package org.veo.persistence.access

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.entity.Asset
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.repository.PagingConfiguration
import org.veo.core.repository.PagingConfiguration.SortOrder
import org.veo.core.repository.QueryCondition
import org.veo.core.repository.RequirementImplementationQuery
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.RequirementImplementationDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.persistence.access.query.RequirementImplementationQueryImpl
import org.veo.persistence.entity.jpa.AbstractJpaSpec
import org.veo.persistence.entity.jpa.ClientData

class RequirementImplementationQuerySpec extends AbstractJpaSpec {
    @Autowired
    ClientDataRepository clientDataRepository
    @Autowired
    RequirementImplementationDataRepository repo
    @Autowired
    UnitDataRepository unitRepository

    ClientData client
    Domain domain
    Unit unit
    Asset asset

    RequirementImplementationQuery query

    def setup() {
        client = clientDataRepository.save(newClient{
            id = UUID.randomUUID()
            newDomain(it)
        })
        unit = unitRepository.save(newUnit(client))
        domain = client.domains.first()
        def control1 =
                controlDataRepository.save(newControl(unit) {
                    name = "Control 1"
                    abbreviation = "c1"
                })

        def control2 =
                controlDataRepository.save(newControl(unit) {
                    name = "Control 2"
                    abbreviation = "c2"
                })

        asset =
                assetDataRepository.save(newAsset(unit).tap {
                    implementControl(control1)
                    implementControl(control2)
                })

        query = new RequirementImplementationQueryImpl(repo, client)
    }

    def 'filter RIs IDs'() {
        when:
        query.whereIdsIn(new QueryCondition(asset.controlImplementations.collectMany {
            it.requirementImplementations
        }.collect { it.getUUID() } as Set))
        def result = query.execute(new PagingConfiguration<>(Integer.MAX_VALUE, 0, "control.abbreviation", SortOrder.DESCENDING))

        then:
        result.totalResults == 2
        result.resultPage*.control*.name == ["Control 2", "Control 1"]
    }

    def 'sort RIs by control abbreviation'() {
        when:
        def result = query.execute(new PagingConfiguration<>(Integer.MAX_VALUE, 0, "control.abbreviation", SortOrder.DESCENDING))

        then:
        result.totalResults == 2
        result.resultPage*.control*.name == ["Control 2", "Control 1"]
    }

    def 'only leaf RIs are returned'() {
        given:
        def control3a =
                controlDataRepository.save(newControl(unit) {
                    name = "Control 3a"
                    abbreviation = "c3a"
                })

        def control3 =
                controlDataRepository.save(newControl(unit) {
                    name = "Control 3"
                    abbreviation = "c3"
                    parts.add(control3a)
                })

        def control4aa =
                controlDataRepository.save(newControl(unit) {
                    name = "Control 4aa"
                    abbreviation = "c4aa"
                })
        def control4a =
                controlDataRepository.save(newControl(unit) {
                    name = "Control 4a"
                    abbreviation = "c4a"
                    parts.add(control4aa)
                })
        def control4 =
                controlDataRepository.save(newControl(unit) {
                    name = "Control 4"
                    abbreviation = "c4"
                    parts.add(control4a)
                })

        asset =
                assetDataRepository.save(assetDataRepository.findById(asset.id).get().tap {
                    implementControl(control3)
                    implementControl(control4)
                })

        when:
        def result = query.execute(new PagingConfiguration<>(Integer.MAX_VALUE, 0, "control.abbreviation", SortOrder.DESCENDING))

        then:
        result.totalResults == 4
        result.resultPage*.control*.name == [
            "Control 4aa",
            "Control 3a",
            "Control 2",
            "Control 1"
        ]
    }

    def 'paginates'() {
        expect:
        with(query.execute(new PagingConfiguration<>(1, 0, "control.name", SortOrder.ASCENDING))) {
            totalResults == 2
            resultPage*.control*.name == ["Control 1"]
        }

        with(query.execute(new PagingConfiguration<>(1, 1, "control.name", SortOrder.ASCENDING))) {
            totalResults == 2
            resultPage*.control*.name == ["Control 2"]
        }
    }
}
