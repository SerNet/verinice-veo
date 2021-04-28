/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
 * Something that happened.
 */
public interface Incident extends EntityLayerSupertype, CompositeEntity<Incident> {

    String SINGULAR_TERM = "incident";
    String PLURAL_TERM = "incidents";
    String TYPE_DESIGNATOR = "INC";

    @Override
    default String getModelType() {
        return SINGULAR_TERM;
    }

    @Override
    default Class<? extends ModelObject> getModelInterface() {
        return Incident.class;
    }

    @Override
    default String getTypeDesignator() {
        return TYPE_DESIGNATOR;
    }
}
