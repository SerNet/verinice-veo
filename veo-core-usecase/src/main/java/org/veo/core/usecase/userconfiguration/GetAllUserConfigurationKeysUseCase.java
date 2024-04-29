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

import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.entity.Key;
import org.veo.core.repository.UserConfigurationRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GetAllUserConfigurationKeysUseCase
    implements TransactionalUseCase<
        GetAllUserConfigurationKeysUseCase.InputData,
        GetAllUserConfigurationKeysUseCase.OutputData> {
  final UserConfigurationRepository userConfigurationRepository;

  @Override
  public OutputData execute(InputData input) {
    Set<String> userConfiguration =
        userConfigurationRepository.findAllKeysByUser(input.clientId, input.userName);
    return new OutputData(userConfiguration);
  }

  @Valid
  public record InputData(Key<UUID> clientId, String userName) implements UseCase.InputData {}

  @Valid
  public record OutputData(Set<String> keys) implements UseCase.OutputData {}
}
