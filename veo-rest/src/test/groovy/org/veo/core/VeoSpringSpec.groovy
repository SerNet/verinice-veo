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
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate

import org.veo.adapter.service.domaintemplate.DomainTemplateServiceImpl
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.usecase.unit.DeleteUnitUseCase
import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.CatalogDataRepository
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
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.rest.RestApplication
import org.veo.rest.configuration.WebMvcSecurityConfiguration
import org.veo.test.VeoSpec

/**
 * Base class for veo specifications that use Spring
 */
@SpringBootTest(classes = RestApplication)
@ActiveProfiles("test")
@Import(NopEntityValidationConfiguration)
@ImportAutoConfiguration
@ComponentScan("org.veo")
abstract class VeoSpringSpec extends VeoSpec {
    // dsgvo-test-1.json
    public static final String DSGVO_TEST_DOMAIN_TEMPLATE_ID = "00000000-0000-0000-0000-000000000001"

    // test-domain.json
    public static final String TEST_DOMAIN_TEMPLATE_ID = "2b00d864-77ee-5378-aba6-e41f618c7bad"

    @Autowired
    ClientDataRepository clientDataRepository

    @Autowired
    CatalogDataRepository catalogDataRepository

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
    DomainTemplateServiceImpl domainTemplateService

    @Autowired
    TransactionTemplate txTemplate

    @Autowired
    DeleteUnitUseCase deleteUnitUseCase

    def deleteUnitRecursively(Unit unit) {
        unit.units.each {
            deleteUnitRecursively(it)
        }
        deleteUnitUseCase.execute(new DeleteUnitUseCase.InputData(unit.id,
                unit.client))
    }

    def setup() {
        txTemplate.execute {
            clientDataRepository.findAll().each{ client->
                unitDataRepository.findByClientId(client.idAsString).findAll { it.parent == null }.each{
                    deleteUnitRecursively(it)
                }
                clientDataRepository.delete(client)
            }
            eventStoreDataRepository.deleteAll()
        }
    }

    Client createTestClient() {
        return clientDataRepository.save(newClient {
            id = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)
        })
    }

    Domain createDsgvoDomain(Client client) {
        return txTemplate.execute {
            def domain = domainTemplateService.createDomain(client, DomainTemplateServiceImpl.DSGVO_DOMAINTEMPLATE_UUID)
            client.addToDomains(domain)
            clientDataRepository.save(client)
            return domain
        }
    }

    Domain createDsgvoTestDomain(Client client) {
        return txTemplate.execute {
            def domain = domainTemplateService.createDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
            client.addToDomains(domain)
            clientDataRepository.save(client)
            return domain
        }
    }

    Domain createTestDomain(Client client) {
        return txTemplate.execute {
            def domain = domainTemplateService.createDomain(client, TEST_DOMAIN_TEMPLATE_ID)
            client.addToDomains(domain)
            clientDataRepository.save(client)
            return domain
        }
    }
}
