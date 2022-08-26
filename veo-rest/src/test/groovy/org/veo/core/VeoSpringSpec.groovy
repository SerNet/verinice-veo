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
import org.veo.core.entity.Element
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.repository.PagingConfiguration
import org.veo.core.usecase.unit.DeleteUnitUseCase
import org.veo.jobs.SpringSpecDomainTemplateCreator
import org.veo.persistence.access.ElementQueryImpl
import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.CatalogDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.ControlDataRepository
import org.veo.persistence.access.jpa.DocumentDataRepository
import org.veo.persistence.access.jpa.DomainDataRepository
import org.veo.persistence.access.jpa.DomainTemplateDataRepository
import org.veo.persistence.access.jpa.ElementDataRepository
import org.veo.persistence.access.jpa.IncidentDataRepository
import org.veo.persistence.access.jpa.PersonDataRepository
import org.veo.persistence.access.jpa.ProcessDataRepository
import org.veo.persistence.access.jpa.ScenarioDataRepository
import org.veo.persistence.access.jpa.ScopeDataRepository
import org.veo.persistence.access.jpa.StoredEventDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.rest.RestApplication
import org.veo.rest.configuration.WebMvcSecurityConfiguration
import org.veo.service.DefaultDomainCreator
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
    // Name-Based UUID: https://v.de/veo/domain-templates/dsgvo/v1.4.0
    public static final String DSGVO_DOMAINTEMPLATE_UUID = "e96dd67f-090e-52be-8b76-f66b75624b45"

    // Name-Based UUID: https://v.de/veo/domain-templates/dsgvo/v2.0.0
    public static final String DSGVO_DOMAINTEMPLATE_V2_UUID = "0482da04-fc3a-5af5-9911-54a1a326116c"

    // dsgvo-test-1.json
    public static final String DSGVO_TEST_DOMAIN_TEMPLATE_ID = "3d4321c8-8764-52fb-b6d4-2480672038ed"

    // test-domain.json
    public static final String TEST_DOMAIN_TEMPLATE_ID = "6b27accf-594b-5750-8b4f-4f869762225c"

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
    DefaultDomainCreator defaultDomainCreator

    @Autowired
    TransactionTemplate txTemplate

    @Autowired
    DeleteUnitUseCase deleteUnitUseCase

    @Autowired
    SpringSpecDomainTemplateCreator domainTemplateCreator

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
            domainTemplateDataRepository.deleteAll()
        }
    }

    def createTestDomainTemplate(String templateId) {
        domainTemplateCreator.createTestTemplate(templateId)
    }

    Client createTestClient() {
        return clientDataRepository.save(newClient {
            id = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)
        })
    }

    Domain createTestDomain(Client client, String templateId) {
        return txTemplate.execute {
            return domainTemplateCreator.createDomainFromTemplate(templateId, client)
        }
    }

    Domain deactivateDomain(String domainId) {
        def domain = domainDataRepository.findById(domainId).orElseThrow()
        domain.setActive(false)
        return domainDataRepository.save(domain)
    }

    def <T> T executeInTransaction(Closure<T> cl) {
        txTemplate.execute {
            cl.call()
        }
    }

    List<Element> findByUnit(ElementDataRepository repository, Unit unit) {
        new ElementQueryImpl(repository, unit.client).whereOwnerIs(unit).execute(PagingConfiguration.UNPAGED).resultPage
    }
}
