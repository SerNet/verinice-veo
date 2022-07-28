/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.core.usecase.domain;

import java.util.UUID;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.profile.ProfileRef;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public class ApplyProfileUseCase
    implements TransactionalUseCase<ApplyProfileUseCase.InputData, UseCase.EmptyOutput> {
  private final DomainRepository domainRepository;
  private final ProfileApplier profileApplier;
  private final UnitRepository unitRepository;

  @Override
  public EmptyOutput execute(InputData input) {
    var domain =
        domainRepository
            .findById(input.domainId, input.clientId)
            .orElseThrow(() -> new NotFoundException(input.domainId, Domain.class));

    var unit =
        unitRepository
            .findById(input.unitId)
            .orElseThrow(() -> new NotFoundException(input.unitId, Unit.class));
    unit.checkSameClient(domain.getOwner());

    profileApplier.applyProfile(domain, input.profile, unit);
    return EmptyOutput.INSTANCE;
  }

  @Value
  public static class InputData implements UseCase.InputData {
    Key<UUID> clientId;
    Key<UUID> domainId;
    ProfileRef profile;
    Key<UUID> unitId;
  }
}
