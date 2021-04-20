/*******************************************************************************
 * Copyright (c) 2021 Urs Zeidler.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Key;

/**
 * As domain template exist in the system space and are not directly bound to
 * any client. This service manage the access rights.
 */
public interface DomainTemplateService {

    List<DomainTemplate> getTemplates(Client client);

    Optional<DomainTemplate> getTemplate(Client client, Key<UUID> templateId);

    Domain createNewDomainFromTemplate(Client client, Key<UUID> templateId);
}
