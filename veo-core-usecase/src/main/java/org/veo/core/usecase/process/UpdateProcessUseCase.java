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

import java.util.Map;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Process;
import org.veo.core.entity.event.RiskComponentChangeEvent;
import org.veo.core.entity.risk.ProcessImpactValues;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.repository.ProcessRepository;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.base.ModifyElementUseCase;
import org.veo.core.usecase.base.ScopeProvider;
import org.veo.core.usecase.decision.Decider;

/**
 * Update a persisted process object.
 */
public class UpdateProcessUseCase extends ModifyElementUseCase<Process> {
    private final EventPublisher eventPublisher;
    private final ScopeProvider scopeProvider;

    public UpdateProcessUseCase(ProcessRepository processRepository, EventPublisher eventPublisher,
            ScopeProvider scopeProvider, Decider decider) {
        super(processRepository, decider);
        this.eventPublisher = eventPublisher;
        this.scopeProvider = scopeProvider;
    }

    @Override
    public OutputData<Process> execute(InputData<Process> input) {
        OutputData<Process> result = super.execute(input);
        eventPublisher.publish(new RiskComponentChangeEvent(result.getEntity()));
        return result;
    }

    @Override
    protected void validate(Process oldElement, Process newElement) {
        newElement.getDomains()
                  .forEach(domain -> {
                      newElement.getImpactValues(domain)
                                .ifPresent(impactMap -> validateRiskValues(newElement, domain,
                                                                           impactMap));
                  });
    }

    private void validateRiskValues(Process control, Domain domain,
            Map<RiskDefinitionRef, ProcessImpactValues> riskValueMap) {
        riskValueMap.keySet()
                    .forEach(riskDefinitionRef -> {
                        if (!scopeProvider.canUseRiskDefinition(control, domain,
                                                                riskDefinitionRef)) {
                            throw new IllegalArgumentException(
                                    String.format("Cannot use risk definition '%s' because the element is not a member of a scope with that risk definition",
                                                  riskDefinitionRef.getIdRef()));
                        }
                    });
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
