/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan.
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
package org.veo.persistence.access.query;

import java.util.UUID;

import javax.persistence.criteria.JoinType;

import org.springframework.data.jpa.domain.Specification;

import org.veo.core.entity.Client;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Key;
import org.veo.core.repository.CompositeElementQuery;
import org.veo.core.repository.SingleValueQueryCondition;
import org.veo.persistence.access.jpa.AssetDataRepository;
import org.veo.persistence.access.jpa.CompositeEntityDataRepository;
import org.veo.persistence.access.jpa.ControlDataRepository;
import org.veo.persistence.access.jpa.DocumentDataRepository;
import org.veo.persistence.access.jpa.IncidentDataRepository;
import org.veo.persistence.access.jpa.PersonDataRepository;
import org.veo.persistence.access.jpa.ProcessDataRepository;
import org.veo.persistence.access.jpa.ScenarioDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.entity.jpa.ElementData;

/** Implements {@link CompositeElementQuery} using {@link Specification} API. */
class CompositeElementQueryImpl<
        TInterface extends CompositeElement<TInterface>, TDataClass extends ElementData>
    extends ElementQueryImpl<TInterface, TDataClass> implements CompositeElementQuery<TInterface> {
  public CompositeElementQueryImpl(
      CompositeEntityDataRepository<TDataClass> repo,
      AssetDataRepository assetDataRepository,
      ControlDataRepository controlDataRepository,
      DocumentDataRepository documentDataRepository,
      IncidentDataRepository incidentDataRepository,
      PersonDataRepository personDataRepository,
      ProcessDataRepository processDataRepository,
      ScenarioDataRepository scenarioDataRepository,
      ScopeDataRepository scopeDataRepository,
      Client client) {
    super(
        repo,
        assetDataRepository,
        controlDataRepository,
        documentDataRepository,
        incidentDataRepository,
        personDataRepository,
        processDataRepository,
        scenarioDataRepository,
        scopeDataRepository,
        client);
  }

  public void whereCompositesContain(SingleValueQueryCondition<Key<UUID>> condition) {
    mySpec =
        mySpec.and(
            (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                    root.join("composites", JoinType.INNER).get("dbId"),
                    condition.getValue().uuidValue()));
  }
}
