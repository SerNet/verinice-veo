/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman.
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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

import org.veo.persistence.entity.jpa.ElementData;

@NoRepositoryBean
public interface CompositeEntityDataRepository<T extends ElementData>
    extends ElementDataRepository<T> {
  @Query("select distinct e from #{#entityName} as e inner join e.parts p where p.id IN ?1")
  List<T> findAllByParts(Set<UUID> partIds);

  @Transactional(readOnly = true)
  @EntityGraph(attributePaths = {"parts"})
  List<T> findAllWithPartsByIdIn(List<UUID> ids);

  @Transactional(readOnly = true)
  @EntityGraph(attributePaths = {"composites", "composites.parts"})
  List<T> findAllWithCompositesAndCompositesPartsByIdIn(List<UUID> ids);
}
