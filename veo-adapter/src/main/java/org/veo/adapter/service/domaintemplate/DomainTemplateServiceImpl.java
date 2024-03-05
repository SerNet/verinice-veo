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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.veo.adapter.IdRefResolvingFactory;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.response.transformer.DomainAssociationTransformer;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.adapter.service.domaintemplate.dto.ExportDomainTemplateDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.ModelConsistencyException;
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
  private final DomainTemplateIdGenerator domainTemplateIdGenerator;
  private final IdentifiableFactory identifiableFactory;
  private final EntityFactory entityFactory;
  private final EntityStateMapper entityStateMapper;

  public DomainTemplateServiceImpl(
      DomainTemplateRepository domainTemplateRepository,
      EntityFactory factory,
      DomainAssociationTransformer domainAssociationTransformer,
      IdentifiableFactory identifiableFactory,
      DomainTemplateIdGenerator domainTemplateIdGenerator,
      ReferenceAssembler referenceAssembler,
      EntityStateMapper entityStateMapper) {
    this.domainTemplateRepository = domainTemplateRepository;
    this.domainTemplateIdGenerator = domainTemplateIdGenerator;
    this.identifiableFactory = identifiableFactory;
    this.entityFactory = factory;
    this.entityStateMapper = entityStateMapper;
    dtoTransformer = new EntityToDtoTransformer(referenceAssembler, domainAssociationTransformer);
  }

  @Override
  public List<DomainTemplate> getTemplates(Client client) {
    return Collections.emptyList();
  }

  @Override
  public DomainTemplate getTemplate(Client client, Key<UUID> key) {
    checkClientAccess(client, key);
    return domainTemplateRepository.getByIdWithRiskDefinitionsProfilesAndCatalogItems(key);
  }

  private void checkClientAccess(Client client, Key<UUID> key) {
    // TODO VEO-1454 check the shop status for available products
  }

  @Override
  public Domain createDomain(Client client, String templateId) {
    DomainTemplate domainTemplate = getTemplate(client, Key.uuidFrom(templateId));

    ExportDomainTemplateDto domainTemplateDto =
        dtoTransformer.transformDomainTemplate2Dto(domainTemplate);

    var resolvingFactory = new IdRefResolvingFactory(identifiableFactory);
    resolvingFactory.setGlobalDomain(identifiableFactory.create(Domain.class, Key.newUuid()));
    var domain =
        new DtoToEntityTransformer(entityFactory, resolvingFactory, entityStateMapper)
            .transformTransformDomainTemplateDto2Domain(domainTemplateDto, resolvingFactory);
    domain.setDomainTemplate(domainTemplate);
    client.addToDomains(domain);
    log.info("Domain {} created for client {}", domain.getName(), client);
    return domain;
  }

  @Override
  public DomainTemplate createDomainTemplateFromDomain(Domain domain) {
    ExportDomainTemplateDto domainDto = dtoTransformer.transformDomainTemplate2Dto(domain);

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
    log.info("Create and save domain template {} from domain {}", newDomainTemplate, domain);
    return domainTemplateRepository.save(newDomainTemplate);
  }

  private String createDomainTemplateId(Domain domain) {
    return domainTemplateIdGenerator.createDomainTemplateId(
        domain.getName(), domain.getTemplateVersion());
  }
}
