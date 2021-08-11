/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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
package org.veo.rest.configuration;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.data.domain.AuditorAware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.veo.adapter.persistence.schema.EntitySchemaServiceClassPathImpl;
import org.veo.adapter.presenter.api.TypeDefinitionProvider;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.adapter.presenter.api.response.transformer.EntitySchemaLoader;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.adapter.presenter.api.response.transformer.SubTypeTransformer;
import org.veo.adapter.service.domaintemplate.CatalogItemPrepareStrategy;
import org.veo.adapter.service.domaintemplate.CatalogItemServiceImpl;
import org.veo.adapter.service.domaintemplate.DomainTemplateServiceImpl;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.CatalogRepository;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.DesignatorSequenceRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.repository.ProcessRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.repository.UnitRepository;
import org.veo.core.service.CatalogItemService;
import org.veo.core.service.DomainTemplateService;
import org.veo.core.service.EntitySchemaService;
import org.veo.core.usecase.DesignatorService;
import org.veo.core.usecase.asset.CreateAssetRiskUseCase;
import org.veo.core.usecase.asset.CreateAssetUseCase;
import org.veo.core.usecase.asset.GetAssetRiskUseCase;
import org.veo.core.usecase.asset.GetAssetRisksUseCase;
import org.veo.core.usecase.asset.GetAssetUseCase;
import org.veo.core.usecase.asset.GetAssetsUseCase;
import org.veo.core.usecase.asset.UpdateAssetRiskUseCase;
import org.veo.core.usecase.asset.UpdateAssetUseCase;
import org.veo.core.usecase.base.DeleteEntityUseCase;
import org.veo.core.usecase.base.UnitHierarchyProvider;
import org.veo.core.usecase.catalog.GetCatalogUseCase;
import org.veo.core.usecase.catalog.GetCatalogsUseCase;
import org.veo.core.usecase.catalogitem.ApplyIncarnationDescriptionUseCase;
import org.veo.core.usecase.catalogitem.GetCatalogItemUseCase;
import org.veo.core.usecase.catalogitem.GetCatalogItemsUseCase;
import org.veo.core.usecase.catalogitem.GetIncarnationDescriptionUseCase;
import org.veo.core.usecase.control.CreateControlUseCase;
import org.veo.core.usecase.control.GetControlUseCase;
import org.veo.core.usecase.control.GetControlsUseCase;
import org.veo.core.usecase.control.UpdateControlUseCase;
import org.veo.core.usecase.document.CreateDocumentUseCase;
import org.veo.core.usecase.document.GetDocumentUseCase;
import org.veo.core.usecase.document.GetDocumentsUseCase;
import org.veo.core.usecase.document.UpdateDocumentUseCase;
import org.veo.core.usecase.domain.GetDomainUseCase;
import org.veo.core.usecase.domain.GetDomainsUseCase;
import org.veo.core.usecase.incident.CreateIncidentUseCase;
import org.veo.core.usecase.incident.GetIncidentUseCase;
import org.veo.core.usecase.incident.GetIncidentsUseCase;
import org.veo.core.usecase.incident.UpdateIncidentUseCase;
import org.veo.core.usecase.person.CreatePersonUseCase;
import org.veo.core.usecase.person.GetPersonUseCase;
import org.veo.core.usecase.person.GetPersonsUseCase;
import org.veo.core.usecase.person.UpdatePersonUseCase;
import org.veo.core.usecase.process.CreateProcessRiskUseCase;
import org.veo.core.usecase.process.CreateProcessUseCase;
import org.veo.core.usecase.process.GetProcessRiskUseCase;
import org.veo.core.usecase.process.GetProcessRisksUseCase;
import org.veo.core.usecase.process.GetProcessUseCase;
import org.veo.core.usecase.process.GetProcessesUseCase;
import org.veo.core.usecase.process.UpdateProcessRiskUseCase;
import org.veo.core.usecase.process.UpdateProcessUseCase;
import org.veo.core.usecase.risk.DeleteRiskUseCase;
import org.veo.core.usecase.scenario.CreateScenarioUseCase;
import org.veo.core.usecase.scenario.GetScenarioUseCase;
import org.veo.core.usecase.scenario.GetScenariosUseCase;
import org.veo.core.usecase.scenario.UpdateScenarioUseCase;
import org.veo.core.usecase.scope.CreateScopeUseCase;
import org.veo.core.usecase.scope.GetScopeUseCase;
import org.veo.core.usecase.scope.GetScopesUseCase;
import org.veo.core.usecase.scope.UpdateScopeUseCase;
import org.veo.core.usecase.unit.CreateUnitUseCase;
import org.veo.core.usecase.unit.DeleteUnitUseCase;
import org.veo.core.usecase.unit.GetUnitDumpUseCase;
import org.veo.core.usecase.unit.GetUnitUseCase;
import org.veo.core.usecase.unit.GetUnitsUseCase;
import org.veo.core.usecase.unit.UpdateUnitUseCase;
import org.veo.persistence.CurrentUserProvider;
import org.veo.persistence.access.AssetRepositoryImpl;
import org.veo.persistence.access.ClientRepositoryImpl;
import org.veo.persistence.access.ControlRepositoryImpl;
import org.veo.persistence.access.DocumentRepositoryImpl;
import org.veo.persistence.access.IncidentRepositoryImpl;
import org.veo.persistence.access.PersonRepositoryImpl;
import org.veo.persistence.access.ProcessRepositoryImpl;
import org.veo.persistence.access.ScenarioRepositoryImpl;
import org.veo.persistence.access.ScopeRepositoryImpl;
import org.veo.persistence.access.StoredEventRepository;
import org.veo.persistence.access.StoredEventRepositoryImpl;
import org.veo.persistence.access.UnitRepositoryImpl;
import org.veo.persistence.access.jpa.StoredEventDataRepository;
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory;
import org.veo.rest.security.AuthAwareImpl;
import org.veo.rest.security.CurrentUserProviderImpl;

/**
 * This configuration takes care of wiring classes from core modules
 * (Entity-Layer, Use-Case-Layer) that have no dependency to the Spring
 * framework. They are therefore not picked up and autowired by Spring.
 */
@Configuration
public class ModuleConfiguration {

    @Bean
    public CreateAssetUseCase createAssetUseCase(UnitRepositoryImpl unitRepository,
            AssetRepositoryImpl assetRepository, DesignatorService designatorService) {
        return new CreateAssetUseCase(unitRepository, assetRepository, designatorService);
    }

    @Bean
    public CreateAssetRiskUseCase createAssetRiskUseCase(RepositoryProvider repositoryProvider,
            DesignatorService designatorService) {
        return new CreateAssetRiskUseCase(repositoryProvider, designatorService);
    }

    @Bean
    public UpdateAssetRiskUseCase updateAssetRiskUseCase(RepositoryProvider repositoryProvider) {
        return new UpdateAssetRiskUseCase(repositoryProvider);
    }

    @Bean
    public GetAssetUseCase getAssetUseCase(AssetRepositoryImpl assetRepository) {
        return new GetAssetUseCase(assetRepository);
    }

    @Bean
    public GetAssetsUseCase getAssetsUseCase(ClientRepositoryImpl clientRepository,
            AssetRepositoryImpl assetRepository, UnitHierarchyProvider unitHierarchyProvider) {
        return new GetAssetsUseCase(clientRepository, assetRepository, unitHierarchyProvider);
    }

    @Bean
    public UpdateAssetUseCase updateAssetUseCase(AssetRepositoryImpl assetRepository) {
        return new UpdateAssetUseCase(assetRepository);
    }

    @Bean
    public CreateControlUseCase createControlUseCase(UnitRepositoryImpl unitRepository,
            ControlRepositoryImpl controlRepository, DesignatorService designatorService) {
        return new CreateControlUseCase(unitRepository, controlRepository, designatorService);
    }

    @Bean
    public GetControlUseCase getControlUseCase(ControlRepositoryImpl controlRepository) {
        return new GetControlUseCase(controlRepository);
    }

    @Bean
    public GetControlsUseCase getControlsUseCase(ClientRepositoryImpl clientRepository,
            ControlRepositoryImpl controlRepository, UnitHierarchyProvider unitHierarchyProvider) {
        return new GetControlsUseCase(clientRepository, controlRepository, unitHierarchyProvider);
    }

    @Bean
    public UpdateControlUseCase updateControlUseCase(ControlRepositoryImpl controlRepository) {
        return new UpdateControlUseCase(controlRepository);
    }

    @Bean
    public CreateDocumentUseCase createDocumentUseCase(UnitRepositoryImpl unitRepository,
            DocumentRepositoryImpl documentRepository, DesignatorService designatorService) {
        return new CreateDocumentUseCase(unitRepository, documentRepository, designatorService);
    }

    @Bean
    public GetDocumentUseCase getDocumentUseCase(DocumentRepositoryImpl documentRepository) {
        return new GetDocumentUseCase(documentRepository);
    }

    @Bean
    public GetDocumentsUseCase getDocumentsUseCase(ClientRepositoryImpl clientRepository,
            DocumentRepositoryImpl documentRepository,
            UnitHierarchyProvider unitHierarchyProvider) {
        return new GetDocumentsUseCase(clientRepository, documentRepository, unitHierarchyProvider);
    }

    @Bean
    public UpdateDocumentUseCase updateDocumentUseCase(DocumentRepositoryImpl documentRepository) {
        return new UpdateDocumentUseCase(documentRepository);
    }

    @Bean
    public CreateScenarioUseCase createScenarioUseCase(UnitRepositoryImpl unitRepository,
            ScenarioRepositoryImpl scenarioRepository, DesignatorService designatorService) {
        return new CreateScenarioUseCase(unitRepository, scenarioRepository, designatorService);
    }

    @Bean
    public GetScenarioUseCase getScenarioUseCase(ScenarioRepositoryImpl scenarioRepository) {
        return new GetScenarioUseCase(scenarioRepository);
    }

    @Bean
    public GetScenariosUseCase getScenariosUseCase(ClientRepositoryImpl clientRepository,
            ScenarioRepositoryImpl scenarioRepository,
            UnitHierarchyProvider unitHierarchyProvider) {
        return new GetScenariosUseCase(clientRepository, scenarioRepository, unitHierarchyProvider);
    }

    @Bean
    public UpdateScenarioUseCase updateScenarioUseCase(ScenarioRepositoryImpl scenarioRepository) {
        return new UpdateScenarioUseCase(scenarioRepository);
    }

    @Bean
    public CreateIncidentUseCase createIncidentUseCase(UnitRepositoryImpl unitRepository,
            IncidentRepositoryImpl incidentRepository, DesignatorService designatorService) {
        return new CreateIncidentUseCase(unitRepository, incidentRepository, designatorService);
    }

    @Bean
    public GetIncidentUseCase getIncidentUseCase(IncidentRepositoryImpl incidentRepository) {
        return new GetIncidentUseCase(incidentRepository);
    }

    @Bean
    public GetIncidentsUseCase getIncidentsUseCase(ClientRepositoryImpl clientRepository,
            IncidentRepositoryImpl incidentRepository,
            UnitHierarchyProvider unitHierarchyProvider) {
        return new GetIncidentsUseCase(clientRepository, incidentRepository, unitHierarchyProvider);
    }

    @Bean
    public UpdateIncidentUseCase updateIncidentUseCase(IncidentRepositoryImpl incidentRepository) {
        return new UpdateIncidentUseCase(incidentRepository);
    }

    @Bean
    public CreateProcessUseCase createProcessUseCase(UnitRepositoryImpl unitRepository,
            ProcessRepositoryImpl processRepository, DesignatorService designatorService) {
        return new CreateProcessUseCase(unitRepository, processRepository, designatorService);
    }

    @Bean
    public CreateProcessRiskUseCase createProcessRiskUseCase(RepositoryProvider repositoryProvider,
            DesignatorService designatorService) {
        return new CreateProcessRiskUseCase(repositoryProvider, designatorService);
    }

    @Bean
    public GetProcessUseCase getProcessUseCase(ProcessRepositoryImpl processRepository) {
        return new GetProcessUseCase(processRepository);
    }

    @Bean
    public GetProcessRiskUseCase getProcessRiskUseCase(RepositoryProvider repositoryProvider) {
        return new GetProcessRiskUseCase(repositoryProvider);
    }

    @Bean
    public GetProcessRisksUseCase getProcessRisksUseCase(RepositoryProvider repositoryProvider) {
        return new GetProcessRisksUseCase(repositoryProvider);
    }

    @Bean
    public UpdateProcessRiskUseCase updateProcessRiskUseCase(
            RepositoryProvider repositoryProvider) {
        return new UpdateProcessRiskUseCase(repositoryProvider);
    }

    @Bean
    public UpdateProcessUseCase putProcessUseCase(ProcessRepositoryImpl processRepository) {
        return new UpdateProcessUseCase(processRepository);
    }

    @Bean
    public GetUnitUseCase getUnitUseCase(UnitRepositoryImpl repository) {
        return new GetUnitUseCase(repository);
    }

    @Bean
    public GetUnitsUseCase getUnitsUseCase(ClientRepository repository,
            UnitRepositoryImpl unitRepository) {
        return new GetUnitsUseCase(repository, unitRepository);
    }

    @Bean
    public GetProcessesUseCase getProcessesUseCase(ClientRepository clientRepository,
            ProcessRepository processRepository, UnitHierarchyProvider unitHierarchyProvider) {
        return new GetProcessesUseCase(clientRepository, processRepository, unitHierarchyProvider);
    }

    @Bean
    public UpdateUnitUseCase getPutUnitUseCase(UnitRepositoryImpl repository) {
        return new UpdateUnitUseCase(repository);
    }

    @Bean
    public CreateUnitUseCase getCreateUnitUseCase(ClientRepositoryImpl clientRepository,
            UnitRepositoryImpl unitRepository, DomainTemplateService domainTemplateService) {
        return new CreateUnitUseCase(clientRepository, unitRepository, getEntityFactory(),
                domainTemplateService);
    }

    @Bean
    public DeleteUnitUseCase getDeleteUnitUseCase(ClientRepositoryImpl clientRepository,
            UnitRepositoryImpl unitRepository, RepositoryProvider repositoryProvider) {
        return new DeleteUnitUseCase(clientRepository, unitRepository, repositoryProvider);
    }

    @Bean
    public CreatePersonUseCase createPersonUseCase(UnitRepositoryImpl unitRepository,
            PersonRepositoryImpl personRepository, DesignatorService designatorService) {
        return new CreatePersonUseCase(unitRepository, personRepository, designatorService);
    }

    @Bean
    public GetPersonUseCase getPersonUseCase(PersonRepositoryImpl personRepository) {
        return new GetPersonUseCase(personRepository);
    }

    @Bean
    public GetPersonsUseCase getPersonsUseCase(ClientRepositoryImpl clientRepository,
            PersonRepositoryImpl personRepository, UnitHierarchyProvider unitHierarchyProvider) {
        return new GetPersonsUseCase(clientRepository, personRepository, unitHierarchyProvider);
    }

    @Bean
    public UpdatePersonUseCase updatePersonUseCase(PersonRepositoryImpl personRepository) {
        return new UpdatePersonUseCase(personRepository);
    }

    @Bean
    public CreateScopeUseCase createScopeUseCase(UnitRepositoryImpl unitRepository,
            ScopeRepositoryImpl scopeRepository, DesignatorService designatorService) {
        return new CreateScopeUseCase(unitRepository, scopeRepository, designatorService);
    }

    @Bean
    public GetScopeUseCase getScopeUseCase(ScopeRepositoryImpl scopeRepository) {
        return new GetScopeUseCase(scopeRepository);
    }

    @Bean
    public GetScopesUseCase getScopesUseCase(ClientRepositoryImpl clientRepository,
            ScopeRepositoryImpl scopeRepository, UnitHierarchyProvider unitHierarchyProvider) {
        return new GetScopesUseCase(clientRepository, scopeRepository, unitHierarchyProvider);
    }

    @Bean
    public UpdateScopeUseCase updateScopeUseCase(ScopeRepositoryImpl scopeRepository) {
        return new UpdateScopeUseCase(scopeRepository);
    }

    @Bean
    public DeleteEntityUseCase deleteEntityUseCase(RepositoryProvider repositoryProvider) {
        return new DeleteEntityUseCase(repositoryProvider);
    }

    @Bean
    public DeleteRiskUseCase deleteRiskUseCase(RepositoryProvider repositoryProvider) {
        return new DeleteRiskUseCase(repositoryProvider);
    }

    @Bean
    public EntitySchemaService getSchemaService(
            @Value("${veo.entity_schemas_location:/schemas/entity/}") String schemaFilePath) {
        return new EntitySchemaServiceClassPathImpl(schemaFilePath);
    }

    @Bean
    public EntityFactory getEntityFactory() {
        return new EntityDataFactory();
    }

    @Bean
    public UnitHierarchyProvider unitHierarchyProvider(UnitRepository unitRepository) {
        return new UnitHierarchyProvider(unitRepository);
    }

    @Bean
    SubTypeTransformer aspectTransformer() {
        return new SubTypeTransformer();
    }

    @Bean
    public GetAssetRiskUseCase getAssetRiskUseCase(RepositoryProvider repositoryProvider) {
        return new GetAssetRiskUseCase(repositoryProvider);
    }

    @Bean
    public GetAssetRisksUseCase getAssetRisksUseCase(RepositoryProvider repositoryProvider) {
        return new GetAssetRisksUseCase(repositoryProvider);
    }

    @Bean
    public GetDomainUseCase getDomainUseCase(DomainRepository domainRepository) {
        return new GetDomainUseCase(domainRepository);
    }

    @Bean
    public GetDomainsUseCase getDomainsUseCase() {
        return new GetDomainsUseCase();
    }

    @Bean
    public GetCatalogUseCase getCatalogUseCase(CatalogRepository catalogRepository) {
        return new GetCatalogUseCase(catalogRepository);
    }

    @Bean
    public GetCatalogsUseCase getCatalogsUseCase() {
        return new GetCatalogsUseCase();
    }

    @Bean
    public GetCatalogItemUseCase getCatalogItemUseCase() {
        return new GetCatalogItemUseCase();
    }

    @Bean
    public GetCatalogItemsUseCase getCatalogItemsUseCase() {
        return new GetCatalogItemsUseCase();
    }

    @Bean
    public StoredEventRepository storedEventRepository(
            StoredEventDataRepository storedEventDataRepository) {
        return new StoredEventRepositoryImpl(storedEventDataRepository);
    }

    @Bean
    public AuthAwareImpl authAwareImpl() {
        return new AuthAwareImpl();
    }

    @Bean
    public EntityToDtoTransformer entityToDtoTransformer(ReferenceAssembler referenceAssembler,
            SubTypeTransformer subTypeTransformer) {
        return new EntityToDtoTransformer(referenceAssembler, subTypeTransformer);
    }

    @Bean
    public DtoToEntityTransformer dtoToEntityTransformer(EntityFactory entityFactory,
            EntitySchemaService entitySchemaService, SubTypeTransformer subTypeTransformer) {
        return new DtoToEntityTransformer(entityFactory,
                new EntitySchemaLoader(entitySchemaService), subTypeTransformer);
    }

    @Primary
    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
        eventMulticaster.setTaskExecutor(new SyncTaskExecutor());
        return eventMulticaster;
    }

    @Bean
    public CurrentUserProvider currentUserProvider(AuditorAware<String> auditorAware) {
        return new CurrentUserProviderImpl(auditorAware);
    }

    @Bean
    public DesignatorService designatorService(
            DesignatorSequenceRepository designatorSequenceRepository) {
        return new DesignatorService(designatorSequenceRepository);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setChannelTransacted(false);
        template.setMandatory(true);
        return template;
    }

    @Bean
    public DomainTemplateServiceImpl domainTemplateService(
            DomainTemplateRepository domainTemplateRepository, EntityFactory factory,
            DomainTemplateResource domainTemplateResource, EntityToDtoTransformer dtoTransformer,
            SubTypeTransformer subTypeTransformer, CatalogItemPrepareStrategy prepareStrategy) {
        return new DomainTemplateServiceImpl(domainTemplateRepository, factory,
                domainTemplateResource.getResources(), dtoTransformer, subTypeTransformer,
                prepareStrategy);
    }

    @Bean
    public CatalogItemService catalogItemService(DtoToEntityTransformer entityTransformer,
            EntityToDtoTransformer dtoTransformer, EntityFactory factory,
            SubTypeTransformer subTypeTransformer, CatalogItemPrepareStrategy prepareStrategy) {
        return new CatalogItemServiceImpl(dtoTransformer, factory, subTypeTransformer,
                prepareStrategy);
    }

    @Bean
    public CatalogItemPrepareStrategy catalogItemPrepareStrategy() {
        return new CatalogItemPrepareStrategy();
    }

    @Bean
    public TypeDefinitionProvider getTypeDefinitionProvider(ReferenceAssembler referenceAssembler) {
        return new TypeDefinitionProvider(referenceAssembler);
    }

    @Bean
    public GetIncarnationDescriptionUseCase getIncarnationDescriptionUseCase(
            org.veo.core.repository.UnitRepository unitRepository,
            CatalogItemRepository catalogItemRepository,
            org.veo.core.repository.RepositoryProvider entityRepository) {
        return new GetIncarnationDescriptionUseCase(unitRepository, catalogItemRepository,
                entityRepository);
    }

    @Bean
    public GetUnitDumpUseCase getUnitDumpUseCase(RepositoryProvider repositoryProvider,
            UnitRepository unitRepository) {
        return new GetUnitDumpUseCase(repositoryProvider, unitRepository);
    }

    @Bean
    public ApplyIncarnationDescriptionUseCase applyIncarnationDescriptionUseCase(
            org.veo.core.repository.UnitRepository unitRepository,
            CatalogItemRepository catalogItemRepository,
            org.veo.core.repository.RepositoryProvider repositoryProvider,
            DesignatorService designatorService, CatalogItemService catalogItemService) {
        return new ApplyIncarnationDescriptionUseCase(unitRepository, catalogItemRepository,
                repositoryProvider, designatorService, catalogItemService);
    }
}
