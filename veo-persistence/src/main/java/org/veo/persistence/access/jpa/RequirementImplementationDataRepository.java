/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Alexander Koderman
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

import java.util.Set;
import java.util.UUID;

import jakarta.annotation.Nonnull;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Person;
import org.veo.persistence.entity.jpa.RequirementImplementationData;

@Transactional(readOnly = true)
public interface RequirementImplementationDataRepository
    extends JpaRepository<RequirementImplementationData, Long> {

  @Query(
      """
          select distinct ri from requirement_implementation ri
          where ri.id in (:uuids)
         """)
  Set<RequirementImplementationData> findAllByUUID(@Param("uuids") Set<UUID> uuids);

  @Query(
      """
                      select distinct ri from requirement_implementation ri
                      join fetch ri.control
                      where ri.dbId in ?1
                     """)
  Set<RequirementImplementationData> findAllByDbIdsWithControls(Iterable<Long> uuids);

  @Query(
      """
                      select distinct ri from requirement_implementation ri
                      join fetch ri.origin
                      join fetch ri.responsible
                      where ri.responsible = ?1
                     """)
  Set<RequirementImplementationData> findByPerson(Person responsible);

  @Query(
      """
                      select distinct ri from requirement_implementation ri
                      join fetch ri.origin
                      join fetch ri.responsible
                      where ri.responsible in ?1
                     """)
  Set<RequirementImplementationData> findByPersons(Set<Person> responsible);

  @Nonnull
  @Transactional(readOnly = true)
  Page<RequirementImplementationData> findAll(
      Specification<RequirementImplementationData> specification, Pageable pageable);

  @Query(
      """
         select distinct ri from requirement_implementation  ri
         where ri.control.dbId in ?1
         """)
  Set<RequirementImplementationData> findAllByControlIds(Set<String> controlIDs);
}
