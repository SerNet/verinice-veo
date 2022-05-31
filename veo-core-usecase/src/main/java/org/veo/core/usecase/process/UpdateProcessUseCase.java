/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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
package org.veo.core.usecase.process;

import org.veo.core.entity.Process;
import org.veo.core.entity.event.RiskAffectingElementChangeEvent;
import org.veo.core.repository.ProcessRepository;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.base.ModifyElementUseCase;
import org.veo.core.usecase.decision.Decider;

/** Update a persisted process object. */
public class UpdateProcessUseCase extends ModifyElementUseCase<Process> {
  private final EventPublisher eventPublisher;

  public UpdateProcessUseCase(
      ProcessRepository processRepository, EventPublisher eventPublisher, Decider decider) {
    super(processRepository, decider);
    this.eventPublisher = eventPublisher;
  }

  @Override
  public OutputData<Process> execute(InputData<Process> input) {
    OutputData<Process> result = super.execute(input);
    eventPublisher.publish(new RiskAffectingElementChangeEvent(result.getEntity(), this));
    return result;
  }

  @Override
  protected void evaluateDecisions(Process entity, Process storedEntity) {
    // FIXME VEO-839
    // Transfer risks from stored element because they may be relevant for risk
    // evaluation
    entity.setRisks(storedEntity.getRisks());
    super.evaluateDecisions(entity, storedEntity);
  }
}
