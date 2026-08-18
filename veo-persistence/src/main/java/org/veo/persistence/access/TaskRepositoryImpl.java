/*
 * verinice.veo
 * Copyright (C) 2026  Jonas Jordan
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
 */
package org.veo.persistence.access;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Unit;
import org.veo.core.repository.TaskQuery;
import org.veo.core.repository.TaskRepository;
import org.veo.persistence.access.jpa.PersonDataRepository;
import org.veo.persistence.access.jpa.RequirementImplementationDataRepository;
import org.veo.persistence.access.query.TaskQueryImpl;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TaskRepositoryImpl implements TaskRepository {

  private final PersonDataRepository personDataRepository;
  private final RequirementImplementationDataRepository riRepository;
  private final EntityManager em;

  @Override
  public TaskQuery queryTasks(Domain domain, Unit unit, UserAccessRights accessRights) {
    return new TaskQueryImpl(em, personDataRepository, riRepository, accessRights, domain, unit);
  }
}
