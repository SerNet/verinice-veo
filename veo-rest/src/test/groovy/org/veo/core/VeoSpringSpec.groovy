/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
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
package org.veo.core

import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate

import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.CatalogDataRepository
import org.veo.persistence.access.jpa.CatalogItemDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.ControlDataRepository
import org.veo.persistence.access.jpa.DocumentDataRepository
import org.veo.persistence.access.jpa.DomainDataRepository
import org.veo.persistence.access.jpa.DomainTemplateDataRepository
import org.veo.persistence.access.jpa.IncidentDataRepository
import org.veo.persistence.access.jpa.PersonDataRepository
import org.veo.persistence.access.jpa.ProcessDataRepository
import org.veo.persistence.access.jpa.ScenarioDataRepository
import org.veo.persistence.access.jpa.ScopeDataRepository
import org.veo.persistence.access.jpa.StoredEventDataRepository
import org.veo.persistence.access.jpa.TailoringReferenceDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.persistence.access.jpa.UpdateReferenceDataRepository
import org.veo.test.VeoSpec

/**
 * Base class for veo specifications that use Spring
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@WithUserDetails("user@domain.example")
abstract class VeoSpringSpec extends VeoSpec {

    @Autowired
    ClientDataRepository clientDataRepository

    @Autowired
    CatalogDataRepository catalogDataRepository

    @Autowired
    CatalogItemDataRepository catalogItemDataRepository

    @Autowired
    TailoringReferenceDataRepository tailoringReferenceDataRepository

    @Autowired
    UpdateReferenceDataRepository updategReferenceDataRepository

    @Autowired
    DomainTemplateDataRepository domainTemplateDataRepository

    @Autowired
    DomainDataRepository domainDataRepository

    @Autowired
    UnitDataRepository unitDataRepository

    @Autowired
    AssetDataRepository assetDataRepository

    @Autowired
    ControlDataRepository controlDataRepository

    @Autowired
    DocumentDataRepository documentDataRepository

    @Autowired
    IncidentDataRepository incidentDataRepository

    @Autowired
    ScenarioDataRepository scenarioDataRepository

    @Autowired
    PersonDataRepository personDataRepository

    @Autowired
    ProcessDataRepository processDataRepository

    @Autowired
    ScopeDataRepository scopeDataRepository

    @Autowired
    StoredEventDataRepository eventStoreDataRepository

    @Autowired
    TransactionTemplate txTemplate

    def setup() {
        txTemplate.execute {
            [
                processDataRepository,
                assetDataRepository,
                controlDataRepository,
                documentDataRepository,
                incidentDataRepository,
                scenarioDataRepository,
                personDataRepository,
                scopeDataRepository
            ].each {
                it.findAll().forEach {
                    it.links.clear()
                }
            }
            [
                scopeDataRepository,
                tailoringReferenceDataRepository,
                updategReferenceDataRepository,
                catalogItemDataRepository,
                assetDataRepository,
                processDataRepository,
                controlDataRepository,
                documentDataRepository,
                incidentDataRepository,
                scenarioDataRepository,
                personDataRepository,
                unitDataRepository,
                domainDataRepository,
                clientDataRepository,
                domainTemplateDataRepository,
                eventStoreDataRepository,
            ]*.deleteAll()
        }
    }
}
