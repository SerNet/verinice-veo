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
 * An entity as defined in Domain Driven Design.
 *
 * <p>Not to be confused with {@link Element}, which is a subcategory of entities.
 */
public interface Entity {
  /**
   * @return Lowercase singular name for the specific type of {@link Entity}
   */
  String getModelType();

  /**
   * @return The specific interface for this type of {@link Entity}
   */
  Class<? extends Entity> getModelInterface();
}
