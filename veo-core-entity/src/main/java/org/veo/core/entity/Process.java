/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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
package org.veo.core.entity;

/**
 * A process is a series of business activites that use specific assets.
 */
public interface Process
        extends EntityLayerSupertype, CompositeEntity<Process>, RiskAffected<Process, ProcessRisk> {

    String SINGULAR_TERM = "process";
    String PLURAL_TERM = "processes";
    String TYPE_DESIGNATOR = "PRO";

    @Override
    default Class<? extends ModelObject> getModelInterface() {
        return Process.class;
    }

    @Override
    default String getModelType() {
        return SINGULAR_TERM;
    }

    @Override
    default String getTypeDesignator() {
        return TYPE_DESIGNATOR;
    }
}
