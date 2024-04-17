/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
 * Based on the definition of the term 'risk' from NIST 800-37:
 *
 * <p>"Risk: A measure of the extent to which an entity is threatened by a potential circumstance or
 * event, [...]",
 *
 * <p>this class links an asset ('entity' in the above definition) to a scenario ('circumstance or
 * event').
 *
 * <p>The asset may be representing a single asset or a group of assets to facilitate modelling the
 * system-of-systems / system-of-interest (SoI) for the observed risk.
 */
public interface AssetRisk extends AbstractRisk<Asset, AssetRisk> {

  String SINGULAR_TERM = Asset.SINGULAR_TERM + "-risk";
  String PLURAL_TERM = Asset.SINGULAR_TERM + "-risks";

  @Override
  default String getModelType() {
    return SINGULAR_TERM;
  }

  @Override
  default Class<AssetRisk> getModelInterface() {
    return AssetRisk.class;
  }
}
