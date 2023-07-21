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

import java.util.Arrays;
import java.util.stream.Collectors;

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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.veo.adapter.SchemaReplacer;
import org.veo.adapter.persistence.schema.EntitySchemaGenerator;
import org.veo.adapter.persistence.schema.EntitySchemaServiceImpl;
import org.veo.adapter.persistence.schema.SchemaExtender;
import org.veo.adapter.presenter.api.TypeDefinitionProvider;
import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.dto.AbstractRiskDto;
import org.veo.adapter.presenter.api.response.transformer.DomainAssociationTransformer;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.adapter.service.ObjectSchemaParser;
import org.veo.adapter.service.domaintemplate.DomainTemplateIdGeneratorImpl;
import org.veo.adapter.service.domaintemplate.DomainTemplateServiceImpl;
import org.veo.adapter.service.domaintemplate.ReferenceDeserializer;
import org.veo.adapter.service.domaintemplate.dto.TransformElementDto;
import org.veo.adapter.service.domaintemplate.dto.TransformRiskDto;
import org.veo.core.entity.AccountProvider;
import org.veo.core.entity.specification.EntityValidator;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.entity.transform.IdentifiableFactory;
import org.veo.core.events.MessageCreatorImpl;
import org.veo.core.repository.AssetRepository;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.CatalogRepository;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.DesignatorSequenceRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.ProcessRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.repository.ScopeRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.service.DomainTemplateIdGenerator;
import org.veo.core.service.DomainTemplateService;
import org.veo.core.service.EntitySchemaService;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.DesignatorService;
import org.veo.core.usecase.IncomingMessageHandler;
import org.veo.core.usecase.InspectElementUseCase;
import org.veo.core.usecase.MessageCreator;
import org.veo.core.usecase.asset.CreateAssetRiskUseCase;
import org.veo.core.usecase.asset.GetAssetRiskUseCase;
import org.veo.core.usecase.asset.GetAssetRisksUseCase;
import org.veo.core.usecase.asset.GetAssetUseCase;
import org.veo.core.usecase.asset.GetAssetsUseCase;
import org.veo.core.usecase.asset.UpdateAssetRiskUseCase;
import org.veo.core.usecase.asset.UpdateAssetUseCase;
import org.veo.core.usecase.base.AddLinksUseCase;
import org.veo.core.usecase.base.AssociateElementWithDomainUseCase;
import org.veo.core.usecase.base.CreateElementUseCase;
import org.veo.core.usecase.base.DeleteElementUseCase;
import org.veo.core.usecase.base.GenericGetElementsUseCase;
import org.veo.core.usecase.base.UnitHierarchyProvider;
import org.veo.core.usecase.base.UpdateAssetInDomainUseCase;
import org.veo.core.usecase.base.UpdateControlInDomainUseCase;
import org.veo.core.usecase.base.UpdateDocumentInDomainUseCase;
import org.veo.core.usecase.base.UpdateIncidentInDomainUseCase;
import org.veo.core.usecase.base.UpdatePersonInDomainUseCase;
import org.veo.core.usecase.base.UpdateProcessInDomainUseCase;
import org.veo.core.usecase.base.UpdateScenarioInDomainUseCase;
import org.veo.core.usecase.base.UpdateScopeInDomainUseCase;
import org.veo.core.usecase.catalog.GetCatalogUseCase;
import org.veo.core.usecase.catalog.GetCatalogsUseCase;
import org.veo.core.usecase.catalogitem.ApplyIncarnationDescriptionUseCase;
import org.veo.core.usecase.catalogitem.GetCatalogItemUseCase;
import org.veo.core.usecase.catalogitem.GetCatalogItemsUseCase;
import org.veo.core.usecase.catalogitem.GetIncarnationDescriptionUseCase;
import org.veo.core.usecase.client.DeleteClientUseCase;
import org.veo.core.usecase.client.GetClientUseCase;
import org.veo.core.usecase.control.GetControlUseCase;
import org.veo.core.usecase.control.GetControlsUseCase;
import org.veo.core.usecase.control.UpdateControlUseCase;
import org.veo.core.usecase.decision.Decider;
import org.veo.core.usecase.decision.EvaluateElementUseCase;
import org.veo.core.usecase.document.GetDocumentUseCase;
import org.veo.core.usecase.document.GetDocumentsUseCase;
import org.veo.core.usecase.document.UpdateDocumentUseCase;
import org.veo.core.usecase.domain.ApplyProfileUseCase;
import org.veo.core.usecase.domain.CreateDomainFromTemplateUseCase;
import org.veo.core.usecase.domain.CreateDomainUseCase;
import org.veo.core.usecase.domain.DeleteDecisionUseCase;
import org.veo.core.usecase.domain.DeleteDomainUseCase;
import org.veo.core.usecase.domain.DeleteRiskDefinitionUseCase;
import org.veo.core.usecase.domain.ElementBatchCreator;
import org.veo.core.usecase.domain.ExportDomainUseCase;
import org.veo.core.usecase.domain.GetDomainUseCase;
import org.veo.core.usecase.domain.GetDomainsUseCase;
import org.veo.core.usecase.domain.GetElementStatusCountUseCase;
import org.veo.core.usecase.domain.ProfileApplier;
import org.veo.core.usecase.domain.SaveDecisionUseCase;
import org.veo.core.usecase.domain.SaveRiskDefinitionUseCase;
import org.veo.core.usecase.domain.UpdateAllClientDomainsUseCase;
import org.veo.core.usecase.domain.UpdateElementTypeDefinitionUseCase;
import org.veo.core.usecase.domaintemplate.CreateDomainTemplateFromDomainUseCase;
import org.veo.core.usecase.domaintemplate.CreateDomainTemplateUseCase;
import org.veo.core.usecase.domaintemplate.FindDomainTemplatesUseCase;
import org.veo.core.usecase.domaintemplate.GetDomainTemplateUseCase;
import org.veo.core.usecase.incident.GetIncidentUseCase;
import org.veo.core.usecase.incident.GetIncidentsUseCase;
import org.veo.core.usecase.incident.UpdateIncidentUseCase;
import org.veo.core.usecase.inspection.Inspector;
import org.veo.core.usecase.person.GetPersonUseCase;
import org.veo.core.usecase.person.GetPersonsUseCase;
import org.veo.core.usecase.person.UpdatePersonUseCase;
import org.veo.core.usecase.process.CreateProcessRiskUseCase;
import org.veo.core.usecase.process.GetProcessRiskUseCase;
import org.veo.core.usecase.process.GetProcessRisksUseCase;
import org.veo.core.usecase.process.GetProcessUseCase;
import org.veo.core.usecase.process.GetProcessesUseCase;
import org.veo.core.usecase.process.UpdateProcessRiskUseCase;
import org.veo.core.usecase.process.UpdateProcessUseCase;
import org.veo.core.usecase.risk.DeleteRiskUseCase;
import org.veo.core.usecase.scenario.GetScenarioUseCase;
import org.veo.core.usecase.scenario.GetScenariosUseCase;
import org.veo.core.usecase.scenario.UpdateScenarioUseCase;
import org.veo.core.usecase.scope.CreateScopeRiskUseCase;
import org.veo.core.usecase.scope.GetScopeRiskUseCase;
import org.veo.core.usecase.scope.GetScopeRisksUseCase;
import org.veo.core.usecase.scope.GetScopeUseCase;
import org.veo.core.usecase.scope.GetScopesUseCase;
import org.veo.core.usecase.scope.UpdateScopeRiskUseCase;
import org.veo.core.usecase.scope.UpdateScopeUseCase;
import org.veo.core.usecase.service.EntityStateMapper;
import org.veo.core.usecase.unit.CreateUnitUseCase;
import org.veo.core.usecase.unit.DeleteUnitUseCase;
import org.veo.core.usecase.unit.GetUnitDumpUseCase;
import org.veo.core.usecase.unit.GetUnitUseCase;
import org.veo.core.usecase.unit.GetUnitsUseCase;
import org.veo.core.usecase.unit.UnitImportUseCase;
import org.veo.core.usecase.unit.UnitValidator;
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
import org.veo.persistence.entity.jpa.ReferenceSerializationModule;
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory;
import org.veo.persistence.entity.jpa.transformer.IdentifiableDataFactory;
import org.veo.rest.security.AuthAwareImpl;
import org.veo.rest.security.CurrentUserProviderImpl;
import org.veo.service.CatalogMigrationService;
import org.veo.service.DefaultDomainCreator;
import org.veo.service.ElementMigrationService;
import org.veo.service.EtagService;
import org.veo.service.risk.RiskService;

/**
 * This configuration takes care of wiring classes from core modules (Entity-Layer, Use-Case-Layer)
 * that have no dependency to the Spring framework. They are therefore not picked up and autowired
 * by Spring.
 */
@Configuration
public class ModuleConfiguration {

  @Bean
  public CreateElementUseCase createElementUseCase(
      RepositoryProvider repositoryProvider,
      DesignatorService designatorService,
      EventPublisher eventPublisher,
      IdentifiableFactory identifiableFactory,
      EntityStateMapper entityStateMapper,
      Decider decider) {
    return new CreateElementUseCase(
        repositoryProvider,
        designatorService,
        eventPublisher,
        identifiableFactory,
        entityStateMapper,
        decider);
  }

  @Bean
  public CreateAssetRiskUseCase createAssetRiskUseCase(
      RepositoryProvider repositoryProvider,
      DesignatorService designatorService,
      EventPublisher eventPublisher) {
    return new CreateAssetRiskUseCase(repositoryProvider, designatorService, eventPublisher);
  }

  @Bean
  public UpdateAssetRiskUseCase updateAssetRiskUseCase(
      RepositoryProvider repositoryProvider, EventPublisher eventPublisher) {
    return new UpdateAssetRiskUseCase(repositoryProvider, eventPublisher);
  }

  @Bean
  public GenericGetElementsUseCase genericGetElementsUseCase(
      ClientRepository clientRepository,
      GenericElementRepository elementRepository,
      UnitHierarchyProvider unitHierarchyProvider) {
    return new GenericGetElementsUseCase(
        clientRepository, elementRepository, unitHierarchyProvider);
  }

  @Bean
  public GetAssetUseCase getAssetUseCase(
      AssetRepositoryImpl assetRepository, DomainRepository domainRepository) {
    return new GetAssetUseCase(assetRepository, domainRepository);
  }

  @Bean
  public GetAssetsUseCase getAssetsUseCase(
      ClientRepositoryImpl clientRepository,
      AssetRepositoryImpl assetRepository,
      UnitHierarchyProvider unitHierarchyProvider) {
    return new GetAssetsUseCase(clientRepository, assetRepository, unitHierarchyProvider);
  }

  @Bean
  public UpdateAssetUseCase updateAssetUseCase(
      RepositoryProvider repositoryProvider, Decider decider, EventPublisher eventPublisher) {
    return new UpdateAssetUseCase(
        repositoryProvider, decider, getEntityStateMapper(), eventPublisher);
  }

  @Bean
  public GetControlUseCase getControlUseCase(
      ControlRepositoryImpl controlRepository, DomainRepository domainRepository) {
    return new GetControlUseCase(controlRepository, domainRepository);
  }

  @Bean
  public GetControlsUseCase getControlsUseCase(
      ClientRepositoryImpl clientRepository,
      ControlRepositoryImpl controlRepository,
      UnitHierarchyProvider unitHierarchyProvider) {
    return new GetControlsUseCase(clientRepository, controlRepository, unitHierarchyProvider);
  }

  @Bean
  public UpdateControlUseCase updateControlUseCase(
      RepositoryProvider repositoryProvider, EventPublisher eventPublisher, Decider decider) {
    return new UpdateControlUseCase(
        repositoryProvider, eventPublisher, decider, getEntityStateMapper());
  }

  @Bean
  public GetDocumentUseCase getDocumentUseCase(
      DocumentRepositoryImpl documentRepository, DomainRepository domainRepository) {
    return new GetDocumentUseCase(documentRepository, domainRepository);
  }

  @Bean
  public GetDocumentsUseCase getDocumentsUseCase(
      ClientRepositoryImpl clientRepository,
      DocumentRepositoryImpl documentRepository,
      UnitHierarchyProvider unitHierarchyProvider) {
    return new GetDocumentsUseCase(clientRepository, documentRepository, unitHierarchyProvider);
  }

  @Bean
  public UpdateDocumentUseCase updateDocumentUseCase(
      RepositoryProvider repositoryProvider, Decider decider) {
    return new UpdateDocumentUseCase(repositoryProvider, decider, getEntityStateMapper());
  }

  @Bean
  public GetScenarioUseCase getScenarioUseCase(
      ScenarioRepositoryImpl scenarioRepository, DomainRepository domainRepository) {
    return new GetScenarioUseCase(scenarioRepository, domainRepository);
  }

  @Bean
  public GetScenariosUseCase getScenariosUseCase(
      ClientRepositoryImpl clientRepository,
      ScenarioRepositoryImpl scenarioRepository,
      UnitHierarchyProvider unitHierarchyProvider) {
    return new GetScenariosUseCase(clientRepository, scenarioRepository, unitHierarchyProvider);
  }

  @Bean
  public UpdateScenarioUseCase updateScenarioUseCase(
      RepositoryProvider repositoryProvider, EventPublisher eventPublisher, Decider decider) {
    return new UpdateScenarioUseCase(
        repositoryProvider, eventPublisher, decider, getEntityStateMapper());
  }

  @Bean
  public GetIncidentUseCase getIncidentUseCase(
      IncidentRepositoryImpl incidentRepository, DomainRepository domainRepository) {
    return new GetIncidentUseCase(incidentRepository, domainRepository);
  }

  @Bean
  public GetIncidentsUseCase getIncidentsUseCase(
      ClientRepositoryImpl clientRepository,
      IncidentRepositoryImpl incidentRepository,
      UnitHierarchyProvider unitHierarchyProvider) {
    return new GetIncidentsUseCase(clientRepository, incidentRepository, unitHierarchyProvider);
  }

  @Bean
  public UpdateIncidentUseCase updateIncidentUseCase(
      RepositoryProvider repositoryProvider, Decider decider) {
    return new UpdateIncidentUseCase(repositoryProvider, decider, getEntityStateMapper());
  }

  @Bean
  public CreateProcessRiskUseCase createProcessRiskUseCase(
      RepositoryProvider repositoryProvider,
      DesignatorService designatorService,
      EventPublisher eventPublisher) {
    return new CreateProcessRiskUseCase(repositoryProvider, designatorService, eventPublisher);
  }

  @Bean
  public GetProcessUseCase getProcessUseCase(
      ProcessRepositoryImpl processRepository, DomainRepository domainRepository) {
    return new GetProcessUseCase(processRepository, domainRepository);
  }

  @Bean
  public GetProcessRiskUseCase getProcessRiskUseCase(RepositoryProvider repositoryProvider) {
    return new GetProcessRiskUseCase(repositoryProvider);
  }

  @Bean
  public GetProcessRisksUseCase getProcessRisksUseCase(
      RepositoryProvider repositoryProvider, ProcessRepository processRepository) {
    return new GetProcessRisksUseCase(repositoryProvider, processRepository);
  }

  @Bean
  public UpdateProcessRiskUseCase updateProcessRiskUseCase(
      RepositoryProvider repositoryProvider, EventPublisher eventPublisher) {
    return new UpdateProcessRiskUseCase(repositoryProvider, eventPublisher);
  }

  @Bean
  public UpdateProcessUseCase putProcessUseCase(
      RepositoryProvider repositoryProvider, EventPublisher eventPublisher, Decider decider) {
    return new UpdateProcessUseCase(
        repositoryProvider, eventPublisher, decider, getEntityStateMapper());
  }

  @Bean
  public GetUnitUseCase getUnitUseCase(UnitRepositoryImpl repository) {
    return new GetUnitUseCase(repository);
  }

  @Bean
  public GetUnitsUseCase getUnitsUseCase(
      ClientRepository repository, UnitRepositoryImpl unitRepository) {
    return new GetUnitsUseCase(repository, unitRepository);
  }

  @Bean
  public GetProcessesUseCase getProcessesUseCase(
      ClientRepository clientRepository,
      ProcessRepository processRepository,
      UnitHierarchyProvider unitHierarchyProvider) {
    return new GetProcessesUseCase(clientRepository, processRepository, unitHierarchyProvider);
  }

  @Bean
  public UnitValidator unitValidator(GenericElementRepository genericElementRepository) {
    return new UnitValidator(genericElementRepository);
  }

  @Bean
  public UpdateUnitUseCase getPutUnitUseCase(
      UnitRepositoryImpl repository,
      UnitValidator unitValidator,
      EntityStateMapper entityStateMapper,
      RepositoryProvider repositoryProvider) {
    return new UpdateUnitUseCase(repository, unitValidator, entityStateMapper, repositoryProvider);
  }

  @Bean
  public EntityStateMapper getEntityStateMapper() {
    return new EntityStateMapper(getEntityFactory());
  }

  @Bean
  public CreateUnitUseCase getCreateUnitUseCase(
      ClientRepository clientRepository,
      UnitRepositoryImpl unitRepository,
      DomainRepository domainRepository) {
    return new CreateUnitUseCase(
        clientRepository, unitRepository, domainRepository, getEntityFactory());
  }

  @Bean
  public DeleteUnitUseCase getDeleteUnitUseCase(
      ClientRepositoryImpl clientRepository,
      UnitRepositoryImpl unitRepository,
      GenericElementRepository elementRepository) {
    return new DeleteUnitUseCase(clientRepository, unitRepository, elementRepository);
  }

  @Bean
  public GetPersonUseCase getPersonUseCase(
      PersonRepositoryImpl personRepository, DomainRepository domainRepository) {
    return new GetPersonUseCase(personRepository, domainRepository);
  }

  @Bean
  public GetPersonsUseCase getPersonsUseCase(
      ClientRepositoryImpl clientRepository,
      PersonRepositoryImpl personRepository,
      UnitHierarchyProvider unitHierarchyProvider) {
    return new GetPersonsUseCase(clientRepository, personRepository, unitHierarchyProvider);
  }

  @Bean
  public UpdatePersonUseCase updatePersonUseCase(
      RepositoryProvider repositoryProvider, Decider decider) {
    return new UpdatePersonUseCase(repositoryProvider, decider, getEntityStateMapper());
  }

  @Bean
  public GetScopeUseCase getScopeUseCase(
      DomainRepository domainRepository, ScopeRepositoryImpl scopeRepository) {
    return new GetScopeUseCase(domainRepository, scopeRepository);
  }

  @Bean
  public GetScopesUseCase getScopesUseCase(
      ClientRepositoryImpl clientRepository,
      ScopeRepositoryImpl scopeRepository,
      UnitHierarchyProvider unitHierarchyProvider) {
    return new GetScopesUseCase(clientRepository, scopeRepository, unitHierarchyProvider);
  }

  @Bean
  public UpdateScopeUseCase updateScopeUseCase(
      RepositoryProvider repositoryProvider, Decider decider, EventPublisher eventPublisher) {
    return new UpdateScopeUseCase(
        repositoryProvider, decider, getEntityStateMapper(), eventPublisher);
  }

  @Bean
  public DeleteElementUseCase deleteElementUseCase(
      RepositoryProvider repositoryProvider, EventPublisher eventPublisher) {
    return new DeleteElementUseCase(repositoryProvider, eventPublisher);
  }

  @Bean
  public DeleteRiskUseCase deleteRiskUseCase(
      RepositoryProvider repositoryProvider, EventPublisher eventPublisher) {
    return new DeleteRiskUseCase(repositoryProvider, eventPublisher);
  }

  @Bean
  public SchemaExtender schemaExtender() {
    return new SchemaExtender();
  }

  @Bean
  public EntitySchemaGenerator entitySchemaGenerator(SchemaExtender schemaExtender) {
    return new EntitySchemaGenerator(schemaExtender);
  }

  @Bean
  public EntitySchemaService getSchemaService(EntitySchemaGenerator generateEntitytSchema) {
    return new EntitySchemaServiceImpl(generateEntitytSchema);
  }

  @Bean
  public EntityFactory getEntityFactory() {
    return new EntityDataFactory();
  }

  @Bean
  public IdentifiableFactory identifiableFactory() {
    return new IdentifiableDataFactory();
  }

  @Bean
  public UnitHierarchyProvider unitHierarchyProvider(UnitRepository unitRepository) {
    return new UnitHierarchyProvider(unitRepository);
  }

  @Bean
  public DomainAssociationTransformer domainAssociationTransformer() {
    return new DomainAssociationTransformer();
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
  public CreateDomainUseCase createDomainUseCase(
      EntityFactory entityFactory,
      DomainRepository domainRepository,
      DomainTemplateRepository domainTemplateRepository) {
    return new CreateDomainUseCase(entityFactory, domainRepository, domainTemplateRepository);
  }

  @Bean
  public DeleteDomainUseCase deleteDomainUseCase(
      UnitRepository unitRepository, DomainRepository domainRepository) {
    return new DeleteDomainUseCase(domainRepository, unitRepository);
  }

  @Bean
  public CreateDomainTemplateFromDomainUseCase createDomainTemplateFromDomainUseCase(
      DomainTemplateService domainTemplateService,
      DomainRepository domainRepository,
      DomainTemplateRepository domainTemplateRepository) {
    return new CreateDomainTemplateFromDomainUseCase(
        domainTemplateService, domainRepository, domainTemplateRepository);
  }

  @Bean
  public GetDomainUseCase getDomainUseCase(DomainRepository domainRepository) {
    return new GetDomainUseCase(domainRepository);
  }

  @Bean
  public GetDomainsUseCase getDomainsUseCase(DomainRepository domainRepository) {
    return new GetDomainsUseCase(domainRepository);
  }

  @Bean
  public ExportDomainUseCase exportDomainUseCase(
      DomainRepository domainRepository, DomainTemplateService domainTemplateService) {
    return new ExportDomainUseCase(domainRepository, domainTemplateService);
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
  public EntityToDtoTransformer entityToDtoTransformer(
      ReferenceAssembler referenceAssembler,
      DomainAssociationTransformer domainAssociationTransformer) {
    return new EntityToDtoTransformer(referenceAssembler, domainAssociationTransformer);
  }

  @Bean
  public DtoToEntityTransformer dtoToEntityTransformer(
      EntityFactory entityFactory,
      IdentifiableFactory identifiableFactory,
      EntitySchemaService entitySchemaService) {
    return new DtoToEntityTransformer(entityFactory, identifiableFactory, getEntityStateMapper());
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
      DomainTemplateRepository domainTemplateRepository,
      EntityFactory factory,
      IdentifiableFactory identifiableFactory,
      DomainAssociationTransformer domainAssociationTransformer,
      DomainTemplateIdGenerator domainTemplateIdGenerator,
      ReferenceAssembler referenceAssembler) {

    ObjectMapper objectMapper =
        new ObjectMapper()
            .addMixIn(AbstractElementDto.class, TransformElementDto.class)
            .addMixIn(AbstractRiskDto.class, TransformRiskDto.class)
            .registerModule(
                new SimpleModule()
                    .addDeserializer(IdRef.class, new ReferenceDeserializer(referenceAssembler)))
            .registerModule(new ReferenceSerializationModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return new DomainTemplateServiceImpl(
        domainTemplateRepository,
        factory,
        domainAssociationTransformer,
        identifiableFactory,
        domainTemplateIdGenerator,
        referenceAssembler,
        objectMapper,
        getEntityStateMapper());
  }

  @Bean
  public TypeDefinitionProvider getTypeDefinitionProvider(ReferenceAssembler referenceAssembler) {
    return new TypeDefinitionProvider(referenceAssembler);
  }

  @Bean
  public GetIncarnationDescriptionUseCase getIncarnationDescriptionUseCase(
      UnitRepository unitRepository,
      CatalogItemRepository catalogItemRepository,
      RepositoryProvider repositoryProvider) {
    return new GetIncarnationDescriptionUseCase(
        unitRepository, catalogItemRepository, repositoryProvider);
  }

  @Bean
  GetClientUseCase getClientUseCase(ClientRepository clientRepository) {
    return new GetClientUseCase(clientRepository);
  }

  @Bean
  public DeleteClientUseCase deleteClientUseCase(
      AccountProvider accountProvider,
      ClientRepository clientRepository,
      UnitRepository unitRepository,
      DeleteUnitUseCase deleteUnitUseCase) {
    return new DeleteClientUseCase(
        accountProvider, clientRepository, deleteUnitUseCase, unitRepository);
  }

  @Bean
  public GetUnitDumpUseCase getUnitDumpUseCase(
      AccountProvider accountProvider,
      GenericElementRepository genericElementRepository,
      UnitRepository unitRepository,
      DomainRepository domainRepository) {
    return new GetUnitDumpUseCase(
        accountProvider, genericElementRepository, unitRepository, domainRepository);
  }

  @Bean
  public UnitImportUseCase unitImportUseCase(
      UnitRepository unitRepository, ElementBatchCreator elementBatchCreator) {
    return new UnitImportUseCase(unitRepository, elementBatchCreator);
  }

  @Bean
  public EntityValidator entityValidator(AccountProvider accountProvider) {
    return new EntityValidator(accountProvider);
  }

  @Bean
  public ApplyIncarnationDescriptionUseCase applyIncarnationDescriptionUseCase(
      UnitRepository unitRepository,
      CatalogItemRepository catalogItemRepository,
      DomainRepository domainRepository,
      RepositoryProvider repositoryProvider,
      DesignatorService designatorService,
      EntityFactory factory) {
    return new ApplyIncarnationDescriptionUseCase(
        unitRepository,
        catalogItemRepository,
        domainRepository,
        repositoryProvider,
        designatorService,
        factory);
  }

  @Bean
  public GetScopeRiskUseCase getScopeRiskUseCase(RepositoryProvider repositoryProvider) {
    return new GetScopeRiskUseCase(repositoryProvider);
  }

  @Bean
  public GetScopeRisksUseCase getScopeRisksUseCase(RepositoryProvider repositoryProvider) {
    return new GetScopeRisksUseCase(repositoryProvider);
  }

  @Bean
  public CreateScopeRiskUseCase createScopeRiskUseCase(
      RepositoryProvider repositoryProvider,
      DesignatorService designatorService,
      EventPublisher eventPublisher) {
    return new CreateScopeRiskUseCase(repositoryProvider, designatorService, eventPublisher);
  }

  @Bean
  public UpdateScopeRiskUseCase updateScopeRiskUseCase(
      RepositoryProvider repositoryProvider, EventPublisher eventPublisher) {
    return new UpdateScopeRiskUseCase(repositoryProvider, eventPublisher);
  }

  @Bean
  public EtagService etagService(RepositoryProvider repositoryProvider) {
    return new EtagService(repositoryProvider);
  }

  @Bean
  public ObjectSchemaParser objectSchemaParser(EntityFactory entityFactory) {
    return new ObjectSchemaParser(entityFactory);
  }

  @Bean
  public UpdateElementTypeDefinitionUseCase getUpdateElementTypeDefinitionUseCase(
      DomainRepository domainRepository) {
    return new UpdateElementTypeDefinitionUseCase(domainRepository);
  }

  @Bean
  public CreateDomainFromTemplateUseCase createDomainFromTemplateUseCase(
      AccountProvider accountProvider,
      ClientRepository clientRepository,
      DomainTemplateService domainTemplateService) {
    return new CreateDomainFromTemplateUseCase(
        accountProvider, clientRepository, domainTemplateService);
  }

  @Bean
  public MessageCreator messageCreator(
      StoredEventRepository storedEventRepository,
      ObjectMapper objectMapper,
      ReferenceAssembler referenceAssembler,
      EntityToDtoTransformer entityToDtoTransformer) {
    return new MessageCreatorImpl(
        storedEventRepository, objectMapper, referenceAssembler, entityToDtoTransformer);
  }

  @Bean
  public IncomingMessageHandler incomingMessageHandler(
      RepositoryProvider repositoryProvider,
      ElementMigrationService elementMigrationService,
      CatalogMigrationService catalogMigrationService) {
    return new IncomingMessageHandler(
        repositoryProvider, elementMigrationService, catalogMigrationService);
  }

  @Bean
  public ElementMigrationService elementMigrationService() {
    return new ElementMigrationService();
  }

  @Bean
  public UpdateAllClientDomainsUseCase getUpdateAllClientDomainsUseCase(
      DomainRepository domainRepository,
      RepositoryProvider repositoryProvider,
      UnitRepository unitRepository,
      ElementMigrationService elementMigrationService,
      Decider decider) {
    return new UpdateAllClientDomainsUseCase(
        domainRepository, repositoryProvider, unitRepository, elementMigrationService, decider);
  }

  @Bean
  public DomainTemplateIdGenerator domainTemplateIdGenerator() {
    return new DomainTemplateIdGeneratorImpl();
  }

  @Bean
  public DefaultDomainCreator defaultDomainTemplateProvider(
      @Value("${veo.default.domaintemplate.names}") String[] defaultDomainTemplateIds,
      DomainTemplateService domainService,
      DomainTemplateRepository domainTemplateRepository) {
    return new DefaultDomainCreator(
        Arrays.stream(defaultDomainTemplateIds).collect(Collectors.toSet()),
        domainService,
        domainTemplateRepository);
  }

  @Bean
  public CreateDomainTemplateUseCase createDomainTemplateUseCase(
      DomainTemplateRepository domainTemplateRepository,
      DomainTemplateIdGenerator domainTemplateIdGenerator) {
    return new CreateDomainTemplateUseCase(domainTemplateRepository, domainTemplateIdGenerator);
  }

  @Bean
  public FindDomainTemplatesUseCase findDomainTemplatesUseCase(
      DomainTemplateRepository domainTemplateRepository) {
    return new FindDomainTemplatesUseCase(domainTemplateRepository);
  }

  @Bean
  public GetDomainTemplateUseCase getDomainTemplateUseCase(
      DomainTemplateService templateService, ClientRepository clientRepository) {
    return new GetDomainTemplateUseCase(templateService, clientRepository);
  }

  @Bean
  public RiskService riskService(
      ProcessRepository processRepository,
      AssetRepository assetRepository,
      ScopeRepository scopeRepository,
      EventPublisher publisher) {
    return new RiskService(processRepository, assetRepository, scopeRepository, publisher);
  }

  @Bean
  public Decider decider(ClientRepository clientRepository, RepositoryProvider repositoryProvider) {
    return new Decider(clientRepository, repositoryProvider);
  }

  @Bean
  public SchemaReplacer schemaReplacer() {
    return new SchemaReplacer();
  }

  @Bean
  public EvaluateElementUseCase evaluateElementUseCase(
      DomainRepository domainRepository,
      RepositoryProvider repositoryProvider,
      Decider decider,
      Inspector inspector) {
    return new EvaluateElementUseCase(domainRepository, repositoryProvider, decider, inspector);
  }

  @Bean
  public Inspector inspector() {
    return new Inspector();
  }

  @Bean
  InspectElementUseCase inspectElementUseCase(
      DomainRepository domainRepository,
      RepositoryProvider repositoryProvider,
      Inspector inspector) {
    return new InspectElementUseCase(domainRepository, repositoryProvider, inspector);
  }

  @Bean
  CatalogMigrationService catalogItemMigrationService(
      ElementMigrationService elementMigrationService,
      CatalogItemRepository catalogItemRepository) {
    return new CatalogMigrationService(elementMigrationService, catalogItemRepository);
  }

  @Bean
  GetElementStatusCountUseCase getElementStatusCountUseCase(
      DomainRepository domainRepository,
      UnitRepository unitRepository,
      RepositoryProvider repositoryProvider) {
    return new GetElementStatusCountUseCase(domainRepository, unitRepository, repositoryProvider);
  }

  @Bean
  ApplyProfileUseCase applyProfileUseCase(
      DomainRepository domainRepository,
      ProfileApplier profileApplier,
      UnitRepository unitRepository) {
    return new ApplyProfileUseCase(domainRepository, profileApplier, unitRepository);
  }

  @Bean
  ProfileApplier profileApplier(
      DomainTemplateService domainTemplateService,
      UnitRepository unitRepository,
      ElementBatchCreator elementBatchCreator) {
    return new ProfileApplier(domainTemplateService, unitRepository, elementBatchCreator);
  }

  @Bean
  ElementBatchCreator elementBatchCreator(
      RepositoryProvider repositoryProvider,
      EventPublisher eventPublisher,
      Decider decider,
      ElementMigrationService elementMigrationService,
      DesignatorService designatorService) {
    return new ElementBatchCreator(
        repositoryProvider, eventPublisher, decider, elementMigrationService, designatorService);
  }

  @Bean
  UpdateAssetInDomainUseCase updateAssetDomainAssociationUseCase(
      RepositoryProvider repositoryProvider, Decider decider, EventPublisher eventPublisher) {
    return new UpdateAssetInDomainUseCase(
        repositoryProvider, decider, getEntityStateMapper(), eventPublisher);
  }

  @Bean
  UpdateControlInDomainUseCase updateControlInDomainUseCase(
      RepositoryProvider repositoryProvider, Decider decider) {
    return new UpdateControlInDomainUseCase(repositoryProvider, decider, getEntityStateMapper());
  }

  @Bean
  UpdateDocumentInDomainUseCase updateDocumentInDomainUseCase(
      RepositoryProvider repositoryProvider, Decider decider) {
    return new UpdateDocumentInDomainUseCase(repositoryProvider, decider, getEntityStateMapper());
  }

  @Bean
  UpdateIncidentInDomainUseCase updateIncidentInDomainUseCase(
      RepositoryProvider repositoryProvider, Decider decider) {
    return new UpdateIncidentInDomainUseCase(repositoryProvider, decider, getEntityStateMapper());
  }

  @Bean
  UpdatePersonInDomainUseCase updatePersonInDomainUseCase(
      RepositoryProvider repositoryProvider, Decider decider) {
    return new UpdatePersonInDomainUseCase(repositoryProvider, decider, getEntityStateMapper());
  }

  @Bean
  UpdateProcessInDomainUseCase updateProcessInDomainUseCase(
      RepositoryProvider repositoryProvider, Decider decider, EventPublisher eventPublisher) {
    return new UpdateProcessInDomainUseCase(
        repositoryProvider, decider, getEntityStateMapper(), eventPublisher);
  }

  @Bean
  UpdateScenarioInDomainUseCase updateScenarioInDomainUseCase(
      RepositoryProvider repositoryProvider, Decider decider, EventPublisher eventPublisher) {
    return new UpdateScenarioInDomainUseCase(
        repositoryProvider, decider, getEntityStateMapper(), eventPublisher);
  }

  @Bean
  UpdateScopeInDomainUseCase updateScopeInDomainUseCase(
      RepositoryProvider repositoryProvider, Decider decider, EventPublisher eventPublisher) {
    return new UpdateScopeInDomainUseCase(
        repositoryProvider, decider, getEntityStateMapper(), eventPublisher);
  }

  @Bean
  AssociateElementWithDomainUseCase associateElementWithDomainUseCase(
      RepositoryProvider repositoryProvider, DomainRepository domainRepository) {
    return new AssociateElementWithDomainUseCase(repositoryProvider, domainRepository);
  }

  @Bean
  AddLinksUseCase addLinksUseCase(
      DomainRepository domainRepository,
      RepositoryProvider repositoryProvider,
      EntityStateMapper entityStateMapper) {
    return new AddLinksUseCase(domainRepository, repositoryProvider, entityStateMapper);
  }

  @Bean
  SaveDecisionUseCase saveDecisionUseCase(DomainRepository domainRepository) {
    return new SaveDecisionUseCase(domainRepository);
  }

  @Bean
  SaveRiskDefinitionUseCase saveRiskDefinitionUseCase(DomainRepository domainRepository) {
    return new SaveRiskDefinitionUseCase(domainRepository);
  }

  @Bean
  DeleteDecisionUseCase deleteDecisionUseCase(DomainRepository domainRepository) {
    return new DeleteDecisionUseCase(domainRepository);
  }

  @Bean
  DeleteRiskDefinitionUseCase deleteRiskDefinitionUseCase(
      DomainRepository domainRepository, RepositoryProvider repositoryProvider) {
    return new DeleteRiskDefinitionUseCase(domainRepository, repositoryProvider);
  }
}
