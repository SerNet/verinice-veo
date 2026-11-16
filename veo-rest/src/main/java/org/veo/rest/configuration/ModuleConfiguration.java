/*
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
 */
package org.veo.rest.configuration;

import java.util.Arrays;
import java.util.Optional;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.http.HttpStatus;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.core.entity.AccountProvider;
import org.veo.core.entity.specification.EntityValidator;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.entity.transform.IdentifiableFactory;
import org.veo.core.repository.AssetRepository;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.ControlImplementationRepository;
import org.veo.core.repository.DesignatorSequenceRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.repository.FlyweightLinkRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.InspectionFindingRepository;
import org.veo.core.repository.PersonRepository;
import org.veo.core.repository.ProcessRepository;
import org.veo.core.repository.ProfileItemRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.repository.RequirementImplementationRepository;
import org.veo.core.repository.ScopeRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.repository.UserConfigurationRepository;
import org.veo.core.service.DomainTemplateIdGenerator;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.DesignatorService;
import org.veo.core.usecase.DomainChangeService;
import org.veo.core.usecase.IncomingMessageHandler;
import org.veo.core.usecase.MessageCreator;
import org.veo.core.usecase.catalogitem.IncarnationDescriptionApplier;
import org.veo.core.usecase.decision.Decider;
import org.veo.core.usecase.domain.ElementBatchCreator;
import org.veo.core.usecase.domain.GetAttributeValuesUseCase;
import org.veo.core.usecase.inspection.Inspector;
import org.veo.core.usecase.service.DomainStateMapper;
import org.veo.core.usecase.service.DomainTemplateService;
import org.veo.core.usecase.service.EntityStateMapper;
import org.veo.core.usecase.service.MigrationService;
import org.veo.core.usecase.service.RefResolverFactory;
import org.veo.core.usecase.unit.UnitValidator;
import org.veo.core.usecase.userconfiguration.SaveUserConfigurationUseCase;
import org.veo.service.ControlImplementationService;
import org.veo.service.DefaultDomainCreator;
import org.veo.service.ElementMigrationService;
import org.veo.service.EtagService;
import org.veo.service.TemplateItemMigrationService;
import org.veo.service.risk.ImpactInheritanceCalculator;
import org.veo.service.risk.ImpactInheritanceCalculatorHighWatermark;
import org.veo.service.risk.RiskService;

import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.blackbird.BlackbirdModule;

/**
 * This configuration takes care of wiring classes from core modules (Entity-Layer, Use-Case-Layer)
 * that have no dependency to the Spring framework. They are therefore not picked up and autowired
 * by Spring on their own.
 *
 * <p>Use cases are found by the component scan below, which selects them by type instead of by
 * annotation, and are instantiated through their constructor. All other core classes, and use cases
 * that need more than plain constructor injection, are defined as {@code @Bean} methods.
 */
@Configuration
public class ModuleConfiguration {

  @Bean
  public SaveUserConfigurationUseCase saveUserConfigurationUseCase(
      UserConfigurationRepository userConfigurationRepository,
      EntityFactory entityFactor,
      @Value("${veo.default.user-configurations-max:10}") int maxConfigurations,
      @Value("${veo.default.user-configuration-bytes-max:4000}") int maxBytesPerConfiguration,
      ClientRepository clientRepository) {
    return new SaveUserConfigurationUseCase(
        userConfigurationRepository,
        entityFactor,
        maxConfigurations,
        maxBytesPerConfiguration,
        clientRepository);
  }

  @Bean
  public UnitValidator unitValidator(GenericElementRepository genericElementRepository) {
    return new UnitValidator(genericElementRepository);
  }

  @Bean
  public EntityStateMapper getEntityStateMapper(
      EntityFactory entityFactory, EventPublisher eventPublisher) {
    return new EntityStateMapper(entityFactory, eventPublisher);
  }

  @Bean
  public RefResolverFactory refResolverFactory(
      RepositoryProvider repositoryProvider, IdentifiableFactory identifiableFactory) {
    return new RefResolverFactory(repositoryProvider, identifiableFactory);
  }

  @Primary
  @Bean(name = "applicationEventMulticaster")
  public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
    SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
    eventMulticaster.setTaskExecutor(new SyncTaskExecutor());
    return eventMulticaster;
  }

  @Bean
  public DesignatorService designatorService(
      DesignatorSequenceRepository designatorSequenceRepository) {
    return new DesignatorService(designatorSequenceRepository);
  }

  @Bean
  public DomainChangeService domainChangeService(
      DomainTemplateRepository domainTemplateRepository,
      TemplateItemMigrationService templateItemMigrationService) {
    return new DomainChangeService(domainTemplateRepository, templateItemMigrationService);
  }

  @Bean
  public BlackbirdModule blackbirdModule() {
    return new BlackbirdModule();
  }

  @Bean
  public JsonMapperBuilderCustomizer jsonCustomizer() {
    return builder ->
        builder
            .addModule(blackbirdModule())
            .disable(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS)
            // work around issue https://github.com/FasterXML/jackson-databind/issues/5432
            .disable(EnumFeature.WRITE_ENUMS_USING_TO_STRING);
  }

  @Bean
  public MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
    return new JacksonJsonMessageConverter(jsonMapper);
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
  public DomainTemplateService domainTemplateService(
      DomainTemplateRepository domainTemplateRepository, DomainStateMapper domainStateMapper) {
    return new DomainTemplateService(domainTemplateRepository, domainStateMapper);
  }

  @Bean
  public EntityValidator entityValidator(AccountProvider accountProvider) {
    return new EntityValidator(accountProvider);
  }

  @Bean
  public IncarnationDescriptionApplier incarnationDescriptionApplier(
      ElementBatchCreator elementBatchCreator,
      EntityFactory factory,
      GenericElementRepository genericElementRepository) {
    return new IncarnationDescriptionApplier(
        factory, elementBatchCreator, genericElementRepository);
  }

  @Bean
  public EtagService etagService(RepositoryProvider repositoryProvider) {
    return new EtagService(repositoryProvider);
  }

  @Bean
  public IncomingMessageHandler incomingMessageHandler(
      PersonRepository personRepository,
      RepositoryProvider repositoryProvider,
      ElementMigrationService elementMigrationService,
      TemplateItemMigrationService templateItemMigrationService) {
    return new IncomingMessageHandler(
        personRepository,
        repositoryProvider,
        elementMigrationService,
        templateItemMigrationService);
  }

  @Bean
  public ElementMigrationService elementMigrationService() {
    return new ElementMigrationService();
  }

  @Bean
  public DefaultDomainCreator defaultDomainTemplateProvider(
      DomainTemplateService domainService,
      DomainTemplateRepository domainTemplateRepository,
      MessageCreator messageCreator) {
    return new DefaultDomainCreator(domainService, domainTemplateRepository, messageCreator);
  }

  @Bean
  public DomainStateMapper domainStateMapper(
      RefResolverFactory refResolvingFactory,
      EntityFactory entityFactory,
      DomainTemplateIdGenerator domainTemplateIdGenerator) {
    return new DomainStateMapper(refResolvingFactory, entityFactory, domainTemplateIdGenerator);
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
  public MigrationService migrationservice(
      GenericElementRepository genericElementRepository,
      Decider decider,
      MessageCreator messageCreator) {
    return new MigrationService(genericElementRepository, decider, messageCreator);
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
  public Decider decider() {
    return new Decider();
  }

  @Bean
  public Inspector inspector(
      DomainTemplateRepository domainTemplateRepository,
      DomainTemplateService domainTemplateService,
      DomainChangeService domainChangeService) {
    return new Inspector(domainTemplateRepository, domainTemplateService, domainChangeService);
  }

  @Bean
  TemplateItemMigrationService templateItemMigrationService(
      ElementMigrationService elementMigrationService,
      CatalogItemRepository catalogItemRepository,
      ProfileItemRepository profileItemRepository) {
    return new TemplateItemMigrationService(
        elementMigrationService, catalogItemRepository, profileItemRepository);
  }

  @Bean
  GetAttributeValuesUseCase getAttributeValuesUseCase(
      DomainRepository domainRepository,
      UnitRepository unitRepository,
      GenericElementRepository genericElementRepository,
      @Value("${veo.attribute-values.max-results:100}") int maxResults) {
    return new GetAttributeValuesUseCase(
        domainRepository, unitRepository, genericElementRepository, maxResults);
  }

  @Bean
  ElementBatchCreator elementBatchCreator(
      GenericElementRepository genericElementRepository,
      EventPublisher eventPublisher,
      Decider decider,
      DesignatorService designatorService,
      InspectionFindingRepository inspectionFindingRepository) {
    return new ElementBatchCreator(
        genericElementRepository,
        eventPublisher,
        decider,
        designatorService,
        inspectionFindingRepository);
  }

  @Bean
  public Validator validator() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      return factory.getValidator();
    }
  }

  // by default, the content schema for every response is derived from the
  // method return type. But for client errors (e.g. 400, 404 etc.), we usually return
  // a ApiResponseBody content. To avoid having to specify this for every
  // @ApiResponse with a 4xx response code, we override it in the generated
  // schema for all the operations unless there is a specific content schema specified.
  @Bean
  public OperationCustomizer errorResponseSchemas(
      final SpringDocConfigProperties springDocConfigProperties) {
    Schema schema =
        AnnotationsUtils.resolveSchemaFromType(
            ApiResponseBody.class, null, null, springDocConfigProperties.isOpenapi31());
    @SuppressWarnings("PMD.LooseCoupling")
    Content content =
        new Content()
            .addMediaType(
                org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                new MediaType().schema(schema));
    return (operation, handlerMethod) -> {
      operation
          .getResponses()
          .forEach((status, response) -> {
            if (!HttpStatus.resolve(Integer.parseInt(status)).is4xxClientError()) {
              return;
            }
            var annotations =
                AnnotatedElementUtils.findMergedRepeatableAnnotations(
                    handlerMethod.getMethod(), ApiResponse.class);
            Optional<ApiResponse> annotationForStatusCode =
                annotations.stream().filter(a -> status.equals(a.responseCode())).findAny();
            if (!annotationForStatusCode.isPresent()) {
              response.setContent(content);
              return;
            }
            // check schema and override if not set
            Optional<io.swagger.v3.oas.annotations.media.Content> contentForStatusCode =
                Arrays.stream(annotationForStatusCode.get().content())
                    .filter(it ->
                        it.mediaType()
                            .equals(org.springframework.http.MediaType.APPLICATION_JSON_VALUE))
                    .findAny();
            if (!contentForStatusCode.isPresent()) {
              response.setContent(content);
              return;
            }
            MediaType mediaType =
                response
                    .getContent()
                    .get(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
            var contentSchema = contentForStatusCode.get().schema();
            if (mediaType != null && contentSchema == null
                || contentSchema.implementation() == Void.class) {
              mediaType.setSchema(schema);
            }
          });
      return operation;
    };
  }
}
