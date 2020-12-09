/*******************************************************************************
 * Copyright (c) 2019 Urs Zeidler.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.persistence;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.veo.core.entity.Asset;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelGroup;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Unit;
import org.veo.core.usecase.repository.AssetRepository;
import org.veo.core.usecase.repository.ClientRepository;
import org.veo.core.usecase.repository.ControlRepository;
import org.veo.core.usecase.repository.DocumentRepository;
import org.veo.core.usecase.repository.DomainRepository;
import org.veo.core.usecase.repository.EntityGroupRepository;
import org.veo.core.usecase.repository.EntityLayerSupertypeRepository;
import org.veo.core.usecase.repository.IncidentRepository;
import org.veo.core.usecase.repository.PersonRepository;
import org.veo.core.usecase.repository.ProcessRepository;
import org.veo.core.usecase.repository.Repository;
import org.veo.core.usecase.repository.RepositoryProvider;
import org.veo.core.usecase.repository.ScenarioRepository;
import org.veo.core.usecase.repository.UnitRepository;

@Service
public class RepositoryProviderImpl implements RepositoryProvider {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private ScenarioRepository scenarioRepository;

    @Autowired
    private ProcessRepository processRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ControlRepository controlRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private EntityGroupRepository entityGroupRepository;

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ModelObject> Repository<T, Key<UUID>> getRepositoryFor(Class<T> entityType) {
        if (EntityLayerSupertype.class.isAssignableFrom(entityType)) {
            return (Repository<T, Key<UUID>>) getEntityLayerSupertypeRepositoryFor((Class<EntityLayerSupertype>) entityType);
        }
        if (Client.class.isAssignableFrom(entityType)) {
            return (Repository<T, Key<UUID>>) clientRepository;
        }
        if (Domain.class.isAssignableFrom(entityType)) {
            return (Repository<T, Key<UUID>>) domainRepository;
        }
        if (Unit.class.isAssignableFrom(entityType)) {
            return (Repository<T, Key<UUID>>) unitRepository;
        }
        throw new IllegalArgumentException("Unsupported entity type " + entityType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends EntityLayerSupertype> EntityLayerSupertypeRepository<T> getEntityLayerSupertypeRepositoryFor(
            Class<T> entityType) {
        if (ModelGroup.class.isAssignableFrom(entityType)) {
            return (EntityLayerSupertypeRepository<T>) entityGroupRepository;
        }
        if (Person.class.isAssignableFrom(entityType)) {
            return (EntityLayerSupertypeRepository<T>) personRepository;
        }
        if (Asset.class.isAssignableFrom(entityType)) {
            return (EntityLayerSupertypeRepository<T>) assetRepository;
        }
        if (Incident.class.isAssignableFrom(entityType)) {
            return (EntityLayerSupertypeRepository<T>) incidentRepository;
        }
        if (Scenario.class.isAssignableFrom(entityType)) {
            return (EntityLayerSupertypeRepository<T>) scenarioRepository;
        }
        if (Process.class.isAssignableFrom(entityType)) {
            return (EntityLayerSupertypeRepository<T>) processRepository;
        }
        if (Document.class.isAssignableFrom(entityType)) {
            return (EntityLayerSupertypeRepository<T>) documentRepository;
        }
        if (Control.class.isAssignableFrom(entityType)) {
            return (EntityLayerSupertypeRepository<T>) controlRepository;
        }

        throw new IllegalArgumentException("Unsupported entity type " + entityType);
    }

}
