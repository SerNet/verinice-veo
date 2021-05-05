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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Key;

import lombok.Data;

@Data
public class DomainTemplateServiceImpl implements DomainTemplateService {

    // TODO: VEO-502 Implement DomainTemplateServiceImpl
    @Override
    public List<DomainTemplate> getTemplates(Client client) {
        return Collections.emptyList();
    }

    @Override
    public Optional<DomainTemplate> getTemplate(Client client, Key<UUID> key) {
        return Optional.empty();
    }

    @Override
    public Domain createNewDomainFromTemplate(Client client, Key<UUID> templateId) {
        return null;
    }

}
