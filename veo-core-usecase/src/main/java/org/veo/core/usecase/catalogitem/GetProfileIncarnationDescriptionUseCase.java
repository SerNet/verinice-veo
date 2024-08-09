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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.entity.specification.EntitySpecifications;
import org.veo.core.repository.ProfileRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.parameter.TemplateItemIncarnationDescription;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class GetProfileIncarnationDescriptionUseCase
    extends AbstractGetIncarnationDescriptionUseCase<ProfileItem, Profile>
    implements TransactionalUseCase<
        GetProfileIncarnationDescriptionUseCase.InputData,
        GetProfileIncarnationDescriptionUseCase.OutputData> {
  private final UnitRepository unitRepository;
  private final ProfileRepository profileRepository;

  @Override
  public OutputData execute(InputData input) {
    log.info(
        "profile: {}, unit: {}, items: {}", input.profileId, input.unitId, input.profileItemIds);
    Unit unit = unitRepository.getByIdFetchClient(input.unitId);
    unit.checkSameClient(input.authenticatedClient);

    validateInput(input);

    var distinctTailoringRefKeys = new HashSet<String>();
    var incarnationDescriptions =
        Optional.ofNullable(input.profileId)
            .map(
                id ->
                    profileRepository.findItemsByProfileIdFetchDomainAndTailoringReferences(
                        id, input.authenticatedClient))
            .orElseGet(
                () ->
                    profileRepository.findItemsByIdsFetchDomainAndTailoringReferences(
                        Set.copyOf(input.profileItemIds), input.authenticatedClient))
            .stream()
            .map(
                catalogItem ->
                    new TemplateItemIncarnationDescription<>(
                        catalogItem,
                        toParameters(
                            input.mergeBidirectionalReferences
                                ? catalogItem.getTailoringReferences().stream()
                                    .filter(tr -> distinctTailoringRefKeys.add(toKey(tr)))
                                    .toList()
                                : catalogItem.getTailoringReferences(),
                            Collections.emptyMap())))
            .toList();

    log.debug("IncarnationDescriptions: {}", incarnationDescriptions);
    return new OutputData(incarnationDescriptions, unit);
  }

  private String toKey(TailoringReference<?, ?> tailoringReference) {
    String origin = tailoringReference.getOwner().getSymbolicIdAsString();
    String target = tailoringReference.getTargetRef().getSymbolicId().toString();
    return switch (tailoringReference.getReferenceType()) {
      case LINK ->
          toKey(
              TailoringReferenceType.LINK,
              origin,
              target,
              ((LinkTailoringReference<?, ?>) tailoringReference).getLinkType());
      case LINK_EXTERNAL ->
          toKey(
              TailoringReferenceType.LINK,
              target,
              origin,
              ((LinkTailoringReference<?, ?>) tailoringReference).getLinkType());
      case PART -> toKey(TailoringReferenceType.PART, origin, target, "");
      case COMPOSITE -> toKey(TailoringReferenceType.PART, target, origin, "");
      case SCOPE -> toKey(TailoringReferenceType.SCOPE, origin, target, "");
      case MEMBER -> toKey(TailoringReferenceType.SCOPE, target, origin, "");
      case RISK -> toKey(TailoringReferenceType.RISK, origin, target, "");
      case CONTROL_IMPLEMENTATION ->
          toKey(TailoringReferenceType.CONTROL_IMPLEMENTATION, origin, target, "");
      default ->
          throw new IllegalArgumentException(
              "Unexpected tailoring reference type %s"
                  .formatted(tailoringReference.getReferenceType()));
    };
  }

  private String toKey(
      TailoringReferenceType type, String sourceId, String targetId, String linkType) {
    return new StringBuffer()
        .append(type.name())
        .append(sourceId)
        .append(targetId)
        .append(linkType)
        .toString();
  }

  private void validateInput(InputData input) {
    input.authenticatedClient.getDomains().stream()
        .filter(d -> d.getIdAsString().equals(input.domainId.uuidValue()))
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
                  .orElseThrow(
                      () ->
                          new ClientBoundaryViolationException(profile, input.authenticatedClient)))
          .isSatisfiedBy(input.authenticatedClient))) {
        throw new ClientBoundaryViolationException(profile, input.authenticatedClient);
      }
      return;
    }
    if (new HashSet<>(input.profileItemIds).size() != input.profileItemIds.size()) {
      throw new IllegalArgumentException("Provided catalogitems are not unique.");
    }
  }

  @Valid
  public record InputData(
      Client authenticatedClient,
      Key<UUID> unitId,
      Key<UUID> domainId,
      List<Key<UUID>> profileItemIds,
      Key<UUID> profileId,
      boolean mergeBidirectionalReferences)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(
      @Valid List<TemplateItemIncarnationDescription<ProfileItem, Profile>> references,
      Unit container)
      implements UseCase.OutputData {}
}
