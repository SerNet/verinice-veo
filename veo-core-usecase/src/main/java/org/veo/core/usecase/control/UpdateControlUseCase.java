/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.core.usecase.control;

import java.util.Map;

import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.event.RiskAffectingElementChangeEvent;
import org.veo.core.entity.risk.ControlRiskValues;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.repository.ControlRepository;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.base.ModifyElementUseCase;
import org.veo.core.usecase.base.ScopeProvider;
import org.veo.core.usecase.decision.Decider;

public class UpdateControlUseCase extends ModifyElementUseCase<Control> {
  private final EventPublisher eventPublisher;
  private final ScopeProvider scopeProvider;

  public UpdateControlUseCase(
      ControlRepository controlRepository,
      EventPublisher eventPublisher,
      ScopeProvider scopeProvider,
      Decider decider) {
    super(controlRepository, decider);
    this.eventPublisher = eventPublisher;
    this.scopeProvider = scopeProvider;
  }

  @Override
  public OutputData<Control> execute(InputData<Control> input) {
    OutputData<Control> result = super.execute(input);
    eventPublisher.publish(new RiskAffectingElementChangeEvent(result.getEntity(), this));
    return result;
  }

  @Override
  protected void validate(Control oldElement, Control newElement) {
    newElement
        .getDomains()
        .forEach(
            domain -> {
              newElement
                  .getRiskValues(domain)
                  .ifPresent(riskValueMap -> validateRiskValues(newElement, domain, riskValueMap));
            });
  }

  private void validateRiskValues(
      Control control, Domain domain, Map<RiskDefinitionRef, ControlRiskValues> riskValueMap) {
    riskValueMap
        .keySet()
        .forEach(
            riskDefinitionRef -> {
              if (!scopeProvider.canUseRiskDefinition(control, domain, riskDefinitionRef)) {
                throw new IllegalArgumentException(
                    String.format(
                        "Cannot use risk definition '%s' because the element is not a member of a scope with that risk definition",
                        riskDefinitionRef.getIdRef()));
              }
            });
  }
}
