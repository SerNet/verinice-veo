/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade
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
package org.veo.persistence.access.jpa;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import org.veo.persistence.entity.jpa.IdentifiableVersionedData;

@NoRepositoryBean
public interface IdentifiableVersionedDataRepository<T extends IdentifiableVersionedData>
    extends CrudRepository<T, String> {

  @Query("select version from #{#entityName} where dbId = ?1")
  @Nonnull
  Optional<Long> getVersion(@Nonnull String id);
}
