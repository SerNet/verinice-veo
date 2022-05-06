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
package org.veo.core.usecase.person;

import java.util.Set;

import org.veo.core.entity.Person;
import org.veo.core.entity.Scope;
import org.veo.core.repository.PersonRepository;
import org.veo.core.repository.ScopeRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.DesignatorService;
import org.veo.core.usecase.base.CreateElementUseCase;
import org.veo.core.usecase.decision.Decider;

public class CreatePersonUseCase extends CreateElementUseCase<Person> {

  public CreatePersonUseCase(
      UnitRepository unitRepository,
      ScopeRepository scopeRepository,
      PersonRepository entityRepo,
      DesignatorService designatorService,
      Decider decider) {
    super(unitRepository, scopeRepository, entityRepo, designatorService, decider);
  }

  @Override
  protected void validate(Person person, Set<Scope> scopes) {
    // GNDN
  }
}
