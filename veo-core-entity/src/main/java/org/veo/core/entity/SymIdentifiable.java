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

import java.util.UUID;

/**
 * An {@link Entity} with a symbolic ID. A symbolic ID is not globally unique, but must be unique
 * within the entity's parent entity. Therefore, the parent entity is called "namespace".
 *
 * <p>Symbolic IDs allow traceability across systems, i.e., when a symbolically identifiable entity
 * is exported and then imported on a different server, the original symbolic ID remains the same on
 * the imported entity, and it can be referenced by future imports.
 *
 * @param <TNamespace> Type of the parent entity in which the symbolic ID must be unique
 * @param <T> Concrete type of the symbolically identifiable entity
 */
public interface SymIdentifiable<
        T extends SymIdentifiable<T, TNamespace>, TNamespace extends Identifiable>
    extends Entity {
  @Override
  Class<? extends T> getModelInterface();

  UUID getSymbolicId();

  String getSymbolicIdAsString();

  void setSymbolicId(UUID symbolicId);

  /** The parent entity in which the symbolic ID must be unique. */
  TNamespace getNamespace();
}
