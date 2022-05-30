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
package org.veo.rest.schemas;

import java.util.Map;
import java.util.Set;

import org.veo.core.entity.decision.DecisionRef;
import org.veo.core.entity.decision.DecisionResult;
import org.veo.core.entity.inspection.Finding;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "EvaluateElementOutput",
    description = "Results of decisions and inspections on given transient element in given domain",
    accessMode = Schema.AccessMode.READ_ONLY)
public interface EvaluateElementOutputSchema {
  @Schema(description = "Results of all decisions in the domain")
  Map<DecisionRef, DecisionResult> getDecisionResults();

  @Schema(description = "Findings yielded by all inspections in the domain")
  Set<Finding> getInspectionFindings();
}
