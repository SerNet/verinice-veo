/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jochen Kemnade.
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
package org.veo.core.usecase;

public class MigrationFailedException extends RuntimeException {

  private static final long serialVersionUID = 6053728693818238191L;

  private final int itemCount;
  private final int failureCount;

  public MigrationFailedException(String message, int itemCount, int failureCount) {
    super(message);
    this.itemCount = itemCount;
    this.failureCount = failureCount;
  }

  public static MigrationFailedException forUnit(int elementCount, int failureCount) {
    return new MigrationFailedException(
        "Migration of unit failed for " + failureCount + " of " + elementCount + " elements(s)",
        elementCount,
        failureCount);
  }

  public static MigrationFailedException forDomain(int unitCount, int failureCount) {
    return new MigrationFailedException(
        "Migration failed for " + failureCount + " of " + unitCount + " unit(s)",
        unitCount,
        failureCount);
  }

  public static MigrationFailedException forClient(int clientCount, int failureCount) {
    return new MigrationFailedException(
        "Migration failed for " + failureCount + " of " + clientCount + " client(s)",
        clientCount,
        failureCount);
  }

  public int getFailureCount() {
    return failureCount;
  }

  public int getItemCount() {
    return itemCount;
  }
}
