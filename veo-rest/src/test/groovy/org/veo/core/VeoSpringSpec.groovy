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
import org.springframework.test.web.servlet.ResultActions
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SchemaValidatorsConfig
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage

import org.veo.adapter.service.domaintemplate.DomainTemplateServiceImpl
import org.veo.core.entity.Client
import org.veo.core.entity.ClientState
import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
import org.veo.core.entity.Unit
import org.veo.core.repository.ClientRepository
import org.veo.core.service.EntitySchemaService
import org.veo.core.usecase.unit.DeleteUnitUseCase
import org.veo.jobs.SpringSpecDomainTemplateCreator
import org.veo.persistence.access.jpa.AssetDataRepository
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
    public static final UUID DSGVO_DOMAINTEMPLATE_UUID = UUID.fromString("dbbf0dbd-073f-51fc-86d2-d890169a3083")

    // Name-Based UUID: https://v.de/veo/domain-templates/dsgvo/v2.0.0
    public static final UUID DSGVO_DOMAINTEMPLATE_V2_UUID = UUID.fromString("b492c7da-0033-59c3-a225-c749595d2b8d")

    // dsgvo-test-1.json
    public static final UUID DSGVO_TEST_DOMAIN_TEMPLATE_ID = UUID.fromString("fece7858-8da5-59a3-b34a-6f8f831a256f")

    // test-domain.json
    public static final UUID TEST_DOMAIN_TEMPLATE_ID = UUID.fromString("b641354b-ca8f-5d43-9e87-d3369451de89")

    @Autowired
    ClientRepository clientRepository

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

    @Autowired
    EntitySchemaService entitySchemaService

    def deleteUnitRecursively(Unit unit) {
        // Query the repository since the persistence context was cleared
        unitDataRepository.findByParentId(unit.id).each {
            deleteUnitRecursively(it)
        }
        deleteUnitUseCase.execute(new DeleteUnitUseCase.InputData(unit.id,
                unit.client))
    }

    def setup() {
        txTemplate.execute {
            TransactionSynchronizationManager.setCurrentTransactionName("TEST_TXTEMPLATE")
            clientRepository.findAll().each { client ->
                unitDataRepository.findByClientId(client.id).findAll { it.parent == null }.each {
                    deleteUnitRecursively(it)
                }
                // Reload the client since the persistence context was cleared
                clientRepository.delete(clientRepository.getById(client.id))
            }
            domainTemplateDataRepository.deleteAll()
            eventStoreDataRepository.deleteAll()
        }
    }

    def createTestDomainTemplate(UUID templateId) {
        domainTemplateCreator.createTestTemplate(templateId)
    }

    Client createTestClient() {
        return clientRepository.save(newClient {
            id = UUID.fromString(WebMvcSecurityConfiguration.TESTCLIENT_UUID)
            state = ClientState.ACTIVATED
        })
    }

    def deleteTestClient() {
        clientRepository.deleteById(UUID.fromString(WebMvcSecurityConfiguration.TESTCLIENT_UUID))
    }

    Domain createTestDomain(Client client, UUID templateId, boolean copyProfiles = true) {
        return txTemplate.execute {
            return domainTemplateCreator.createDomainFromTemplate(templateId, client, copyProfiles)
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

    JsonSchema getSchema(Client client, ElementType type) {
        parseSchema(entitySchemaService.getSchema(type, client.domains))
    }

    JsonSchema getSchema(Domain domain, ElementType elementType) {
        parseSchema(entitySchemaService.getSchema(elementType, domain))
    }

    Set<ValidationMessage> validate(Object target, ResultActions schema) {
        return parseSchema(schema.andReturn().response.contentAsString)
                .validate(new ObjectMapper().valueToTree(target))
    }

    private JsonSchema parseSchema(String schema) {
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909).getSchema(
                schema,
                new SchemaValidatorsConfig().tap {
                    // schema is used to to validate outgoing data from an API
                    writeOnly = true
                })
    }
}
