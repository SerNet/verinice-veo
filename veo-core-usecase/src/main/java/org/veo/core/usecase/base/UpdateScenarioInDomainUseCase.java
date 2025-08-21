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
package org.veo.core.usecase.base;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.event.RiskAffectingElementChangeEvent;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.decision.Decider;
import org.veo.core.usecase.service.EntityStateMapper;
import org.veo.core.usecase.service.RefResolverFactory;

public class UpdateScenarioInDomainUseCase extends UpdateElementInDomainUseCase<Scenario> {
  private final EventPublisher eventPublisher;

  public UpdateScenarioInDomainUseCase(
      RepositoryProvider repositoryProvider,
      Decider decider,
      EntityStateMapper entityStateMapper,
      EventPublisher eventPublisher,
      RefResolverFactory refResolverFactory) {
    super(
        repositoryProvider.getElementRepositoryFor(Scenario.class),
        refResolverFactory,
        decider,
        entityStateMapper);
    this.eventPublisher = eventPublisher;
  }

  @Override
  public OutputData<Scenario> execute(
      InputData<Scenario> input, UserAccessRights userAccessRights) {
    var result = super.execute(input, userAccessRights);
    eventPublisher.publish(new RiskAffectingElementChangeEvent(result.entity(), this));
    return result;
  }
}
