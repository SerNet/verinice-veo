/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan.
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

public interface Designated {
  /**
   * @return A compact human-readable identifier that is unique within the client.
   */
  String getDesignator();

  /**
   * @param designator A compact human-readable identifier that is unique within the client.
   */
  void setDesignator(String designator);

  /**
   * @return A 3-character designator that is constant for the entity type (or family of entity
   *     types) and acts as a prefix to the individual object designators.
   */
  String getTypeDesignator();
}
