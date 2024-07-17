/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.service;

import java.util.Set;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Profile;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.service.DomainTemplateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Looks up latest default domain templates and incarnates them in clients. The names of the default
 * domain templates are injected.
 */
@RequiredArgsConstructor
@Slf4j
public class DefaultDomainCreator {
  private final Set<String> defaultDomainTemplateNames;
  private final DomainTemplateService domainService;
  private final DomainTemplateRepository domainTemplateRepository;

  public void addDefaultDomains(Client client) {
    addDefaultDomains(client, true);
  }

  public void addDefaultDomains(Client client, boolean copyProfiles) {
    if (!client.getDomains().isEmpty()) {
      throw new IllegalArgumentException("The client already owns domains.");
    }
    defaultDomainTemplateNames.forEach(name -> addDomain(client, name, copyProfiles));
  }

  public void addDomain(Client client, String templateName, boolean copyProfiles) {
    domainTemplateRepository
        .getLatestDomainTemplateId(templateName)
        .ifPresentOrElse(
            templateId -> {
              log.debug(
                  "Adding default domain {} ({}) to client {}",
                  templateId,
                  templateName,
                  client.getIdAsString());
              client.addToDomains(
                  domainService.createDomain(client, templateId.uuidValue(), copyProfiles));
            },
            () -> {
              log.warn("Default domain template {} not found.", templateName);
            });
  }

  public void copyProfileToDomain(Profile profile, Domain domain) {
    domainService.copyProfileToDomain(profile, domain);
  }
}
