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
 * An asset describes a part in the unit. It could be a physical element like a
 * computer, but also something not physical like a software for example.
 */
public interface Asset extends Element, CompositeElement<Asset>, RiskAffected<Asset, AssetRisk> {

    String SINGULAR_TERM = "asset";
    String PLURAL_TERM = "assets";
    String TYPE_DESIGNATOR = "AST";

    @Override
    default String getModelType() {
        return SINGULAR_TERM;
    }

    @Override
    default Class<? extends Identifiable> getModelInterface() {
        return Asset.class;
    }

    @Override
    default String getTypeDesignator() {
        return TYPE_DESIGNATOR;
    }

}
