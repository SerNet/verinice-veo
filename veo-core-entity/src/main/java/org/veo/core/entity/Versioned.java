/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Urs Zeidler.
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

import java.time.Instant;

/**
 * A versioned entity with a sequential version number and some metadata (time & author of creation
 * and last modification).
 */
public interface Versioned {
  Instant getCreatedAt();

  void setCreatedAt(Instant createdAt);

  Instant getUpdatedAt();

  void setUpdatedAt(Instant updatedAt);

  String getCreatedBy();

  void setCreatedBy(String username);

  String getUpdatedBy();

  void setUpdatedBy(String username);

  /**
   * The version number is used for optimistic locking and should be increased by the persistence
   * provider according to JSR-338 Ch. 3.4.2
   *
   * <p>This differs from the change number (see below) in certain situations - for instance deleted
   * entities will not get an increase in their version number, certain lock modes will increase the
   * version number disregarding whether actual changes will take place etc.
   */
  long getVersion();

  void setVersion(long version);

  /**
   * Returns the change number.
   *
   * <p>The change number starts at 0 for a new object and should be increased whenever the entity
   * is edited by the user.
   */
  long getChangeNumber();

  /**
   * Return the next change number in sequence.
   *
   * @return the next change number
   */
  long nextChangeNumberForUpdate();

  /**
   * Returns the first change number. Can be used by callers to make sure that they are indeed
   * handling the very first change (insert) of this entity.
   *
   * @throws UnexpectedChangeNumberException when the change number is not at its starting value
   */
  long initialChangeNumberForInsert();

  void consolidateChangeNumber(long lowestSeenChangeNo);
}
