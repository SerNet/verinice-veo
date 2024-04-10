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
package org.veo.core.repository;

import java.util.Set;

import org.veo.core.entity.Element;
import org.veo.core.entity.Entity;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.RiskRelated;
import org.veo.core.entity.SymIdentifiable;
import org.veo.core.entity.Versioned;
import org.veo.core.entity.ref.IEntityRef;

/** A service that helps to retrieve the correct {@link Repository} for a given type */
public interface RepositoryProvider {
  <T extends Entity, TRepo extends RepositoryBase<T, TRef>, TRef extends IEntityRef<T>>
      TRepo getRepositoryBaseFor(Class<T> entityType);

  <T extends Element> ElementRepository<T> getElementRepositoryFor(Class<T> entityType);

  <T extends Identifiable & Versioned>
      IdentifiableVersionedRepository<T> getVersionedIdentifiableRepositoryFor(Class<T> entityType);

  <T extends Identifiable> Repository<T> getRepositoryFor(Class<T> entityType);

  Set<ElementRepository<? extends RiskRelated>> getRiskRelatedElementRepos();

  <T extends SymIdentifiable<T, TNamespace>, TNamespace extends Identifiable>
      SymIdentifiableRepository<T, TNamespace> getSymRepositoryFor(Class<T> entityType);
}
