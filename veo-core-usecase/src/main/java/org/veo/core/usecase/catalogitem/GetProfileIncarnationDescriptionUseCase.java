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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.Unit;
import org.veo.core.repository.ProfileRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.parameter.TemplateItemIncarnationDescription;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class GetProfileIncarnationDescriptionUseCase
    extends AbstractGetIncarnationDescriptionUseCase<ProfileItem>
    implements TransactionalUseCase<
        GetProfileIncarnationDescriptionUseCase.InputData,
        GetProfileIncarnationDescriptionUseCase.OutputData> {
  private final UnitRepository unitRepository;
  private final ProfileRepository profileRepository;

  @Override
  public OutputData execute(InputData input) {
    log.info(
        "profile: {}, unid: {}, items: {}", input.profileId, input.unitId, input.profileItemIds);
    Unit unit = unitRepository.getByIdFetchClient(input.getUnitId());
    unit.checkSameClient(input.authenticatedClient);

    validateInput(input);

    var incarnationDescriptions =
        Optional.ofNullable(input.profileId)
            .map(profileRepository::findAllByIdsFetchDomainAndTailoringReferences)
            .orElseGet(
                () ->
                    profileRepository.findAllByIdsFetchDomainAndTailoringReferences(
                        Set.copyOf(input.profileItemIds)))
            .stream()
            .map(
                catalogItem ->
                    new TemplateItemIncarnationDescription(
                        catalogItem,
                        toParameters(catalogItem.getTailoringReferences(), Collections.emptyMap())))
            .toList();

    log.info("IncarnationDescription: {}", incarnationDescriptions);
    return new OutputData(incarnationDescriptions, unit);
  }

  private void validateInput(InputData input) {
    if (input.getProfileId() != null) return;
    if (input.profileItemIds.stream().collect(Collectors.toSet()).size()
        != input.profileItemIds.size()) {
      throw new IllegalArgumentException("Provided catalogitems are not unique.");
    }
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    Client authenticatedClient;
    Key<UUID> unitId;
    List<Key<UUID>> profileItemIds;
    Key<UUID> profileId;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid List<TemplateItemIncarnationDescription> references;
    Unit container;
  }
}
