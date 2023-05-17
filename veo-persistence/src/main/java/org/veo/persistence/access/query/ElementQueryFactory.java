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
package org.veo.persistence.access.query;

import org.springframework.stereotype.Component;

import org.veo.core.entity.Asset;
import org.veo.core.entity.Client;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Control;
import org.veo.core.entity.Document;
import org.veo.core.entity.Element;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.repository.CompositeElementQuery;
import org.veo.core.repository.ElementQuery;
import org.veo.persistence.access.jpa.AssetDataRepository;
import org.veo.persistence.access.jpa.CompositeEntityDataRepository;
import org.veo.persistence.access.jpa.ControlDataRepository;
import org.veo.persistence.access.jpa.DocumentDataRepository;
import org.veo.persistence.access.jpa.ElementDataRepository;
import org.veo.persistence.access.jpa.IncidentDataRepository;
import org.veo.persistence.access.jpa.PersonDataRepository;
import org.veo.persistence.access.jpa.ProcessDataRepository;
import org.veo.persistence.access.jpa.ScenarioDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.entity.jpa.ElementData;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ElementQueryFactory {
  private final ElementDataRepository<ElementData> elementRepository;
  private final AssetDataRepository assetDataRepository;
  private final ControlDataRepository controlDataRepository;
  private final DocumentDataRepository documentDataRepository;
  private final IncidentDataRepository incidentDataRepository;
  private final PersonDataRepository personDataRepository;
  private final ProcessDataRepository processDataRepository;
  private final ScenarioDataRepository scenarioDataRepository;
  private final ScopeDataRepository scopeDataRepository;

  public ElementQuery<Element> queryElements(Client client) {
    return query(client, elementRepository);
  }

  public CompositeElementQuery<Asset> queryAssets(Client client) {
    return query(client, assetDataRepository);
  }

  public CompositeElementQuery<Control> queryControls(Client client) {
    return query(client, controlDataRepository);
  }

  public CompositeElementQuery<Document> queryDocuments(Client client) {
    return query(client, documentDataRepository);
  }

  public CompositeElementQuery<Incident> queryIncidents(Client client) {
    return query(client, incidentDataRepository);
  }

  public CompositeElementQuery<Person> queryPersons(Client client) {
    return query(client, personDataRepository);
  }

  public CompositeElementQuery<Process> queryProcesses(Client client) {
    return query(client, processDataRepository);
  }

  public CompositeElementQuery<Scenario> queryScenarios(Client client) {
    return query(client, scenarioDataRepository);
  }

  public ElementQuery<Scope> queryScopes(Client client) {
    return query(client, scopeDataRepository);
  }

  private <TElement extends Element, TData extends ElementData>
      ElementQueryImpl<TElement, TData> query(Client client, ElementDataRepository<TData> repo) {
    return new ElementQueryImpl<>(
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

  private <TElement extends CompositeElement<TElement>, TData extends ElementData>
      CompositeElementQueryImpl<TElement, TData> query(
          Client client, CompositeEntityDataRepository<TData> repo) {
    return new CompositeElementQueryImpl<>(
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
}
