/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Ben Nasrallah.
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
package org.veo.core.usecase.scenario;

import org.veo.core.entity.Scenario;
import org.veo.core.entity.event.RiskComponentChangeEvent;
import org.veo.core.repository.ScenarioRepository;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.base.ModifyElementUseCase;
import org.veo.core.usecase.decision.Decider;

public class UpdateScenarioUseCase extends ModifyElementUseCase<Scenario> {

  private final EventPublisher eventPublisher;

  public UpdateScenarioUseCase(
      ScenarioRepository scenarioRepository, EventPublisher eventPublisher, Decider decider) {
    super(scenarioRepository, decider);
    this.eventPublisher = eventPublisher;
  }

  @Override
  public OutputData<Scenario> execute(InputData<Scenario> input) {
    OutputData<Scenario> result = super.execute(input);
    eventPublisher.publish(new RiskComponentChangeEvent(result.getEntity()));
    return result;
  }

  @Override
  protected void validate(Scenario oldElement, Scenario newElement) {
    // GNDN
  }
}
