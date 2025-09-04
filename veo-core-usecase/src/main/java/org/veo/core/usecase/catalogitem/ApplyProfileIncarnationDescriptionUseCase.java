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

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Element;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.state.TemplateItemIncarnationDescriptionState;
import org.veo.core.repository.ProfileItemRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.parameter.TemplateItemIncarnationDescription;

import lombok.RequiredArgsConstructor;

/** Uses a list of {@link TemplateItemIncarnationDescription}s to create profile items in a unit. */
@RequiredArgsConstructor
public class ApplyProfileIncarnationDescriptionUseCase
    implements TransactionalUseCase<
        ApplyProfileIncarnationDescriptionUseCase.InputData,
        ApplyProfileIncarnationDescriptionUseCase.OutputData> {
  private final ProfileItemRepository profileItemRepository;
  private final IncarnationDescriptionApplier applier;
  private final UnitRepository unitRepository;

  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    var unit =
        unitRepository
            .findById(input.unitId, userAccessRights)
            .orElseThrow(() -> new NotFoundException(input.unitId(), Unit.class));
    userAccessRights.checkElementWriteAccess(unit);
    return new OutputData(
        applier.incarnate(unit, input.descriptions, profileItemRepository, unit.getClient()));
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(
      @NotNull UUID unitId,
      @NotNull List<TemplateItemIncarnationDescriptionState<ProfileItem, Profile>> descriptions)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(@Valid List<Element> newElements) implements UseCase.OutputData {}
}
