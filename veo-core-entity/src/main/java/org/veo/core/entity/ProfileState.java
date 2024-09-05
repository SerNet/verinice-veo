/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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

import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.event.ClientChangedEvent;
import org.veo.core.entity.state.ProfileItemState;

public interface ProfileState {
  int PRODUCT_ID_MAX_LENGTH = Constraints.DEFAULT_CONSTANT_MAX_LENGTH;

  UUID getSelfId();

  /** Human-readable profile name in the profile's language */
  String getName();

  /**
   * Technical key for licensing purposes.
   *
   * <p>This ID is not unique. Multiple profiles can share the same product ID, even in the same
   * domain (e.g., when a profile exists in different languages).
   *
   * <p>A client can obtain a license for a profile product ID, allowing them to receive a copy of
   * all available profiles with that product ID (see {@link
   * ClientChangedEvent#getDomainProducts()}).
   */
  String getProductId();

  String getDescription();

  String getLanguage();

  Set<ProfileItemState> getItemStates();
}
