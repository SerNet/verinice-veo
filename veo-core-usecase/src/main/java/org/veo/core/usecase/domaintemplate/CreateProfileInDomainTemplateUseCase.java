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
package org.veo.core.usecase.domaintemplate;

import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileState;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.specification.MissingAdminPrivilegesException;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.repository.ProfileRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.service.DomainStateMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CreateProfileInDomainTemplateUseCase
    implements TransactionalUseCase<
        CreateProfileInDomainTemplateUseCase.InputData,
        CreateProfileInDomainTemplateUseCase.OutputData> {
  private final DomainTemplateRepository domainTemplateRepository;
  private final ProfileRepository profileRepository;
  private final DomainStateMapper domainStateMapper;

  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    var domainTemplate =
        domainTemplateRepository
            .findById(input.templateId)
            .orElseThrow(() -> new NotFoundException(input.templateId, DomainTemplate.class));

    if (!checkClient(userAccessRights.clientId(), domainTemplate)) {
      throw new MissingAdminPrivilegesException();
    }
    domainTemplate.getProfiles().stream()
        .filter(p -> p.matches(input.profile))
        .findFirst()
        .ifPresent(
            oldProfile -> {
              log.info("remove profile: {}({})", oldProfile.getName(), oldProfile.getId());
              domainTemplate.getProfiles().remove(oldProfile);
            });

    Profile profile = domainStateMapper.toProfile(input.profile, domainTemplate);
    log.info(
        "profile added {}({}) to {}({})",
        profile.getName(),
        profile.getIdAsString(),
        profile.getOwner().getName(),
        profile.getOwner().getIdAsString());
    return new OutputData(profileRepository.save(profile));
  }

  /** test if the client can modify the domaintemplate * */
  private boolean checkClient(UUID clientId, DomainTemplate domainTemplate) {
    log.warn(
        "Client {} modify domaintemplate {}({})",
        clientId,
        domainTemplate.getName(),
        domainTemplate.getIdAsString());
    return true;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(UUID templateId, ProfileState profile) implements UseCase.InputData {}

  @Valid
  public record OutputData(Profile profile) implements UseCase.OutputData {}
}
