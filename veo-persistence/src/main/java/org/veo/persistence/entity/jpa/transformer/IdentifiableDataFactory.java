/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.persistence.entity.jpa.transformer;

import java.util.UUID;

import org.veo.core.entity.Asset;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Key;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.Unit;
import org.veo.core.entity.UpdateReference;
import org.veo.core.entity.transform.IdentifiableFactory;
import org.veo.persistence.entity.jpa.AssetData;
import org.veo.persistence.entity.jpa.CatalogItemData;
import org.veo.persistence.entity.jpa.CatalogTailoringReferenceData;
import org.veo.persistence.entity.jpa.ClientData;
import org.veo.persistence.entity.jpa.ControlData;
import org.veo.persistence.entity.jpa.DocumentData;
import org.veo.persistence.entity.jpa.DomainData;
import org.veo.persistence.entity.jpa.DomainTemplateData;
import org.veo.persistence.entity.jpa.IncidentData;
import org.veo.persistence.entity.jpa.LinkTailoringReferenceData;
import org.veo.persistence.entity.jpa.PersonData;
import org.veo.persistence.entity.jpa.ProcessData;
import org.veo.persistence.entity.jpa.ScenarioData;
import org.veo.persistence.entity.jpa.ScopeData;
import org.veo.persistence.entity.jpa.UnitData;
import org.veo.persistence.entity.jpa.UpdateReferenceData;

public class IdentifiableDataFactory implements IdentifiableFactory {
  @Override
  public <T extends Identifiable> T create(Class<T> type, Key<UUID> id) {
    var entity = create(type);
    entity.setId(id);
    return entity;
  }

  private <T extends Identifiable> T create(Class<T> type) {
    if (type == Person.class) {
      return (T) new PersonData();
    }
    if (type == Process.class) {
      return (T) new ProcessData();
    }
    if (type == Client.class) {
      return (T) new ClientData();
    }
    if (type == Asset.class) {
      return (T) new AssetData();
    }
    if (type == Control.class) {
      return (T) new ControlData();
    }
    if (type == Incident.class) {
      return (T) new IncidentData();
    }
    if (type == Scenario.class) {
      return (T) new ScenarioData();
    }
    if (type == Unit.class) {
      return (T) new UnitData();
    }
    if (type == Document.class) {
      return (T) new DocumentData();
    }
    if (type == Domain.class) {
      return (T) new DomainData();
    }
    if (type == Scope.class) {
      return (T) new ScopeData();
    }
    if (type == DomainTemplate.class) {
      return (T) new DomainTemplateData();
    }
    if (type == CatalogItem.class) {
      return (T) new CatalogItemData();
    }
    if (type == TailoringReference.class) {
      return (T) new CatalogTailoringReferenceData();
    }
    if (type == UpdateReference.class) {
      return (T) new UpdateReferenceData();
    }
    if (type == LinkTailoringReference.class) {
      return (T) new LinkTailoringReferenceData();
    }
    throw new UnsupportedOperationException("Unsupported type " + type);
  }
}
