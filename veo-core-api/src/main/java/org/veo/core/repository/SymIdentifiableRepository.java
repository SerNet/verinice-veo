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
package org.veo.core.repository;

import org.veo.core.entity.Identifiable;
import org.veo.core.entity.SymIdentifiable;
import org.veo.core.entity.ref.ITypedSymbolicId;

public interface SymIdentifiableRepository<
        T extends SymIdentifiable<T, TNamespace>, TNamespace extends Identifiable>
    extends RepositoryBase<T, ITypedSymbolicId<T, ? extends TNamespace>> {}
