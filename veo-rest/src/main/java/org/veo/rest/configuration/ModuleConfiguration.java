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

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.data.domain.AuditorAware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;

import org.veo.adapter.SchemaReplacer;
import org.veo.adapter.persistence.schema.EntitySchemaGenerator;
import org.veo.adapter.persistence.schema.EntitySchemaServiceImpl;
import org.veo.adapter.persistence.schema.SchemaExtender;
import org.veo.adapter.presenter.api.TypeDefinitionProvider;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.response.transformer.DomainAssociationTransformer;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.adapter.service.ObjectSchemaParser;
import org.veo.adapter.service.domaintemplate.DomainTemplateIdGeneratorImpl;
import org.veo.adapter.service.domaintemplate.DomainTemplateServiceImpl;
import org.veo.core.VeoConstants;
import org.veo.core.entity.AccountProvider;
import org.veo.core.entity.specification.EntityValidator;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.entity.transform.IdentifiableFactory;
import org.veo.core.events.MessageCreatorImpl;
import org.veo.core.repository.AssetRepository;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.ControlImplementationRepository;
import org.veo.core.repository.DesignatorSequenceRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.repository.FlyweightLinkRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.ProcessRepository;
import org.veo.core.repository.ProfileItemRepository;
import org.veo.core.repository.ProfileRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.repository.RequirementImplementationRepository;
import org.veo.core.repository.ScopeRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.repository.UserConfigurationRepository;
import org.veo.core.service.DomainTemplateIdGenerator;
import org.veo.core.service.DomainTemplateService;
import org.veo.core.service.EntitySchemaService;
import org.veo.core.service.EventPublisher;
import org.veo.core.service.MigrateDomainUseCase;
import org.veo.core.usecase.DesignatorService;
import org.veo.core.usecase.GetAvailableActionsUseCase;
import org.veo.core.usecase.IncomingMessageHandler;
import org.veo.core.usecase.InspectElementUseCase;
import org.veo.core.usecase.MessageCreator;
import org.veo.core.usecase.PerformActionUseCase;
import org.veo.core.usecase.asset.CreateAssetRiskUseCase;
import org.veo.core.usecase.asset.GetAssetRiskUseCase;
import org.veo.core.usecase.asset.GetAssetRisksUseCase;
import org.veo.core.usecase.asset.GetAssetUseCase;
import org.veo.core.usecase.asset.UpdateAssetRiskUseCase;
import org.veo.core.usecase.asset.UpdateAssetUseCase;
import org.veo.core.usecase.base.AddLinksUseCase;
import org.veo.core.usecase.base.AssociateElementWithDomainUseCase;
import org.veo.core.usecase.base.CreateElementUseCase;
import org.veo.core.usecase.base.DeleteElementUseCase;
import org.veo.core.usecase.base.GetElementsUseCase;
import org.veo.core.usecase.base.UnitHierarchyProvider;
import org.veo.core.usecase.base.UpdateAssetInDomainUseCase;
import org.veo.core.usecase.base.UpdateControlInDomainUseCase;
import org.veo.core.usecase.base.UpdateDocumentInDomainUseCase;
import org.veo.core.usecase.base.UpdateIncidentInDomainUseCase;
import org.veo.core.usecase.base.UpdatePersonInDomainUseCase;
import org.veo.core.usecase.base.UpdateProcessInDomainUseCase;
import org.veo.core.usecase.base.UpdateScenarioInDomainUseCase;
import org.veo.core.usecase.base.UpdateScopeInDomainUseCase;
import org.veo.core.usecase.catalogitem.ApplyCatalogIncarnationDescriptionUseCase;
import org.veo.core.usecase.catalogitem.ApplyProfileIncarnationDescriptionUseCase;
import org.veo.core.usecase.catalogitem.GetCatalogIncarnationDescriptionUseCase;
import org.veo.core.usecase.catalogitem.GetCatalogItemUseCase;
import org.veo.core.usecase.catalogitem.GetCatalogItemsUseCase;
import org.veo.core.usecase.catalogitem.GetProfileIncarnationDescriptionUseCase;
import org.veo.core.usecase.catalogitem.IncarnationDescriptionApplier;
import org.veo.core.usecase.catalogitem.QueryCatalogItemsUseCase;
import org.veo.core.usecase.client.DeleteClientUseCase;
import org.veo.core.usecase.compliance.GetControlImplementationsUseCase;
import org.veo.core.usecase.compliance.GetRequirementImplementationUseCase;
import org.veo.core.usecase.compliance.GetRequirementImplementationsByControlImplementationUseCase;
import org.veo.core.usecase.compliance.UpdateRequirementImplementationUseCase;
import org.veo.core.usecase.control.GetControlUseCase;
import org.veo.core.usecase.control.UpdateControlUseCase;
import org.veo.core.usecase.decision.Decider;
import org.veo.core.usecase.decision.EvaluateElementUseCase;
import org.veo.core.usecase.document.GetDocumentUseCase;
import org.veo.core.usecase.document.UpdateDocumentUseCase;
import org.veo.core.usecase.domain.CreateCatalogFromUnitUseCase;
import org.veo.core.usecase.domain.CreateDomainFromTemplateUseCase;
import org.veo.core.usecase.domain.CreateDomainUseCase;
import org.veo.core.usecase.domain.CreateProfileFromUnitUseCase;
import org.veo.core.usecase.domain.DeleteDecisionUseCase;
import org.veo.core.usecase.domain.DeleteDomainUseCase;
import org.veo.core.usecase.domain.DeleteInspectionUseCase;
import org.veo.core.usecase.domain.DeleteProfileUseCase;
import org.veo.core.usecase.domain.DeleteRiskDefinitionUseCase;
import org.veo.core.usecase.domain.ElementBatchCreator;
import org.veo.core.usecase.domain.ExportDomainUseCase;
import org.veo.core.usecase.domain.GetCatalogItemsTypeCountUseCase;
import org.veo.core.usecase.domain.GetClientIdsWhereDomainTemplateNotAppliedUseCase;
import org.veo.core.usecase.domain.GetDomainUseCase;
import org.veo.core.usecase.domain.GetDomainsUseCase;
import org.veo.core.usecase.domain.GetElementStatusCountUseCase;
import org.veo.core.usecase.domain.GetInspectionUseCase;
import org.veo.core.usecase.domain.GetInspectionsUseCase;
import org.veo.core.usecase.domain.SaveDecisionUseCase;
import org.veo.core.usecase.domain.SaveInspectionUseCase;
import org.veo.core.usecase.domain.SaveRiskDefinitionUseCase;
import org.veo.core.usecase.domain.UpdateAllClientDomainsUseCase;
import org.veo.core.usecase.domain.UpdateElementTypeDefinitionUseCase;
import org.veo.core.usecase.domaintemplate.CreateDomainTemplateFromDomainUseCase;
import org.veo.core.usecase.domaintemplate.CreateDomainTemplateUseCase;
import org.veo.core.usecase.domaintemplate.CreateProfileInDomainTemplateUseCase;
import org.veo.core.usecase.domaintemplate.DeleteProfileInDomainTemplateUseCase;
import org.veo.core.usecase.domaintemplate.FindDomainTemplatesUseCase;
import org.veo.core.usecase.domaintemplate.GetDomainTemplateUseCase;
import org.veo.core.usecase.incident.GetIncidentUseCase;
import org.veo.core.usecase.incident.UpdateIncidentUseCase;
import org.veo.core.usecase.inspection.Inspector;
import org.veo.core.usecase.person.GetPersonUseCase;
import org.veo.core.usecase.person.UpdatePersonUseCase;
import org.veo.core.usecase.process.CreateProcessRiskUseCase;
import org.veo.core.usecase.process.GetProcessRiskUseCase;
import org.veo.core.usecase.process.GetProcessRisksUseCase;
import org.veo.core.usecase.process.GetProcessUseCase;
import org.veo.core.usecase.process.UpdateProcessRiskUseCase;
import org.veo.core.usecase.process.UpdateProcessUseCase;
import org.veo.core.usecase.profile.GetIncarnationConfigurationUseCase;
import org.veo.core.usecase.profile.GetProfileItemUseCase;
import org.veo.core.usecase.profile.GetProfileItemsUseCase;
import org.veo.core.usecase.profile.GetProfileUseCase;
import org.veo.core.usecase.profile.GetProfilesUseCase;
import org.veo.core.usecase.profile.SaveIncarnationConfigurationUseCase;
import org.veo.core.usecase.risk.DeleteRiskUseCase;
import org.veo.core.usecase.scenario.GetScenarioUseCase;
import org.veo.core.usecase.scenario.UpdateScenarioUseCase;
import org.veo.core.usecase.scope.CreateScopeRiskUseCase;
import org.veo.core.usecase.scope.GetScopeRiskUseCase;
import org.veo.core.usecase.scope.GetScopeRisksUseCase;
import org.veo.core.usecase.scope.GetScopeUseCase;
import org.veo.core.usecase.scope.UpdateScopeRiskUseCase;
import org.veo.core.usecase.scope.UpdateScopeUseCase;
import org.veo.core.usecase.service.DomainStateMapper;
import org.veo.core.usecase.service.EntityStateMapper;
import org.veo.core.usecase.service.RefResolverFactory;
import org.veo.core.usecase.unit.CreateUnitUseCase;
import org.veo.core.usecase.unit.DeleteUnitUseCase;
import org.veo.core.usecase.unit.GetUnitDumpUseCase;
import org.veo.core.usecase.unit.GetUnitUseCase;
import org.veo.core.usecase.unit.GetUnitsUseCase;
import org.veo.core.usecase.unit.UnitImportUseCase;
import org.veo.core.usecase.unit.UnitValidator;
import org.veo.core.usecase.unit.UpdateUnitUseCase;
import org.veo.core.usecase.userconfiguration.DeleteUserConfigurationUseCase;
import org.veo.core.usecase.userconfiguration.GetAllUserConfigurationKeysUseCase;
import org.veo.core.usecase.userconfiguration.GetUserConfigurationUseCase;
import org.veo.core.usecase.userconfiguration.SaveUserConfigurationUseCase;
import org.veo.persistence.CurrentUserProvider;
import org.veo.persistence.access.AssetRepositoryImpl;
import org.veo.persistence.access.ClientRepositoryImpl;
import org.veo.persistence.access.ControlImplementationRepositoryImpl;
import org.veo.persistence.access.ControlRepositoryImpl;
import org.veo.persistence.access.DocumentRepositoryImpl;
import org.veo.persistence.access.IncidentRepositoryImpl;
import org.veo.persistence.access.PersonRepositoryImpl;
import org.veo.persistence.access.ProcessRepositoryImpl;
import org.veo.persistence.access.RequirementImplementationRepositoryImpl;
import org.veo.persistence.access.ScenarioRepositoryImpl;
import org.veo.persistence.access.ScopeRepositoryImpl;
import org.veo.persistence.access.StoredEventRepository;
import org.veo.persistence.access.StoredEventRepositoryImpl;
import org.veo.persistence.access.UnitRepositoryImpl;
import org.veo.persistence.access.jpa.ControlImplementationDataRepository;
import org.veo.persistence.access.jpa.RequirementImplementationDataRepository;
import org.veo.persistence.access.jpa.StoredEventDataRepository;
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory;
import org.veo.persistence.entity.jpa.transformer.IdentifiableDataFactory;
import org.veo.rest.VeoRestConstants;
import org.veo.rest.security.AuthAwareImpl;
import org.veo.rest.security.CurrentUserProviderImpl;
import org.veo.service.CatalogMigrationService;
import org.veo.service.ControlImplementationService;
import org.veo.service.DefaultDomainCreator;
import org.veo.service.ElementMigrationService;
import org.veo.service.EtagService;
import org.veo.service.risk.ImpactInheritanceCalculator;
import org.veo.service.risk.ImpactInheritanceCalculatorHighWatermark;
import org.veo.service.risk.RiskService;

/**
 * This configuration takes care of wiring classes from core modules (Entity-Layer, Use-Case-Layer)
 * that have no dependency to the Spring framework. They are therefore not picked up and autowired
 * by Spring.
 */
@Configuration
public class ModuleConfiguration {
  @Bean
  public SaveUserConfigurationUseCase saveUserConfigurationUseCase(
      UserConfigurationRepository userConfigurationRepository,
      EntityFactory entityFactor,
      @Value("${veo.default.user-configurations-max:10}") int maxConfigurations,
      @Value("${veo.default.user-configuration-bytes-max:4000}") int maxBytesPerConfiguration) {
    return new SaveUserConfigurationUseCase(
        userConfigurationRepository, entityFactor, maxConfigurations, maxBytesPerConfiguration);
  }

  @Bean
  public GetAllUserConfigurationKeysUseCase getAllUserConfigurationKeysUseCase(
      UserConfigurationRepository userConfigurationRepository) {
    return new GetAllUserConfigurationKeysUseCase(userConfigurationRepository);
  }

  @Bean
  public DeleteUserConfigurationUseCase deleteUserConfigurationUseCase(
      UserConfigurationRepository userConfigurationRepository) {
    return new DeleteUserConfigurationUseCase(userConfigurationRepository);
  }

  @Bean
  public GetUserConfigurationUseCase getUserConfigurationUseCase(
      UserConfigurationRepository userConfigurationRepository) {
    return new GetUserConfigurationUseCase(userConfigurationRepository);
  }

  @Bean
  public GetIncarnationConfigurationUseCase getIncarnationConfigurationUseCase(
      DomainRepository domainRepository) {
    return new GetIncarnationConfigurationUseCase(domainRepository);
  }

  @Bean
  public SaveIncarnationConfigurationUseCase saveIncarnationConfigurationUseCase(
      DomainRepository domainRepository) {
    return new SaveIncarnationConfigurationUseCase(domainRepository);
  }

  @Bean
  public GetProfilesUseCase getProfilesUseCase(ProfileRepository profileRepository) {
    return new GetProfilesUseCase(profileRepository);
  }

  @Bean
  public GetProfileUseCase getProfileUseCase(ProfileRepository profileRepository) {
    return new GetProfileUseCase(profileRepository);
  }

  @Bean
  public GetProfileItemUseCase getProfileItemUseCase(ProfileRepository profileRepository) {
    return new GetProfileItemUseCase(profileRepository);
  }

  @Bean
  public GetProfileItemsUseCase getProfileItemsUseCase(ProfileRepository profileRepository) {
    return new GetProfileItemsUseCase(profileRepository);
  }

  @Bean
  public GetCatalogItemsTypeCountUseCase getCatalogItemsTypeCountUseCase(
      DomainRepository domainRepository, CatalogItemRepository catalogItemRepository) {
    return new GetCatalogItemsTypeCountUseCase(domainRepository, catalogItemRepository);
  }

  @Bean
  public CreateElementUseCase createElementUseCase(
      RepositoryProvider repositoryProvider,
      DesignatorService designatorService,
      EventPublisher eventPublisher,
      IdentifiableFactory identifiableFactory,
      EntityStateMapper entityStateMapper,
      Decider decider,
      RefResolverFactory refResolverFactory) {
    return new CreateElementUseCase(
        refResolverFactory,
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
  public GetElementsUseCase genericGetElementsUseCase(
      ClientRepository clientRepository,
      GenericElementRepository elementRepository,
      RepositoryProvider repositoryProvider,
      UnitHierarchyProvider unitHierarchyProvider) {
    return new GetElementsUseCase(
        clientRepository, elementRepository, repositoryProvider, unitHierarchyProvider);
  }

  @Bean
  public QueryCatalogItemsUseCase queryCatalogItemsUseCase(
      DomainRepository domainRepository, CatalogItemRepository catalogItemRepository) {
    return new QueryCatalogItemsUseCase(domainRepository, catalogItemRepository);
  }

  @Bean
  public GetAssetUseCase getAssetUseCase(
      AssetRepositoryImpl assetRepository, DomainRepository domainRepository) {
    return new GetAssetUseCase(assetRepository, domainRepository);
  }

  @Bean
  public UpdateAssetUseCase updateAssetUseCase(
      RepositoryProvider repositoryProvider,
      Decider decider,
      EventPublisher eventPublisher,
      RefResolverFactory refResolverFactory) {
    return new UpdateAssetUseCase(
        repositoryProvider,
        decider,
        getEntityStateMapper(eventPublisher),
        eventPublisher,
        refResolverFactory);
  }

  @Bean
  public GetControlUseCase getControlUseCase(
      ControlRepositoryImpl controlRepository, DomainRepository domainRepository) {
    return new GetControlUseCase(controlRepository, domainRepository);
  }

  @Bean
  public UpdateControlUseCase updateControlUseCase(
      RepositoryProvider repositoryProvider,
      EventPublisher eventPublisher,
      Decider decider,
      RefResolverFactory refResolverFactory) {
    return new UpdateControlUseCase(
        repositoryProvider,
        eventPublisher,
        decider,
        getEntityStateMapper(eventPublisher),
        refResolverFactory);
  }

  @Bean
  public GetDocumentUseCase getDocumentUseCase(
      DocumentRepositoryImpl documentRepository, DomainRepository domainRepository) {
    return new GetDocumentUseCase(documentRepository, domainRepository);
  }

  @Bean
  public UpdateDocumentUseCase updateDocumentUseCase(
      RepositoryProvider repositoryProvider,
      Decider decider,
      EventPublisher eventPublisher,
      RefResolverFactory refResolverFactory) {
    return new UpdateDocumentUseCase(
        repositoryProvider, decider, getEntityStateMapper(eventPublisher), refResolverFactory);
  }

  @Bean
  public GetScenarioUseCase getScenarioUseCase(
      ScenarioRepositoryImpl scenarioRepository, DomainRepository domainRepository) {
    return new GetScenarioUseCase(scenarioRepository, domainRepository);
  }

  @Bean
  public UpdateScenarioUseCase updateScenarioUseCase(
      RepositoryProvider repositoryProvider,
      EventPublisher eventPublisher,
      Decider decider,
      RefResolverFactory refResolverFactory) {
    return new UpdateScenarioUseCase(
        repositoryProvider,
        eventPublisher,
        decider,
        getEntityStateMapper(eventPublisher),
        refResolverFactory);
  }

  @Bean
  public GetIncidentUseCase getIncidentUseCase(
      IncidentRepositoryImpl incidentRepository, DomainRepository domainRepository) {
    return new GetIncidentUseCase(incidentRepository, domainRepository);
  }

  @Bean
  public UpdateIncidentUseCase updateIncidentUseCase(
      RepositoryProvider repositoryProvider,
      Decider decider,
      EventPublisher eventPublisher,
      RefResolverFactory refResolverFactory) {
    return new UpdateIncidentUseCase(
        repositoryProvider, decider, getEntityStateMapper(eventPublisher), refResolverFactory);
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
      RepositoryProvider repositoryProvider,
      EventPublisher eventPublisher,
      Decider decider,
      RefResolverFactory refResolverFactory) {
    return new UpdateProcessUseCase(
        repositoryProvider,
        eventPublisher,
        decider,
        getEntityStateMapper(eventPublisher),
        refResolverFactory);
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
  public UnitValidator unitValidator(GenericElementRepository genericElementRepository) {
    return new UnitValidator(genericElementRepository);
  }

  @Bean
  public UpdateUnitUseCase getPutUnitUseCase(
      UnitRepositoryImpl repository,
      UnitValidator unitValidator,
      EntityStateMapper entityStateMapper,
      RefResolverFactory refResolverFactory) {
    return new UpdateUnitUseCase(repository, unitValidator, entityStateMapper, refResolverFactory);
  }

  @Bean
  public EntityStateMapper getEntityStateMapper(EventPublisher eventPublisher) {
    return new EntityStateMapper(getEntityFactory(), eventPublisher);
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
  public UpdatePersonUseCase updatePersonUseCase(
      RepositoryProvider repositoryProvider,
      Decider decider,
      EventPublisher eventPublisher,
      RefResolverFactory refResolverFactory) {
    return new UpdatePersonUseCase(
        repositoryProvider, decider, getEntityStateMapper(eventPublisher), refResolverFactory);
  }

  @Bean
  public GetScopeUseCase getScopeUseCase(
      DomainRepository domainRepository, ScopeRepositoryImpl scopeRepository) {
    return new GetScopeUseCase(domainRepository, scopeRepository);
  }

  @Bean
  public UpdateScopeUseCase updateScopeUseCase(
      RepositoryProvider repositoryProvider,
      Decider decider,
      EventPublisher eventPublisher,
      RefResolverFactory refResolverFactory) {
    return new UpdateScopeUseCase(
        repositoryProvider,
        decider,
        getEntityStateMapper(eventPublisher),
        eventPublisher,
        refResolverFactory);
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
  public CreateCatalogFromUnitUseCase createCatalogForDomainUseCase(
      GenericElementRepository genericElementRepository,
      UnitRepository unitRepository,
      DomainRepository domainRepository,
      EntityFactory factory,
      CatalogItemRepository catalogItemRepository) {
    return new CreateCatalogFromUnitUseCase(
        genericElementRepository, unitRepository, domainRepository, factory, catalogItemRepository);
  }

  @Bean
  public CreateProfileFromUnitUseCase createProfileForDomainUseCase(
      GenericElementRepository genericElementRepository,
      UnitRepository unitRepository,
      DomainRepository domainRepository,
      EntityFactory factory,
      ProfileRepository profileRepository) {
    return new CreateProfileFromUnitUseCase(
        genericElementRepository, unitRepository, domainRepository, factory, profileRepository);
  }

  @Bean
  public CreateProfileInDomainTemplateUseCase createProfileInDomainTemplateUseCase(
      GenericElementRepository genericElementRepository,
      UnitRepository unitRepository,
      DomainTemplateRepository domainTemplateRepository,
      EntityFactory factory,
      ProfileRepository profileRepository,
      DomainStateMapper domainStateMapper) {
    return new CreateProfileInDomainTemplateUseCase(
        domainTemplateRepository, profileRepository, domainStateMapper);
  }

  @Bean
  public DeleteProfileInDomainTemplateUseCase deleteProfileInDomainTemplateUseCase(
      DomainTemplateRepository domainTemplateRepository) {
    return new DeleteProfileInDomainTemplateUseCase(domainTemplateRepository);
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
  public RefResolverFactory refResolverFactory(
      RepositoryProvider repositoryProvider, IdentifiableFactory identifiableFactory) {
    return new RefResolverFactory(repositoryProvider, identifiableFactory);
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
  public DomainAssociationTransformer domainAssociationTransformer(
      ReferenceAssembler referenceAssembler) {
    return new DomainAssociationTransformer(referenceAssembler);
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
  public GetRequirementImplementationUseCase getRequirementImplementationUseCase(
      RepositoryProvider repositoryProvider) {
    return new GetRequirementImplementationUseCase(repositoryProvider);
  }

  @Bean
  public UpdateRequirementImplementationUseCase updateRequirementImplementationUseCase(
      RepositoryProvider repositoryProvider,
      EntityStateMapper entityStateMapper,
      RefResolverFactory refResolverFactory) {
    return new UpdateRequirementImplementationUseCase(
        repositoryProvider, refResolverFactory, entityStateMapper);
  }

  @Bean
  public GetRequirementImplementationsByControlImplementationUseCase
      retrieveRequirementImplementationsUseCase(
          RepositoryProvider repositoryProvider,
          RequirementImplementationRepository requirementImplementationRepository) {
    return new GetRequirementImplementationsByControlImplementationUseCase(
        repositoryProvider, requirementImplementationRepository);
  }

  @Bean
  public GetControlImplementationsUseCase retrieveGetControlImplementationsByControlUseCase(
      ControlImplementationRepository controlImplementationRepository,
      GenericElementRepository genericElementRepository) {
    return new GetControlImplementationsUseCase(
        controlImplementationRepository, genericElementRepository);
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
  public ExportDomainUseCase exportDomainUseCase(DomainRepository domainRepository) {
    return new ExportDomainUseCase(domainRepository);
  }

  @Bean
  public GetCatalogItemUseCase getCatalogItemUseCase(
      DomainRepository domainRepository, CatalogItemRepository catalogItemRepository) {
    return new GetCatalogItemUseCase(domainRepository, catalogItemRepository);
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
  public RequirementImplementationRepository requirementImplementationRepository(
      RequirementImplementationDataRepository dataRepository) {
    return new RequirementImplementationRepositoryImpl(dataRepository);
  }

  @Bean
  public ControlImplementationRepository controlImplementationRepository(
      ControlImplementationDataRepository dataRepository) {
    return new ControlImplementationRepositoryImpl(dataRepository);
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
  public BlackbirdModule blackbirdModule() {
    return new BlackbirdModule();
  }

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
    return builder ->
        builder
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .filters(
                new SimpleFilterProvider()
                    .addFilter(
                        VeoConstants.JSON_FILTER_IDREF, SimpleBeanPropertyFilter.serializeAll()));
  }

  @Bean
  public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
    return new Jackson2JsonMessageConverter(objectMapper);
  }

  @Bean
  public RabbitTemplate rabbitTemplate(
      final ConnectionFactory connectionFactory, MessageConverter messageConverter) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(messageConverter);
    template.setChannelTransacted(false);
    template.setMandatory(true);
    return template;
  }

  @Bean
  public DomainTemplateServiceImpl domainTemplateService(
      DomainTemplateRepository domainTemplateRepository, DomainStateMapper domainStateMapper) {
    return new DomainTemplateServiceImpl(domainTemplateRepository, domainStateMapper);
  }

  @Bean
  public TypeDefinitionProvider getTypeDefinitionProvider(ReferenceAssembler referenceAssembler) {
    return new TypeDefinitionProvider(referenceAssembler);
  }

  @Bean
  public GetCatalogIncarnationDescriptionUseCase getIncarnationDescriptionUseCase(
      UnitRepository unitRepository,
      CatalogItemRepository catalogItemRepository,
      DomainRepository domainRepository,
      GenericElementRepository genericElementRepository) {
    return new GetCatalogIncarnationDescriptionUseCase(
        domainRepository, unitRepository, catalogItemRepository, genericElementRepository);
  }

  @Bean
  public GetProfileIncarnationDescriptionUseCase getProfileIncarnationDescriptionUseCase(
      UnitRepository unitRepository, ProfileRepository profileRepository) {
    return new GetProfileIncarnationDescriptionUseCase(unitRepository, profileRepository);
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
      UnitRepository unitRepository,
      ElementBatchCreator elementBatchCreator,
      EntityStateMapper entityStateMapper,
      RefResolverFactory refResolverFactory,
      EventPublisher eventPublisher) {
    return new UnitImportUseCase(
        unitRepository, refResolverFactory, entityStateMapper, elementBatchCreator, eventPublisher);
  }

  @Bean
  public EntityValidator entityValidator(AccountProvider accountProvider) {
    return new EntityValidator(accountProvider);
  }

  @Bean
  public IncarnationDescriptionApplier incarnationDescriptionApplier(
      UnitRepository unitRepository,
      ElementBatchCreator elementBatchCreator,
      EntityFactory factory,
      GenericElementRepository genericElementRepository) {
    return new IncarnationDescriptionApplier(
        factory, unitRepository, elementBatchCreator, genericElementRepository);
  }

  @Bean
  public ApplyCatalogIncarnationDescriptionUseCase applyCatalogIncarnationDescriptionUseCase(
      CatalogItemRepository catalogItemRepository,
      IncarnationDescriptionApplier incarnationDescriptionApplier) {
    return new ApplyCatalogIncarnationDescriptionUseCase(
        catalogItemRepository, incarnationDescriptionApplier);
  }

  @Bean
  public ApplyProfileIncarnationDescriptionUseCase applyProfileIncarnationDescriptionUseCase(
      ProfileItemRepository profileItemRepository,
      IncarnationDescriptionApplier incarnationDescriptionApplier) {
    return new ApplyProfileIncarnationDescriptionUseCase(
        profileItemRepository, incarnationDescriptionApplier);
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
  // TODO #3042: remove this when we remove support for JSON schema
  public ObjectSchemaParser objectSchemaParser(EntityFactory entityFactory) {
    return new ObjectSchemaParser(entityFactory);
  }

  @Bean
  public UpdateElementTypeDefinitionUseCase getUpdateElementTypeDefinitionUseCase(
      DomainStateMapper domainStateMapper, DomainRepository domainRepository) {
    return new UpdateElementTypeDefinitionUseCase(domainStateMapper, domainRepository);
  }

  @Bean
  public GetClientIdsWhereDomainTemplateNotAppliedUseCase
      getClientIdsWhereDomainTemplateNotAppliedUseCase(
          AccountProvider accountProvider, ClientRepository clientRepository) {
    return new GetClientIdsWhereDomainTemplateNotAppliedUseCase(accountProvider, clientRepository);
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
  public DeleteProfileUseCase deleteProfileUseCase(DomainRepository domainRepository) {
    return new DeleteProfileUseCase(domainRepository);
  }

  @Bean
  public MessageCreator messageCreator(
      StoredEventRepository storedEventRepository,
      ObjectMapper objectMapper,
      ReferenceAssembler referenceAssembler,
      EntityToDtoTransformer entityToDtoTransformer) {
    return new MessageCreatorImpl(
        storedEventRepository,
        objectMapper
            .copy()
            .setFilterProvider(
                new SimpleFilterProvider()
                    .addFilter(
                        VeoConstants.JSON_FILTER_IDREF,
                        VeoRestConstants.JSON_FILTER_EXCLUDE_SEARCHES_AND_RESOURCES)),
        referenceAssembler,
        entityToDtoTransformer);
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
      DomainRepository domainRepository, MigrateDomainUseCase migrateDomainUseCase) {
    return new UpdateAllClientDomainsUseCase(domainRepository, migrateDomainUseCase);
  }

  @Bean
  public GetInspectionUseCase getInspectionUseCase(DomainRepository domainRepository) {
    return new GetInspectionUseCase(domainRepository);
  }

  @Bean
  public GetInspectionsUseCase getInspectionsUseCase(DomainRepository domainRepository) {
    return new GetInspectionsUseCase(domainRepository);
  }

  @Bean
  public SaveInspectionUseCase saveInspectionUseCase(DomainRepository domainRepository) {
    return new SaveInspectionUseCase(domainRepository);
  }

  @Bean
  public DeleteInspectionUseCase deleteInspectionUseCase(DomainRepository domainRepository) {
    return new DeleteInspectionUseCase(domainRepository);
  }

  @Bean
  public MigrateDomainUseCase migrateDomainUseCase(
      DomainRepository domainRepository,
      RepositoryProvider repositoryProvider,
      UnitRepository unitRepository,
      ElementMigrationService elementMigrationService,
      Decider decider) {
    return new MigrateDomainUseCase(
        domainRepository, repositoryProvider, elementMigrationService, decider, unitRepository);
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
  public DomainStateMapper domainStateMapper(
      RefResolverFactory refResolvingFactory,
      EntityFactory entityFactory,
      DomainTemplateIdGenerator domainTemplateIdGenerator) {
    return new DomainStateMapper(refResolvingFactory, entityFactory, domainTemplateIdGenerator);
  }

  @Bean
  public CreateDomainTemplateUseCase createDomainTemplateUseCase(
      DomainTemplateRepository domainTemplateRepository, DomainStateMapper domainStateMapper) {
    return new CreateDomainTemplateUseCase(domainStateMapper, domainTemplateRepository);
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
  public ImpactInheritanceCalculator impactInheritanceCalculatorHighWatermark(
      ProcessRepository processRepository,
      AssetRepository assetRepository,
      ScopeRepository scopeRepository,
      FlyweightLinkRepository linkRepo) {
    return new ImpactInheritanceCalculatorHighWatermark(
        processRepository, assetRepository, scopeRepository, linkRepo);
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
  public ControlImplementationService controlImplementationService(
      ControlImplementationRepository ciRepo, RequirementImplementationRepository riRepo) {
    return new ControlImplementationService(ciRepo, riRepo);
  }

  @Bean
  public Decider decider(
      ClientRepository clientRepository, GenericElementRepository genericElementRepository) {
    return new Decider(clientRepository, genericElementRepository);
  }

  @Bean
  public SchemaReplacer schemaReplacer() {
    return new SchemaReplacer();
  }

  @Bean
  public EvaluateElementUseCase evaluateElementUseCase(
      RefResolverFactory refResolverFactory,
      DomainRepository domainRepository,
      GenericElementRepository genericElementRepository,
      Decider decider,
      Inspector inspector,
      IdentifiableFactory identifiableFactory,
      EntityStateMapper entityStateMapper) {
    return new EvaluateElementUseCase(
        refResolverFactory,
        identifiableFactory,
        entityStateMapper,
        domainRepository,
        genericElementRepository,
        decider,
        inspector);
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
      CatalogItemRepository catalogItemRepository,
      ProfileItemRepository profileItemRepository) {
    return new CatalogMigrationService(
        elementMigrationService, catalogItemRepository, profileItemRepository);
  }

  @Bean
  GetElementStatusCountUseCase getElementStatusCountUseCase(
      DomainRepository domainRepository,
      UnitRepository unitRepository,
      GenericElementRepository elementRepository) {
    return new GetElementStatusCountUseCase(domainRepository, unitRepository, elementRepository);
  }

  @Bean
  ElementBatchCreator elementBatchCreator(
      GenericElementRepository genericElementRepository,
      EventPublisher eventPublisher,
      Decider decider,
      DesignatorService designatorService) {
    return new ElementBatchCreator(
        genericElementRepository, eventPublisher, decider, designatorService);
  }

  @Bean
  UpdateAssetInDomainUseCase updateAssetDomainAssociationUseCase(
      RepositoryProvider repositoryProvider,
      Decider decider,
      EventPublisher eventPublisher,
      RefResolverFactory refResolverFactory) {
    return new UpdateAssetInDomainUseCase(
        repositoryProvider,
        decider,
        getEntityStateMapper(eventPublisher),
        eventPublisher,
        refResolverFactory);
  }

  @Bean
  UpdateControlInDomainUseCase updateControlInDomainUseCase(
      RepositoryProvider repositoryProvider,
      Decider decider,
      EventPublisher eventPublisher,
      RefResolverFactory refResolverFactory) {
    return new UpdateControlInDomainUseCase(
        repositoryProvider, decider, getEntityStateMapper(eventPublisher), refResolverFactory);
  }

  @Bean
  UpdateDocumentInDomainUseCase updateDocumentInDomainUseCase(
      RepositoryProvider repositoryProvider,
      Decider decider,
      EventPublisher eventPublisher,
      RefResolverFactory refResolverFactory) {
    return new UpdateDocumentInDomainUseCase(
        repositoryProvider, decider, getEntityStateMapper(eventPublisher), refResolverFactory);
  }

  @Bean
  UpdateIncidentInDomainUseCase updateIncidentInDomainUseCase(
      RepositoryProvider repositoryProvider,
      Decider decider,
      EventPublisher eventPublisher,
      RefResolverFactory refResolverFactory) {
    return new UpdateIncidentInDomainUseCase(
        repositoryProvider, decider, getEntityStateMapper(eventPublisher), refResolverFactory);
  }

  @Bean
  UpdatePersonInDomainUseCase updatePersonInDomainUseCase(
      RepositoryProvider repositoryProvider,
      Decider decider,
      EventPublisher eventPublisher,
      RefResolverFactory refResolverFactory) {
    return new UpdatePersonInDomainUseCase(
        repositoryProvider, decider, getEntityStateMapper(eventPublisher), refResolverFactory);
  }

  @Bean
  UpdateProcessInDomainUseCase updateProcessInDomainUseCase(
      RepositoryProvider repositoryProvider,
      Decider decider,
      EventPublisher eventPublisher,
      RefResolverFactory refResolverFactory) {
    return new UpdateProcessInDomainUseCase(
        repositoryProvider,
        decider,
        getEntityStateMapper(eventPublisher),
        eventPublisher,
        refResolverFactory);
  }

  @Bean
  UpdateScenarioInDomainUseCase updateScenarioInDomainUseCase(
      RepositoryProvider repositoryProvider,
      Decider decider,
      EventPublisher eventPublisher,
      RefResolverFactory refResolverFactory) {
    return new UpdateScenarioInDomainUseCase(
        repositoryProvider,
        decider,
        getEntityStateMapper(eventPublisher),
        eventPublisher,
        refResolverFactory);
  }

  @Bean
  UpdateScopeInDomainUseCase updateScopeInDomainUseCase(
      RepositoryProvider repositoryProvider,
      Decider decider,
      EventPublisher eventPublisher,
      RefResolverFactory refResolverFactory) {
    return new UpdateScopeInDomainUseCase(
        repositoryProvider,
        decider,
        getEntityStateMapper(eventPublisher),
        eventPublisher,
        refResolverFactory);
  }

  @Bean
  AssociateElementWithDomainUseCase associateElementWithDomainUseCase(
      GenericElementRepository genericElementRepository, DomainRepository domainRepository) {
    return new AssociateElementWithDomainUseCase(genericElementRepository, domainRepository);
  }

  @Bean
  AddLinksUseCase addLinksUseCase(
      DomainRepository domainRepository,
      GenericElementRepository genericElementRepository,
      EntityStateMapper entityStateMapper,
      RefResolverFactory refResolverFactory) {
    return new AddLinksUseCase(
        domainRepository, genericElementRepository, refResolverFactory, entityStateMapper);
  }

  @Bean
  GetAvailableActionsUseCase getAvailableActionsUseCase(DomainRepository domainRepository) {
    return new GetAvailableActionsUseCase(domainRepository);
  }

  @Bean
  PerformActionUseCase performActionUseCase(
      ClientRepository clientRepository,
      DomainRepository domainRepository,
      GenericElementRepository elementRepository,
      GetCatalogIncarnationDescriptionUseCase getCatalogIncarnationDescriptionUseCase,
      ApplyCatalogIncarnationDescriptionUseCase applyCatalogIncarnationDescriptionUseCase,
      DesignatorService designatorService,
      EntityFactory factory) {
    return new PerformActionUseCase(
        clientRepository,
        domainRepository,
        elementRepository,
        getCatalogIncarnationDescriptionUseCase,
        applyCatalogIncarnationDescriptionUseCase,
        designatorService,
        factory);
  }

  @Bean
  SaveDecisionUseCase saveDecisionUseCase(DomainRepository domainRepository) {
    return new SaveDecisionUseCase(domainRepository);
  }

  @Bean
  SaveRiskDefinitionUseCase saveRiskDefinitionUseCase(
      DomainRepository domainRepository, EventPublisher publisher) {
    return new SaveRiskDefinitionUseCase(domainRepository, publisher);
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

  @Bean
  public Validator validator() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      return factory.getValidator();
    }
  }
}
