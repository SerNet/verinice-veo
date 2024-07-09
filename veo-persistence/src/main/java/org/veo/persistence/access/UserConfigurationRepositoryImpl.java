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
package org.veo.persistence.access;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.Key;
import org.veo.core.entity.UserConfiguration;
import org.veo.core.repository.UserConfigurationRepository;
import org.veo.persistence.access.jpa.UserConfigurationDataRepository;
import org.veo.persistence.entity.jpa.UserConfigurationData;

import lombok.AllArgsConstructor;

@Repository
@AllArgsConstructor
public class UserConfigurationRepositoryImpl implements UserConfigurationRepository {
  private final UserConfigurationDataRepository dataRepository;

  @Override
  public UserConfiguration save(UserConfiguration entity) {
    return dataRepository.save((UserConfigurationData) entity);
  }

  @Override
  public void delete(UserConfiguration entity) {
    dataRepository.delete((UserConfigurationData) entity);
  }

  @Override
  public Set<String> findAllKeysByUser(Key<UUID> clientId, String userName) {
    return dataRepository.findAllKeysByUser(clientId.value(), userName);
  }

  @Override
  public Optional<UserConfiguration> findUserConfiguration(
      Key<UUID> clientId, String username, String applicationId) {
    return dataRepository
        .findUserConfiguration(clientId.value(), username, applicationId)
        .map(UserConfiguration.class::cast);
  }

  @Override
  public Set<UserConfiguration> findAllByClient(Key<UUID> clientId) {
    return dataRepository.findUserConfigurationsByClient(clientId.value()).stream()
        .map(UserConfiguration.class::cast)
        .collect(Collectors.toSet());
  }

  @Override
  public int countUserConfigurations(Key<UUID> clientId, String username) {
    return dataRepository.countUserConfigurations(clientId.value(), username);
  }
}
