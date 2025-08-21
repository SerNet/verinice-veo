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

import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.UserConfiguration;
import org.veo.core.repository.UserConfigurationRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DeleteUserConfigurationUseCase
    implements TransactionalUseCase<
        DeleteUserConfigurationUseCase.InputData, DeleteUserConfigurationUseCase.OutputData> {
  final UserConfigurationRepository userConfigurationRepository;

  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    UserConfiguration userConfiguration =
        userConfigurationRepository.getUserConfiguration(
            input.clientId, input.userName, input.applicationId);
    userConfigurationRepository.delete(userConfiguration);
    return new OutputData(userConfiguration.getApplicationId());
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(UUID clientId, String userName, String applicationId)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(String applicationId) implements UseCase.OutputData {}
}
