/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler
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
package org.veo.core.usecase.profile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.Profile;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.repository.ProfileRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetProfileUseCase extends AbstractProfileUseCase
    implements TransactionalUseCase<GetProfileUseCase.InputData, GetProfileUseCase.OutputData> {

  public GetProfileUseCase(ProfileRepository profileRepo) {
    super(profileRepo);
  }

  @Override
  public OutputData execute(InputData input) {
    checkClientOwnsDomain(input.getAuthenticatedClient(), input.domain.getId());
    var profile =
        profileRepo
            .findById(input.authenticatedClient.getId(), input.profile.toKey())
            .orElseThrow(
                () -> new NotFoundException(Key.uuidFrom(input.profile.getId()), Profile.class));
    log.info("profile: {}", profile);

    return new OutputData(profile);
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    @NotNull Client authenticatedClient;
    @NotNull ITypedId<Domain> domain;
    @NotNull ITypedId<Profile> profile;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid Profile profile;
  }
}
