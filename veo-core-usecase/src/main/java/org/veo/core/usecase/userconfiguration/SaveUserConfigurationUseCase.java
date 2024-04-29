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
package org.veo.core.usecase.userconfiguration;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import jakarta.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.UserConfiguration;
import org.veo.core.entity.specification.ContentTooLongException;
import org.veo.core.entity.specification.ExceedLimitException;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.UserConfigurationRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SaveUserConfigurationUseCase
    implements TransactionalUseCase<
        SaveUserConfigurationUseCase.InputData, SaveUserConfigurationUseCase.OutputData> {
  final UserConfigurationRepository userConfigurationRepository;
  final EntityFactory entityFactory;
  private final int maxUserConfigurations;
  private final int maxBytesPerConfiguration;

  @Override
  public OutputData execute(InputData input) {
    if (input.configuration.toString().getBytes(StandardCharsets.UTF_8).length
        > maxBytesPerConfiguration) {
      throw new ContentTooLongException(
          "Exceeds the configuration size limit. (%d bytes)".formatted(maxBytesPerConfiguration));
    }
    UserConfiguration userConfiguration =
        userConfigurationRepository
            .findUserConfiguration(input.client.getId(), input.userName, input.applicationId)
            .orElse(
                entityFactory.createUserConfiguration(
                    input.client, input.userName, input.applicationId));
    boolean created = !userConfiguration.isPersisted();
    if (created
        && userConfigurationRepository.countUserConfigurations(input.client.getId(), input.userName)
            >= maxUserConfigurations) {
      throw new ExceedLimitException(
          "Exceeds the configuration per user limit. (%d allowed)"
              .formatted(maxUserConfigurations));
    }
    userConfiguration.setConfiguration(input.configuration);
    userConfigurationRepository.save(userConfiguration);
    return new OutputData(created, userConfiguration.getApplicationId());
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(
      Client client, String userName, String applicationId, Map<String, Object> configuration)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(boolean created, String applicationId) implements UseCase.OutputData {}
}
