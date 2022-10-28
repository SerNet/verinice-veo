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

import java.util.Set;

import org.veo.core.entity.Nameable;

/** Collects the Attributes of the v1 version of the risk objects. */
@Deprecated
public final class DeprecatedAttributes {
  static final String ABBREVIATION = Nameable.ABBREVIATION;
  static final String DESCRIPTION = Nameable.DESCRIPTION;
  static final String NAME = Nameable.NAME;

  static final Set<String> DEPRECATED_ATTRIBUTES =
      Set.of(Nameable.NAME, Nameable.ABBREVIATION, Nameable.DESCRIPTION);

  private DeprecatedAttributes() {
    super();
  }
}
