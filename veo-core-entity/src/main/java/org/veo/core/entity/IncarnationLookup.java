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

/**
 * Describes different strategies to determine when to look for existing incarnations of an item
 * during the creation of incarnation descriptions. Items for which an existing incarnation has been
 * found will not be incarnated as new elements. The existing incarnation will represent the item in
 * the context of the request. This means that all references to the item will be resolved using
 * that existing element. There is a differentiation between:
 *
 * <ul>
 *   <li>Requested items: Items that the user explicitly wishes to incarnate
 *   <li>Referenced items: The targets of the requested items' tailoring references. These can also
 *       be indirectly referenced items (i.e. targets of other referenced items' tailoring
 *       references).
 * </ul>
 */
public enum IncarnationLookup {
  /**
   * Never search for existing incarnations. Both requested items and referenced items are always
   * incarnated as new elements.
   */
  NEVER,
  /**
   * Only search for existing incarnations of referenced items. Referenced items are only incarnated
   * as new items if no existing incarnation has been found in the unit. Requested items are always
   * incarnated as new elements.
   */
  FOR_REFERENCED_ITEMS,
  /**
   * Search for existing incarnations of both requested and referenced items. Requested items and
   * referenced items are only created as new elements if no existing incarnation has been found in
   * the unit.
   */
  ALWAYS,
}
