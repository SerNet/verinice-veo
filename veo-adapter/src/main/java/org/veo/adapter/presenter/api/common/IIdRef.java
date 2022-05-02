/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.adapter.presenter.api.common;

/**
 * A representation of a URI reference to an {@link org.veo.core.entity.Identifiable} resource.
 * Offers methods to retrieve a resource identifier / resource locator to the resource itself and an
 * end-user friendly name that can be used when rendering the URI/URL. It also features URIs that
 * can be used to query or access the complete collection of identifiable resources.
 */
public interface IIdRef extends Ref {

  /** A user friendly name of the target object. */
  String getDisplayName();

  /**
   * Returns a URI of searches for the target type that can be used for discovery. It may be a URL.
   */
  String getSearchesUri();

  /**
   * Returns a URI of a collection of objects for the target type that may be used for discovery. It
   * may be a URL.
   */
  String getResourcesUri();
}
