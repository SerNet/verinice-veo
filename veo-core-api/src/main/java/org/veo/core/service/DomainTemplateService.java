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
package org.veo.core.service;

import java.util.List;
import java.util.UUID;

import org.veo.core.ExportDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;

/**
 * A {@link DomainTemplate} exist in the system space and is not directly bound to any client. This
 * service manages the access rights. It is responsible for creating a client domain from a domain
 * template.
 */
public interface DomainTemplateService {
  List<DomainTemplate> getTemplates(Client client);

  DomainTemplate getTemplate(Client client, Key<UUID> templateId);

  /**
   * Creates a domain from the given templateId for the given client. The client is used check the
   * rights. No modification on the client is done.
   *
   * @throws NotFoundException when the template doesn't exist
   */
  Domain createDomain(Client client, String templateId);

  ExportDto exportDomain(Domain domain);

  DomainTemplate createDomainTemplateFromDomain(Domain domain);
}
