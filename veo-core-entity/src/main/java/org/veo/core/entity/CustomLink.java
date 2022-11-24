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

import jakarta.validation.constraints.NotNull;

/**
 * A link connects two {@link Element} objects. It serves documentation purposes only. It's defined
 * by its source (owning) entity & target entity, but may also hold dynamic attributes that document
 * the link (just like the attributes on a custom aspect).
 *
 * @see CustomAspect
 */
public interface CustomLink extends CustomAttributeContainer {

  @NotNull
  Element getTarget();

  void setTarget(Element aTarget);

  @NotNull
  Element getSource();

  void setSource(Element aSource);

  void remove();
}
