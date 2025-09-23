/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler.
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
package org.veo.core.usecase.catalogitem;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.entity.specification.EntitySpecifications;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.ProfileRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.parameter.TemplateItemIncarnationDescription;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetProfileIncarnationDescriptionUseCase
    extends AbstractGetIncarnationDescriptionUseCase<ProfileItem, Profile>
    implements TransactionalUseCase<
        GetProfileIncarnationDescriptionUseCase.InputData,
        GetProfileIncarnationDescriptionUseCase.OutputData> {

  private final UnitRepository unitRepository;
  private final ProfileRepository profileRepository;
  private final ClientRepository clientRepository;

  public GetProfileIncarnationDescriptionUseCase(
      GenericElementRepository genericElementRepository,
      UnitRepository unitRepository,
      ProfileRepository profileRepository,
      ClientRepository clientRepository) {
    super(ProfileItem.class, genericElementRepository);
    this.unitRepository = unitRepository;
    this.profileRepository = profileRepository;
    this.clientRepository = clientRepository;
  }

  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    var client = clientRepository.getById(userAccessRights.getClientId());
    log.info(
        "profile: {}, unit: {}, items: {}", input.profileId, input.unitId, input.profileItemIds);
    Unit unit = unitRepository.getByIdFetchClient(input.unitId, userAccessRights);
    validateInput(input, client);
    Profile profile =
        profileRepository
            .findById(userAccessRights.getClientId(), input.profileId)
            .orElseThrow(() -> new NotFoundException(input.profileId, Profile.class));

    Domain domain = profile.requireDomainMembership();
    List<UUID> profileItemIds;
    Collection<ProfileItem> profileItems;
    if (input.profileItemIds != null) {
      profileItems =
          profileRepository.findItemsByIdsFetchDomainAndTailoringReferences(
              Set.copyOf(input.profileItemIds), client);
      profileItemIds = input.profileItemIds;
    } else {
      profileItems =
          profileRepository.findItemsByProfileIdFetchDomainAndTailoringReferences(
              input.profileId, client);
      profileItemIds = profileItems.stream().map(TemplateItem::getSymbolicId).toList();
    }

    return new OutputData(
        getIncarnationDescriptions(
            profileItemIds,
            profileItems,
            profile,
            domain,
            unit,
            null,
            null,
            null,
            null,
            input.mergeBidirectionalReferences),
        unit);
  }

  private void validateInput(InputData input, Client client) {
    client.getDomains().stream()
        .filter(d -> d.getId().equals(input.domainId))
        .findAny()
        .orElseThrow(() -> new NotFoundException(input.domainId, Domain.class));

    if (input.profileId != null) {
      Profile profile =
          profileRepository
              .findById(input.profileId)
              .orElseThrow(() -> new NotFoundException(input.profileId, Profile.class));
      if (!(EntitySpecifications.hasSameClient(
              profile
                  .getOwningClient()
                  .orElseThrow(() -> new ClientBoundaryViolationException(profile, client)))
          .isSatisfiedBy(client))) {
        throw new ClientBoundaryViolationException(profile, client);
      }
      return;
    }
    if (new HashSet<>(input.profileItemIds).size() != input.profileItemIds.size()) {
      throw new IllegalArgumentException("Provided catalogitems are not unique.");
    }
  }

  @Valid
  public record InputData(
      UUID unitId,
      UUID domainId,
      List<UUID> profileItemIds,
      UUID profileId,
      boolean mergeBidirectionalReferences)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(
      @Valid List<TemplateItemIncarnationDescription<ProfileItem, Profile>> references,
      Unit container)
      implements UseCase.OutputData {}
}
