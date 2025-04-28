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

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.compliance.RequirementImplementation;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.base.AbstractUseCase;
import org.veo.core.usecase.common.ETag;

public class GetRequirementImplementationUseCase
    extends AbstractUseCase<
        GetRequirementImplementationUseCase.InputData,
        GetRequirementImplementationUseCase.OutputData> {
  public GetRequirementImplementationUseCase(RepositoryProvider repositoryProvider) {
    super(repositoryProvider);
  }

  @Override
  public OutputData execute(InputData input) {
    var origin = getEntity(input.origin, input.user);
    var control = getEntity(input.control, input.user);
    return new OutputData(origin.getRequirementImplementation(control), ETag.from(origin));
  }

  public record InputData(
      UserAccessRights user,
      Client authenticatedClient,
      TypedId<? extends RiskAffected<?, ?>> origin,
      TypedId<Control> control)
      implements UseCase.InputData {}

  public record OutputData(RequirementImplementation requirementImplementation, String eTag)
      implements UseCase.OutputData {}
}
