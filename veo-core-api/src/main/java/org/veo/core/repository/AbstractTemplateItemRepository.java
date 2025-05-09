/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.Client;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TemplateItem;

public interface AbstractTemplateItemRepository<
        T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
    extends SymIdentifiableRepository<T, TNamespace> {

  Set<TailoringReference<T, TNamespace>> findTailoringReferencesByIds(Set<UUID> ids, Client client);

  T save(T item);

  void saveAll(Collection<T> templateItems);
}
