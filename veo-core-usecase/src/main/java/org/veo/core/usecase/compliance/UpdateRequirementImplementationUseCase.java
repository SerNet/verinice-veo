/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
package org.veo.core.usecase.compliance;

import java.time.Instant;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Control;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.entity.state.RequirementImplementationState;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.base.AbstractUseCase;
import org.veo.core.usecase.base.DomainSensitiveElementValidator;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.service.EntityStateMapper;
import org.veo.core.usecase.service.RefResolverFactory;

public class UpdateRequirementImplementationUseCase
    extends AbstractUseCase<
        UpdateRequirementImplementationUseCase.InputData,
        UpdateRequirementImplementationUseCase.OutputData> {
  private final RefResolverFactory refResolverFactory;
  private final EntityStateMapper entityStateMapper;

  public UpdateRequirementImplementationUseCase(
      RepositoryProvider repositoryProvider,
      RefResolverFactory refResolverFactory,
      EntityStateMapper entityStateMapper) {
    super(repositoryProvider);
    this.refResolverFactory = refResolverFactory;
    this.entityStateMapper = entityStateMapper;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    var origin = getEntity(input.origin, userAccessRights);
    var control = getEntity(input.control, userAccessRights);
    var requirementImplementation = origin.getRequirementImplementation(control);
    userAccessRights.checkElementWriteAccess(origin);
    ETag.validate(input.eTag, origin);
    var idRefResolver = refResolverFactory.db(origin.getOwningClient().get());
    entityStateMapper.mapState(input.state, requirementImplementation, idRefResolver);
    origin.setUpdatedAt(Instant.now());
    DomainSensitiveElementValidator.validate(origin);
    return new OutputData(ETag.from(getEntity(input.origin, userAccessRights)));
  }

  public record InputData(
      TypedId<? extends RiskAffected<?, ?>> origin,
      TypedId<Control> control,
      RequirementImplementationState state,
      String eTag)
      implements UseCase.InputData {}

  public record OutputData(String eTag) implements UseCase.OutputData {}
}
