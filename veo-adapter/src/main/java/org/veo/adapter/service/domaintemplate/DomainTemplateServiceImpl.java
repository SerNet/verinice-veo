/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.adapter.service.domaintemplate;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.veo.adapter.IdRefResolvingFactory;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.dto.AbstractRiskDto;
import org.veo.adapter.presenter.api.response.transformer.DomainAssociationTransformer;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.adapter.service.InternalDataCorruptionException;
import org.veo.adapter.service.domaintemplate.dto.TransformDomainTemplateDto;
import org.veo.core.ExportDto;
import org.veo.core.entity.Catalog;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.ModelConsistencyException;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.profile.ProfileDefinition;
import org.veo.core.entity.profile.ProfileRef;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.entity.transform.IdentifiableFactory;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.service.DomainTemplateIdGenerator;
import org.veo.core.service.DomainTemplateService;
import org.veo.core.usecase.service.EntityStateMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DomainTemplateServiceImpl implements DomainTemplateService {

  private final DomainTemplateRepository domainTemplateRepository;
  private final EntityToDtoTransformer dtoTransformer;
  private final EntityFactory factory;
  private final CatalogItemPrepareStrategy preparations;
  private final DomainTemplateIdGenerator domainTemplateIdGenerator;

  private final ReferenceAssembler assembler;
  private final ObjectMapper objectMapper;
  private final IdentifiableFactory identifiableFactory;
  private final EntityFactory entityFactory;
  private final EntityStateMapper entityStateMapper;

  public DomainTemplateServiceImpl(
      DomainTemplateRepository domainTemplateRepository,
      EntityFactory factory,
      DomainAssociationTransformer domainAssociationTransformer,
      IdentifiableFactory identifiableFactory,
      CatalogItemPrepareStrategy preparations,
      DomainTemplateIdGenerator domainTemplateIdGenerator,
      ReferenceAssembler referenceAssembler,
      ObjectMapper objectMapper,
      EntityStateMapper entityStateMapper) {
    this.domainTemplateRepository = domainTemplateRepository;
    this.factory = factory;
    this.preparations = preparations;
    this.domainTemplateIdGenerator = domainTemplateIdGenerator;
    this.identifiableFactory = identifiableFactory;
    this.entityFactory = factory;
    this.entityStateMapper = entityStateMapper;
    assembler = referenceAssembler;
    dtoTransformer = new EntityToDtoTransformer(assembler, domainAssociationTransformer);
    this.objectMapper = objectMapper;
  }

  @Override
  public List<DomainTemplate> getTemplates(Client client) {
    return Collections.emptyList();
  }

  @Override
  public Optional<DomainTemplate> getTemplate(Client client, Key<UUID> key) {
    checkClientAccess(client, key);
    return domainTemplateRepository.findByIdWithProfilesAndRiskDefinitions(key);
  }

  private void checkClientAccess(Client client, Key<UUID> key) {
    // TODO VEO-1454 check the shop status for available products
  }

  @Override
  public Domain createDomain(Client client, String templateId) {
    DomainTemplate domainTemplate =
        domainTemplateRepository
            .findByIdWithProfilesAndRiskDefinitions(Key.uuidFrom(templateId))
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Domain template %s does not exist for client %s.", templateId, client));

    TransformDomainTemplateDto domainTemplateDto =
        dtoTransformer.transformDomainTemplate2Dto(domainTemplate);

    var resolvingFactory = new IdRefResolvingFactory(identifiableFactory);
    resolvingFactory.setGlobalDomain(identifiableFactory.create(Domain.class, Key.newUuid()));
    var domain =
        new DtoToEntityTransformer(entityFactory, resolvingFactory, entityStateMapper)
            .transformTransformDomainTemplateDto2Domain(domainTemplateDto, resolvingFactory);
    domain.getCatalogs().stream()
        .flatMap((Catalog catalog) -> catalog.getCatalogItems().stream())
        .forEach(preparations::prepareCatalogItem);
    domain.setDomainTemplate(domainTemplate);
    client.addToDomains(domain);
    log.info("Domain {} created for client {}", domain.getName(), client);
    return domain;
  }

  @Override
  public Collection<Element> getProfileElements(Domain domain, ProfileRef profileKey) {
    var client = domain.getOwner();
    return domain
        .findProfile(profileKey)
        .map(profile -> createElementsFromProfile(client, domain, profile))
        .orElseThrow(() -> new NotFoundException("Profile '%s' not found", profileKey.getKeyRef()));
  }

  private Collection<Element> createElements(
      Client client,
      Domain domain,
      Set<AbstractElementDto> profileElements,
      Set<AbstractRiskDto> profileRisks) {
    var resolvingFactory = new IdRefResolvingFactory(identifiableFactory);
    resolvingFactory.setGlobalDomain(domain);
    var transformer = new DtoToEntityTransformer(factory, resolvingFactory, entityStateMapper);
    var elements =
        profileElements.stream()
            .map(e -> transformer.transformDto2Element(e, resolvingFactory))
            .toList();
    Unit dummyOwner = factory.createUnit(UUID.randomUUID().toString(), null);
    dummyOwner.setClient(client);
    elements.forEach(
        e -> {
          log.debug("Process element {}", e);
          e.setOwner(dummyOwner);
          DomainTemplateService.updateVersion(e);
        });
    profileRisks.forEach(
        r -> {
          log.debug("Transforming risk {}", r);
          var risk = transformer.transformDto2Risk(r, resolvingFactory);
          log.debug("Transformed risk: {}", risk);
        });
    dummyOwner.setClient(null);
    return elements;
  }

  private Collection<Element> createElementsFromProfile(
      Client client, Domain domain, ProfileDefinition profileDefinition) {
    try {
      return createElements(
          client,
          domain,
          parseJsonObjects(profileDefinition.getElements(), new TypeReference<>() {}),
          parseJsonObjects(profileDefinition.getRisks(), new TypeReference<>() {}));
    } catch (IOException e) {
      log.error("Error reading profile from domain template", e);
      throw new InternalDataCorruptionException("Error reading profile from domain template.", e);
    }
  }

  @Override
  public DomainTemplate createDomainTemplateFromDomain(Domain domain) {
    TransformDomainTemplateDto domainDto = dtoTransformer.transformDomainTemplate2Dto(domain);

    String domainTemplateId = createDomainTemplateId(domain);
    Key<UUID> domainTemplateKey = Key.uuidFrom(domainTemplateId);
    if (domainTemplateRepository.exists(domainTemplateKey)) {
      throw new ModelConsistencyException("The UUID %s is already used.", domainTemplateId);
    }
    var resolvingFactory = new IdRefResolvingFactory(identifiableFactory);
    resolvingFactory.setGlobalDomainTemplateId(domainTemplateId);
    var transformer =
        new DtoToEntityTransformer(entityFactory, resolvingFactory, entityStateMapper);
    var newDomainTemplate =
        transformer.transformTransformDomainTemplateDto2DomainTemplate(domainDto, resolvingFactory);
    newDomainTemplate.setId(domainTemplateKey);
    newDomainTemplate.getCatalogs().stream()
        .flatMap((Catalog catalog) -> catalog.getCatalogItems().stream())
        .forEach(preparations::prepareCatalogItem);
    log.info("Create and save domain template {} from domain {}", newDomainTemplate, domain);
    return domainTemplateRepository.save(newDomainTemplate);
  }

  private <T> Set<T> parseJsonObjects(Set<?> objects, TypeReference<Set<T>> typeRef)
      throws JsonProcessingException {
    return objectMapper.readValue(objectMapper.writeValueAsString(objects), typeRef);
  }

  private String createDomainTemplateId(Domain domain) {
    return domainTemplateIdGenerator.createDomainTemplateId(
        domain.getName(), domain.getTemplateVersion());
  }

  @Override
  public ExportDto exportDomain(Domain domain) {
    return dtoTransformer.transformDomain2ExportDto(domain);
  }
}
