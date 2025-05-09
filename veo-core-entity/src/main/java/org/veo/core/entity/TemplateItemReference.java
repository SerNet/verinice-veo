/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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

import java.util.Optional;

/** Basic type for catalog references. */
public interface TemplateItemReference<
        T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
    extends Identifiable, ClientOwned {

  /** The reference to an other catalogitem. */
  T getTarget();

  void setTarget(T target);

  T getOwner();

  void setOwner(T owner);

  @Override
  Optional<Client> getOwningClient();
}
