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

import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.veo.core.entity.Constraints;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * The basic class for a dimension definition. A dimension definition has an
 * unique id and can work with {@link DiscreteValue} as level.
 */
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class DimensionDefinition {
    protected static final String DIMENSION_PROBABILITY = "Prob";
    protected static final String DIMENSION_IMPLEMENTATION_STATE = "Ctr";

    @NotNull(message = "An id must be present.")
    @Size(max = Constraints.DEFAULT_CONSTANT_MAX_LENGTH)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;
    @NotNull(message = "A name must be present.")
    @Size(max = Constraints.DEFAULT_CONSTANT_MAX_LENGTH)
    @ToString.Include
    private String name;
    @Size(max = Constraints.DEFAULT_CONSTANT_MAX_LENGTH)
    private String abbreviation;
    @Size(max = Constraints.DEFAULT_CONSTANT_MAX_LENGTH)
    private String description;

    /**
     * Initialize the ordinal value of each DiscreteValue in the list.
     */
    static void initLevel(List<? extends DiscreteValue> discretValues) {
        discretValues.stream()
                     .forEachOrdered(cl -> cl.setOrdinalValue(discretValues.indexOf(cl)));
    }

}