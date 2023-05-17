/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
package org.veo.persistence.entity.jpa

import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

import org.veo.core.entity.Asset
import org.veo.core.entity.Unit
import org.veo.core.repository.PagingConfiguration
import org.veo.persistence.JpaTestConfig
import org.veo.persistence.VeoJpaConfiguration
import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.ControlDataRepository
import org.veo.persistence.access.jpa.DocumentDataRepository
import org.veo.persistence.access.jpa.ElementDataRepository
import org.veo.persistence.access.jpa.IncidentDataRepository
import org.veo.persistence.access.jpa.PersonDataRepository
import org.veo.persistence.access.jpa.ProcessDataRepository
import org.veo.persistence.access.jpa.ScenarioDataRepository
import org.veo.persistence.access.jpa.ScopeDataRepository
import org.veo.persistence.access.query.ElementQueryFactory
import org.veo.test.VeoSpec

@DataJpaTest
@ContextConfiguration(classes = [VeoJpaConfiguration, JpaTestConfig])
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
abstract class AbstractJpaSpec extends VeoSpec {

    @Autowired
    ElementDataRepository elementDataRepository

    @Autowired
    AssetDataRepository assetDataRepository

    @Autowired
    ControlDataRepository controlDataRepository

    @Autowired
    DocumentDataRepository documentDataRepository

    @Autowired
    IncidentDataRepository incidentDataRepository

    @Autowired
    PersonDataRepository personDataRepository

    @Autowired
    ProcessDataRepository processDataRepository

    @Autowired
    ScenarioDataRepository scenarioDataRepository

    @Autowired
    ScopeDataRepository scopeDataRepository

    ElementQueryFactory elementQueryFactory

    def setup() {
        elementQueryFactory = new ElementQueryFactory(
                elementDataRepository,
                assetDataRepository,
                controlDataRepository,
                documentDataRepository,
                incidentDataRepository,
                personDataRepository,
                processDataRepository,
                scenarioDataRepository,
                scopeDataRepository,
                )
    }

    List<Asset> findAssetsByUnit(Unit unit) {
        return elementQueryFactory
                .queryAssets(unit.client)
                .with {
                    whereOwnerIs(unit)
                    execute(PagingConfiguration.UNPAGED)
                            .resultPage
                }
    }
}
