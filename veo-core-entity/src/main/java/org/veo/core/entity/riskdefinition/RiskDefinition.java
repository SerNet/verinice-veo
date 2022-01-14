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

import static org.veo.core.entity.riskdefinition.DimensionDefinition.initLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.veo.core.entity.Constraints;

import lombok.Data;
import lombok.ToString;

@Data()
@ToString(onlyExplicitlyIncluded = true)
public class RiskDefinition {
    @NotNull(message = "An id must be present.")
    @Size(max = Constraints.DEFAULT_CONSTANT_MAX_LENGTH)
    @ToString.Include
    private String id;
    @NotNull
    private ProbabilityDefinition probability;
    @NotNull
    private ImplementationStateDefinition implementationStateDefinition;
    @NotNull
    private List<CategoryDefinition> categories = new ArrayList<>();
    @NotNull
    private List<RiskValue> riskValues = new ArrayList<>();
    @NotNull
    private RiskMethod riskMethod;

    public Optional<CategoryDefinition> getCategory(String categoryId) {
        return categories.stream()
                         .filter(c -> c.getId()
                                       .equals(categoryId))
                         .findAny();
    }

    public void setRiskValues(List<RiskValue> values) {
        this.riskValues = values;
        initLevel(values);
    }

    public void validateRiskDefinition() {
        categories.stream()
                  .forEach(cd -> cd.validateRiskCategory(riskValues));
    }
}
