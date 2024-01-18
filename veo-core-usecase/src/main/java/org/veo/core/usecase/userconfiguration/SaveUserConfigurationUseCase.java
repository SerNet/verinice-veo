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

import java.util.Map;

import jakarta.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.UserConfiguration;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.UserConfigurationRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.AllArgsConstructor;
import lombok.Value;

@AllArgsConstructor
public class SaveUserConfigurationUseCase
    implements TransactionalUseCase<
        SaveUserConfigurationUseCase.InputData, SaveUserConfigurationUseCase.OutputData> {
  final UserConfigurationRepository userConfigurationRepository;
  final EntityFactory entityFactory;

  @Override
  public OutputData execute(InputData input) {
    UserConfiguration userConfiguration =
        userConfigurationRepository
            .findUserConfiguration(input.client.getId(), input.userName, input.applicationId)
            .orElse(
                entityFactory.createUserConfiguration(
                    input.client, input.userName, input.applicationId));
    boolean created = !userConfiguration.isPersisted();

    userConfiguration.setConfiguration(input.getConfiguration());
    userConfigurationRepository.save(userConfiguration);
    return new OutputData(created, userConfiguration.getApplicationId());
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    Client client;
    String userName;
    String applicationId;
    Map<String, Object> configuration;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    boolean created;
    String applicationId;
  }
}
