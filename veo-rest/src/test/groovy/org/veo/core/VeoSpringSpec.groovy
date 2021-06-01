/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.core

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate

import org.veo.adapter.service.domaintemplate.DomainTemplateServiceImpl
import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.CatalogDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.ControlDataRepository
import org.veo.persistence.access.jpa.DocumentDataRepository
import org.veo.persistence.access.jpa.DomainTemplateDataRepository
import org.veo.persistence.access.jpa.IncidentDataRepository
import org.veo.persistence.access.jpa.PersonDataRepository
import org.veo.persistence.access.jpa.ProcessDataRepository
import org.veo.persistence.access.jpa.ScenarioDataRepository
import org.veo.persistence.access.jpa.ScopeDataRepository
import org.veo.persistence.access.jpa.StoredEventDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.test.VeoSpec

/**
 * Base class for veo specifications that use Spring
 */
@SpringBootTest
@ActiveProfiles("test")
@ImportAutoConfiguration
@WithUserDetails("user@domain.example")
@ComponentScan("org.veo")
abstract class VeoSpringSpec extends VeoSpec {

    @Autowired
    ClientDataRepository clientDataRepository

    @Autowired
    CatalogDataRepository catalogDataRepository

    @Autowired
    DomainTemplateDataRepository domainTemplateDataRepository

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
    DomainTemplateServiceImpl domainTemplateService

    @Autowired
    TransactionTemplate txTemplate

    def setup() {
        txTemplate.execute {
            def catalogs = catalogDataRepository.findAll()
            def entityDataRepositories = [
                scopeDataRepository,
                processDataRepository,
                assetDataRepository,
                controlDataRepository,
                documentDataRepository,
                incidentDataRepository,
                scenarioDataRepository,
                personDataRepository
            ]
            entityDataRepositories.each {
                def elements = it.findAll()
                elements.each {
                    it.links.clear()
                    it.domains.clear()
                    it.appliedCatalogItems.clear()
                }
            }
            catalogs.each {
                it.catalogItems.clear()
            }
            (entityDataRepositories + [
                unitDataRepository,
                clientDataRepository,
                eventStoreDataRepository
            ])*.deleteAll()
        }
    }
}
