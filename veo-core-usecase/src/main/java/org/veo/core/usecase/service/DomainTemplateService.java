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
package org.veo.core.usecase.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Profile;
import org.veo.core.entity.exception.ModelConsistencyException;
import org.veo.core.repository.DomainTemplateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A {@link DomainTemplate} exist in the system space and is not directly bound to any client. This
 * service manages the access rights. It is responsible for creating a client domain from a domain
 * template.
 */
@RequiredArgsConstructor
@Slf4j
public class DomainTemplateService {
  private final DomainTemplateRepository domainTemplateRepository;
  private final DomainStateMapper domainStateMapper;

  public List<DomainTemplate> getTemplates(Client client) {
    return Collections.emptyList();
  }

  public DomainTemplate getTemplate(Client client, UUID key) {
    checkClientAccess(client, key);
    return domainTemplateRepository.getByIdWithRiskDefinitionsProfilesAndCatalogItems(key);
  }

  private void checkClientAccess(
      @SuppressWarnings("PMD.UnusedFormalParameter") Client client,
      @SuppressWarnings("PMD.UnusedFormalParameter") UUID key) {
    // TODO VEO-1454 check the shop status for available products
  }

  public Domain createDomain(Client client, UUID templateId) {
    return createDomain(client, templateId, true);
  }

  public Domain createDomain(Client client, UUID templateId, boolean copyProfiles) {
    DomainTemplate domainTemplate = getTemplate(client, templateId);

    var domain = domainStateMapper.toDomain(domainTemplate, copyProfiles);
    domain.setDomainTemplate(domainTemplate);
    client.addToDomains(domain);
    log.info("Domain {} created for client {}", domain.getName(), client);
    return domain;
  }

  public void copyProfileToDomain(Profile profile, Domain domain) {
    log.info(
        "create profile {} in domain {}:{}",
        profile.getName(),
        domain.getName(),
        domain.getTemplateVersion());

    if (domain.getProfiles().stream().anyMatch(profile::matches)) {
      log.info(
          "Profile {} already present in domain {}, skip", profile.getName(), domain.getName());
      return;
    }

    Profile profileCopy = domainStateMapper.toProfile(profile, domain);
    domain.getProfiles().add(profileCopy);
  }

  public DomainTemplate createDomainTemplateFromDomain(Domain domain) {
    var newDomainTemplate = domainStateMapper.toTemplate(domain);
    if (domainTemplateRepository.exists(newDomainTemplate.getId())) {
      throw new ModelConsistencyException(
          "The UUID %s is already used.", newDomainTemplate.getIdAsString());
    }
    log.info("Create and save domain template {} from domain {}", newDomainTemplate, domain);
    return domainTemplateRepository.save(newDomainTemplate);
  }
}
