/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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
package org.veo.core.usecase.repository;

import org.veo.core.entity.Document;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A repository for <code>Document</code> entities.
 *
 * Implements basic CRUD operations from the superinterface and extends them
 * with more specific methods - i.e. queries based on particular fields.
 *
 * @deprecated use {@link org.veo.core.repository.DocumentRepository}
 */
@Deprecated
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_INTERFACE")
public interface DocumentRepository extends org.veo.core.repository.DocumentRepository,
        EntityLayerSupertypeRepository<Document> {

}
