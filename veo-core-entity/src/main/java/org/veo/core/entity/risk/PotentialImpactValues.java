/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jochen Kemnade
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
package org.veo.core.entity.risk;

import java.util.Map;

/**
 * This represent an associated array of impact values, it describes one impact value. The impact
 * value is a vector with each {@link CategoryRef} as dimension index.
 */
public interface PotentialImpactValues {
  /** Map of categorized {@link CategoryRef} impact values * */
  Map<String, ImpactRef> getPotentialImpacts();
}
