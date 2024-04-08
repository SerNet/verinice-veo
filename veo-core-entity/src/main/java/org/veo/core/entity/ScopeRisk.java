/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Finn Westendorf
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
 * <p>this class links a scope ('entity' in the above definition) to a scenario ('circumstance or
 * event').
 *
 * <p>The process may be representing a single scope or a group of scopes to facilitate modelling
 * subscope affected by the observed risk.
 */
public interface ScopeRisk extends AbstractRisk<Scope, ScopeRisk> {
  String SINGULAR_TERM = Scope.SINGULAR_TERM + "-risk";
  String PLURAL_TERM = Scope.SINGULAR_TERM + "-risks";

  @Override
  default String getModelType() {
    return SINGULAR_TERM;
  }

  @Override
  default Class<ScopeRisk> getModelInterface() {
    return ScopeRisk.class;
  }
}
