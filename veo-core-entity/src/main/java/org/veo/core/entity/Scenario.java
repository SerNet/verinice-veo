/*******************************************************************************
 * Copyright (c) 2020 Alexander Ben Nasrallah.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.entity;

/**
 * NIST: Threat Scenario: A set of discrete threat events, associated with a
 * specific threat source or multiple threat sources, partially ordered in time.
 */
public interface Scenario extends EntityLayerSupertype, CompositeEntity<Scenario> {

    @Override
    default String getModelType() {
        return EntityTypeNames.SCENARIO;
    }

    @Override
    default Class<? extends ModelObject> getModelInterface() {
        return Scenario.class;
    }
}
