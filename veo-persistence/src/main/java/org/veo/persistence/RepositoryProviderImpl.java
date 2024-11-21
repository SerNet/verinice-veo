/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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
package org.veo.persistence;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.veo.core.entity.Asset;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.Entity;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.RiskRelated;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.SymIdentifiable;
import org.veo.core.entity.Unit;
import org.veo.core.entity.Versioned;
import org.veo.core.entity.ref.IEntityRef;
import org.veo.core.repository.AssetRepository;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.ControlRepository;
import org.veo.core.repository.DocumentRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.repository.ElementRepository;
import org.veo.core.repository.IdentifiableVersionedRepository;
import org.veo.core.repository.IncidentRepository;
import org.veo.core.repository.PersonRepository;
import org.veo.core.repository.ProcessRepository;
import org.veo.core.repository.ProfileItemRepository;
import org.veo.core.repository.ProfileRepository;
import org.veo.core.repository.Repository;
import org.veo.core.repository.RepositoryBase;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.repository.ScenarioRepository;
import org.veo.core.repository.ScopeRepository;
import org.veo.core.repository.SymIdentifiableRepository;
import org.veo.core.repository.UnitRepository;

@Service
public class RepositoryProviderImpl implements RepositoryProvider {

  @Autowired private PersonRepository personRepository;

  @Autowired private AssetRepository assetRepository;

  @Autowired private IncidentRepository incidentRepository;

  @Autowired private ScenarioRepository scenarioRepository;

  @Autowired private ProcessRepository processRepository;

  @Autowired private DocumentRepository documentRepository;

  @Autowired private ControlRepository controlRepository;

  @Autowired private ClientRepository clientRepository;

  @Autowired private DomainRepository domainRepository;
  @Autowired private DomainTemplateRepository domainTemplateRepository;

  @Autowired private UnitRepository unitRepository;

  @Autowired private ScopeRepository scopeRepository;

  @Autowired private CatalogItemRepository catalogItemRepository;
  @Autowired private ProfileItemRepository profileItemRepository;

  @Autowired private ProfileRepository profileRepository;

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Identifiable> Repository<T> getRepositoryFor(Class<T> entityType) {
    if (Element.class.isAssignableFrom(entityType)) {
      return (Repository<T>) getElementRepositoryFor((Class<Element>) entityType);
    }
    if (Client.class.isAssignableFrom(entityType)) {
      return (Repository<T>) clientRepository;
    }
    if (Domain.class.isAssignableFrom(entityType)) {
      return (Repository<T>) domainRepository;
    }
    if (DomainTemplate.class.isAssignableFrom(entityType)) {
      return (Repository<T>) domainTemplateRepository;
    }
    if (Unit.class.isAssignableFrom(entityType)) {
      return (Repository<T>) unitRepository;
    }
    if (Profile.class.isAssignableFrom(entityType)) {
      return (Repository<T>) profileRepository;
    }
    throw new IllegalArgumentException("Unsupported entity type " + entityType);
  }

  @Override
  public <T extends Entity, TRepo extends RepositoryBase<T, TRef>, TRef extends IEntityRef<T>>
      TRepo getRepositoryBaseFor(Class<T> entityType) {
    if (Identifiable.class.isAssignableFrom(entityType)) {
      return (TRepo) getRepositoryFor((Class) entityType);
    }
    if (SymIdentifiable.class.isAssignableFrom((Class<?>) entityType)) {
      return (TRepo) getSymRepositoryFor((Class) entityType);
    }
    throw new IllegalArgumentException("Unsupported entity type " + entityType.getSimpleName());
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Identifiable & Versioned>
      IdentifiableVersionedRepository<T> getVersionedIdentifiableRepositoryFor(
          Class<T> entityType) {
    if (Element.class.isAssignableFrom(entityType)) {
      return (IdentifiableVersionedRepository<T>)
          getElementRepositoryFor((Class<Element>) entityType);
    }
    if (Client.class.isAssignableFrom(entityType)) {
      return (IdentifiableVersionedRepository<T>) clientRepository;
    }
    if (Domain.class.isAssignableFrom(entityType)) {
      return (IdentifiableVersionedRepository<T>) domainRepository;
    }
    if (Unit.class.isAssignableFrom(entityType)) {
      return (IdentifiableVersionedRepository<T>) unitRepository;
    }
    if (Profile.class.isAssignableFrom(entityType)) {
      return (IdentifiableVersionedRepository<T>) profileRepository;
    }
    throw new IllegalArgumentException("Unsupported entity type " + entityType);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Element> ElementRepository<T> getElementRepositoryFor(Class<T> entityType) {
    if (Scope.class.isAssignableFrom(entityType)) {
      return (ElementRepository<T>) scopeRepository;
    }
    if (Person.class.isAssignableFrom(entityType)) {
      return (ElementRepository<T>) personRepository;
    }
    if (Asset.class.isAssignableFrom(entityType)) {
      return (ElementRepository<T>) assetRepository;
    }
    if (Incident.class.isAssignableFrom(entityType)) {
      return (ElementRepository<T>) incidentRepository;
    }
    if (Scenario.class.isAssignableFrom(entityType)) {
      return (ElementRepository<T>) scenarioRepository;
    }
    if (Process.class.isAssignableFrom(entityType)) {
      return (ElementRepository<T>) processRepository;
    }
    if (Document.class.isAssignableFrom(entityType)) {
      return (ElementRepository<T>) documentRepository;
    }
    if (Control.class.isAssignableFrom(entityType)) {
      return (ElementRepository<T>) controlRepository;
    }

    throw new IllegalArgumentException("Unsupported entity type " + entityType);
  }

  @Override
  public Set<ElementRepository<? extends RiskRelated>> getRiskRelatedElementRepos() {
    return Set.of(assetRepository, processRepository, scenarioRepository, scopeRepository);
  }

  @Override
  public <T extends SymIdentifiable<T, TNamespace>, TNamespace extends Identifiable>
      SymIdentifiableRepository<T, TNamespace> getSymRepositoryFor(Class<T> entityType) {
    if (CatalogItem.class.isAssignableFrom(entityType)) {
      return (SymIdentifiableRepository<T, TNamespace>) catalogItemRepository;
    }
    if (ProfileItem.class.isAssignableFrom(entityType)) {
      return (SymIdentifiableRepository<T, TNamespace>) profileItemRepository;
    }
    throw new IllegalArgumentException("Unsupported entity type " + entityType.getSimpleName());
  }
}
