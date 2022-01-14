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
package org.veo.adapter.presenter.api.dto;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.veo.core.entity.Nameable;
import org.veo.core.entity.riskdefinition.CategoryDefinition;
import org.veo.core.entity.riskdefinition.ImplementationStateDefinition;
import org.veo.core.entity.riskdefinition.ProbabilityDefinition;

import lombok.Data;
import lombok.ToString;

@Data
public class RiskDefinitionDto {
    @NotNull(message = "An id must be present.")
    @Size(max = Nameable.NAME_MAX_LENGTH)
    @ToString.Include
    private String id;

    private List<ProbabilityDefinition> probabilities = new ArrayList<>();
    private List<ImplementationStateDefinition> controlImplementationState = new ArrayList<>();
    private List<CategoryDefinition> categories = new ArrayList<>();

    public String getName() {
        return id + "_riskdefinition_name";
    }

    public String getAabbreviation() {
        return id + "_riskdefinition_abbreviation";
    }

    public String getDescription() {
        return id + "_riskdefinition_description";
    }

}
