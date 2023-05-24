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
package org.veo.core.usecase.base;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Scope;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.ScopeRepository;
import org.veo.core.usecase.decision.Decider;

public class UpdateScopeInDomainUseCase extends UpdateElementInDomainUseCase<Scope> {

  public UpdateScopeInDomainUseCase(
      DomainRepository domainRepository, ScopeRepository repo, Decider decider) {
    super(domainRepository, repo, decider);
  }

  @Override
  protected void applyChanges(Scope source, Scope target, Domain domain) {
    super.applyChanges(source, target, domain);
    target.setMembers(source.getMembers());
  }
}
