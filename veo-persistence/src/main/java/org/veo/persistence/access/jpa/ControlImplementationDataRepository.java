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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Person;
import org.veo.core.entity.compliance.ControlImplementation;
import org.veo.persistence.entity.jpa.ControlImplementationData;

@Transactional(readOnly = true)
@Repository
public interface ControlImplementationDataRepository
    extends JpaRepository<ControlImplementationData, Long> {

  @Query(
      """
         select distinct ci from control_implementation ci
         where ci.id = ?1
         """)
  Optional<ControlImplementationData> findByUUID(UUID uuid);

  @Query(
      """
          select distinct ci from control_implementation  ci
          join fetch ci.owner
          where ci.control.dbId in ?1
         """)
  Set<ControlImplementation> findByControlIdWithOwner(Set<String> controlIds);

  @Query(
      """
              select distinct ci from control_implementation  ci
              join fetch ci.owner
              join fetch ci.responsible
              where ci.responsible = ?1
             """)
  Set<ControlImplementation> findByPerson(Person responsible);

  @Query(
      """
                  select distinct ci from control_implementation  ci
                  join fetch ci.owner
                  join fetch ci.responsible
                  where ci.responsible in ?1
                 """)
  Set<ControlImplementation> findByPersons(Set<Person> responsibles);

  @Query(
      value =
          """
         select ci.db_id from control_implementation ci
         where ci.requirement_implementations @> CONCAT('"', ?1, '"')::::jsonb
         """,
      nativeQuery = true)
  // NOTES:
  // - In Hibernate the colon is reserved for ":parameters". The escape for ":" is "::". So
  //   the cast "::jsonb" becomes "::::jsonb".
  // - The CONCAT is necessary to make the replacement for ?1 work inside the JSON string.
  Set<Long> findIdsByRequirement(UUID reqImplRef);
}
