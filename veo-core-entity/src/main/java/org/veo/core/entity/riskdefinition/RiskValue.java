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

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.veo.core.entity.Nameable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true)
public class RiskValue extends DiscreteValue {

    public RiskValue(int ordinalValue, String name, String abbreviation, String description,
            String htmlColor,
            @NotNull(message = "A symbolic risk value must be present.") @Size(max = 255) String sybolicRisk) {
        super(name, abbreviation, description, htmlColor);
        this.symbolicRisk = sybolicRisk;
        setOrdinalValue(ordinalValue);
    }

    @NotNull(message = "A symbolic risk value must be present.")
    @Size(max = Nameable.NAME_MAX_LENGTH)
    @ToString.Include
    private String symbolicRisk;
}
