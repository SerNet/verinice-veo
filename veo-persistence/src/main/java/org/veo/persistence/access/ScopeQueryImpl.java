/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade
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
package org.veo.persistence.access;

import java.util.List;

import org.veo.core.entity.Client;
import org.veo.core.entity.Scope;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.ScopeQuery;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.entity.jpa.ScopeData;

public class ScopeQueryImpl extends ElementQueryImpl<Scope, ScopeData> implements ScopeQuery {

  private final ScopeDataRepository scopeRepository;
  private boolean fetchMembers;

  public ScopeQueryImpl(ScopeDataRepository repo, Client client) {
    super(repo, client);
    this.scopeRepository = repo;
  }

  @Override
  public ElementQuery<Scope> fetchParentsAndChildrenAndSiblings() {
    super.fetchParentsAndChildrenAndSiblings();
    return fetchMembers();
  }

  @Override
  public ScopeQueryImpl fetchMembers() {
    fetchMembers = true;
    return this;
  }

  @Override
  protected List<ScopeData> fullyLoadItems(List<String> ids) {
    List<ScopeData> result = super.fullyLoadItems(ids);
    if (fetchMembers) {
      scopeRepository.findAllWithMembersByDbIdIn(ids);
    }
    if (fetchRisks) {
      scopeRepository.findAllWithRisksByDbIdIn(ids);
    }
    if (fetchRiskValuesAspects) {
      scopeRepository.findAllWithRiskValuesAspectsByDbIdIn(ids);
    }
    return result;
  }
}
