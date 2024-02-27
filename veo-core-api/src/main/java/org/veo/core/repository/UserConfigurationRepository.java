/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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
package org.veo.core.repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.Key;
import org.veo.core.entity.UserConfiguration;
import org.veo.core.entity.exception.NotFoundException;

public interface UserConfigurationRepository {

  UserConfiguration save(UserConfiguration entity);

  void delete(UserConfiguration entity);

  Set<UserConfiguration> findAllByClient(Key<UUID> clientId);

  Optional<UserConfiguration> findUserConfiguration(
      Key<UUID> clientId, String userName, String applicationId);

  default UserConfiguration getUserConfiguration(
      Key<UUID> clientId, String userName, String applicationId) {
    return findUserConfiguration(clientId, userName, applicationId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format(
                        "no configuration found for user '%s' and application id: '%s'",
                        userName, applicationId),
                    UserConfiguration.class));
  }

  int countUserConfigurations(Key<UUID> id, String userName);
}
