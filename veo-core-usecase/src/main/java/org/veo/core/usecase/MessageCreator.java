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
package org.veo.core.usecase;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.event.VersioningEvent;

/**
 * Creates outgoing messages and persists them so they can be sent to the
 * message queue by a background task.
 */
public interface MessageCreator {
    void createEntityRevisionMessage(VersioningEvent event, Client client);

    void createDomainCreationMessage(Domain domain);
}
