/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Urs Zeidler
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
package org.veo.core.entity.riskdefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true)
public class CategoryDefinition extends DimensionDefinition {
    public CategoryDefinition(String id, String name, String abbreviation, String description,
            List<List<RiskValue>> valueMatrix, List<CategoryLevel> potentialImpacts) {
        super(id, name, abbreviation, description);
        this.valueMatrix = valueMatrix;
        this.potentialImpacts = potentialImpacts;
        initLevel(potentialImpacts);
    }

    private List<List<RiskValue>> valueMatrix = new ArrayList<>();
    private List<CategoryLevel> potentialImpacts = new ArrayList<>();

    /**
     * returns a risk value from the matrix for the ProbabilityLevel and the
     * CategoryLevel.
     */
    public RiskValue getRiskValue(ProbabilityLevel plevel, CategoryLevel clevel) {
        if (clevel.getOrdinalValue() > valueMatrix.size() - 1)
            throw new IllegalArgumentException("No risk Value for ProbabilityLevel: "
                    + clevel.getName() + "[" + clevel.getOrdinalValue() + "]");
        List<RiskValue> riskValuesForProbability = valueMatrix.get(clevel.getOrdinalValue());
        if (plevel.getOrdinalValue() > riskValuesForProbability.size() - 1)
            throw new IllegalArgumentException("No risk Value for CategoryLevel: "
                    + plevel.getName() + "[" + plevel.getOrdinalValue() + "]");

        return riskValuesForProbability.get(plevel.getOrdinalValue());
    }

    public void setPotentialImpacts(List<CategoryLevel> potentialImpacts) {
        this.potentialImpacts = potentialImpacts;
        initLevel(potentialImpacts);
    }

    public void validateRiskCategory(@NotNull List<RiskValue> riskValues) {
        Set<RiskValue> containedValues = valueMatrix.stream()
                                                    .flatMap(x -> x.stream())
                                                    .collect(Collectors.toSet());
        containedValues.removeAll(riskValues);
        if (!containedValues.isEmpty()) {
            throw new IllegalArgumentException("Invalid risk values: " + containedValues);
        }

    }
}
