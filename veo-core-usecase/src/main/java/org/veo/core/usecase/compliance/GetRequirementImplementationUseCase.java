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

import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.compliance.RequirementImplementation;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.base.AbstractUseCase;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.service.TypedId;

import lombok.Value;

public class GetRequirementImplementationUseCase
    extends AbstractUseCase<
        GetRequirementImplementationUseCase.InputData,
        GetRequirementImplementationUseCase.OutputData> {
  public GetRequirementImplementationUseCase(RepositoryProvider repositoryProvider) {
    super(repositoryProvider);
  }

  @Override
  public OutputData execute(InputData input) {
    var origin = getEntity(input.origin, input.authenticatedClient);
    var control = getEntity(input.control, input.authenticatedClient);
    return new OutputData(origin.getRequirementImplementation(control), ETag.from(origin));
  }

  @Value
  public static class InputData implements UseCase.InputData {
    Client authenticatedClient;
    TypedId<? extends RiskAffected<?, ?>> origin;
    TypedId<Control> control;
  }

  @Value
  public static class OutputData implements UseCase.OutputData {
    RequirementImplementation requirementImplementation;
    String eTag;
  }
}