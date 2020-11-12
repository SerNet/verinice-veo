/*******************************************************************************
 * Copyright (c) 2020 Urs Zeidler.
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
package org.veo.persistence.entity.jpa.transformer;

import java.util.HashSet;
import java.util.UUID;

import org.veo.core.entity.Asset;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.CustomProperties;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.GroupType;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelGroup;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Unit;
import org.veo.core.entity.groups.AssetGroup;
import org.veo.core.entity.groups.ControlGroup;
import org.veo.core.entity.groups.DocumentGroup;
import org.veo.core.entity.groups.IncidentGroup;
import org.veo.core.entity.groups.PersonGroup;
import org.veo.core.entity.groups.ProcessGroup;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.persistence.entity.jpa.AssetData;
import org.veo.persistence.entity.jpa.ClientData;
import org.veo.persistence.entity.jpa.ControlData;
import org.veo.persistence.entity.jpa.CustomLinkData;
import org.veo.persistence.entity.jpa.CustomPropertiesData;
import org.veo.persistence.entity.jpa.DocumentData;
import org.veo.persistence.entity.jpa.DomainData;
import org.veo.persistence.entity.jpa.IncidentData;
import org.veo.persistence.entity.jpa.PersonData;
import org.veo.persistence.entity.jpa.ProcessData;
import org.veo.persistence.entity.jpa.UnitData;
import org.veo.persistence.entity.jpa.groups.AssetGroupData;
import org.veo.persistence.entity.jpa.groups.ControlGroupData;
import org.veo.persistence.entity.jpa.groups.DocumentGroupData;
import org.veo.persistence.entity.jpa.groups.IncidentGroupData;
import org.veo.persistence.entity.jpa.groups.PersonGroupData;
import org.veo.persistence.entity.jpa.groups.ProcessGroupData;

/**
 * @author urszeidler
 */
public class EntityDataFactory implements EntityFactory {

    @Override
    public CustomProperties createCustomProperties() {
        return new CustomPropertiesData();
    }

    @Override
    public Person createPerson(Key<UUID> id, String name, Unit unit) {
        Person person = new PersonData();
        setEntityLayerData(person, id, name, unit);
        return person;
    }

    @Override
    public Process createProcess(Key<UUID> id, String name, Unit unit) {
        Process process = new ProcessData();
        setEntityLayerData(process, id, name, unit);
        return process;
    }

    @Override
    public Client createClient(Key<UUID> id, String name) {
        Client client = new ClientData();
        client.setId(id);
        client.setName(name);
        client.setDomains(new HashSet<Domain>());
        return client;
    }

    @Override
    public Asset createAsset(Key<UUID> id, String name, Unit unit) {
        Asset asset = new AssetData();
        setEntityLayerData(asset, id, name, unit);
        return asset;
    }

    @Override
    public Control createControl(Key<UUID> id, String name, Unit unit) {
        Control control = new ControlData();
        setEntityLayerData(control, id, name, unit);
        return control;
    }

    @Override
    public Incident createIncident(Key<UUID> id, String name, Unit unit) {
        Incident incident = new IncidentData();
        setEntityLayerData(incident, id, name, unit);
        return incident;
    }

    @Override
    public Unit createUnit(Key<UUID> id, String name, Unit parent) {
        Unit unit = new UnitData();
        unit.setId(id);
        unit.setName(name);
        unit.setParent(parent);
        if (parent != null) {
            unit.setClient(parent.getClient());
        }
        unit.setDomains(new HashSet<Domain>());
        return unit;
    }

    @Override
    public Document createDocument(Key<UUID> id, String name, Unit parent) {
        Document document = new DocumentData();
        setEntityLayerData(document, id, name, parent);
        return document;
    }

    @Override
    public Domain createDomain(Key<UUID> id, String name) {
        Domain domain = new DomainData();
        domain.setId(id);
        domain.setName(name);
        return domain;
    }

    @Override
    public CustomLink createCustomLink(String name, EntityLayerSupertype linkTarget,
            EntityLayerSupertype linkSource) {
        CustomLink link = new CustomLinkData();
        link.setName(name);
        link.setTarget(linkTarget);
        link.setSource(linkSource);
        return link;
    }

    @Override
    public ModelGroup<?> createGroup(GroupType groupType, Key<UUID> key, String name, Unit unit) {
        return createGroupInstance(groupType.entityClass, key, name, unit);
    }

    @Override
    public PersonGroup createPersonGroup(Key<UUID> key, String name, Unit owner) {
        var group = new PersonGroupData();
        group.setId(key);
        group.setName(name);
        group.setOwner(owner);
        return group;
    }

    @Override
    public AssetGroup createAssetGroup(Key<UUID> key, String name, Unit owner) {
        var group = new AssetGroupData();
        group.setId(key);
        group.setName(name);
        group.setOwner(owner);
        return group;
    }

    @Override
    public ProcessGroup createProcessGroup(Key<UUID> key, String name, Unit owner) {
        var group = new ProcessGroupData();
        group.setId(key);
        group.setName(name);
        group.setOwner(owner);
        return group;
    }

    @Override
    public DocumentGroup createDocumentGroup(Key<UUID> key, String name, Unit owner) {
        var group = new DocumentGroupData();
        group.setId(key);
        group.setName(name);
        group.setOwner(owner);
        return group;
    }

    @Override
    public ControlGroup createControlGroup(Key<UUID> key, String name, Unit owner) {
        var group = new ControlGroupData();
        group.setId(key);
        group.setName(name);
        group.setOwner(owner);
        return group;
    }

    @Override
    public IncidentGroup createIncidentGroup(Key<UUID> key, String name, Unit owner) {
        var group = new IncidentGroupData();
        group.setId(key);
        group.setName(name);
        group.setOwner(owner);
        return group;
    }

    private void initCollection(EntityLayerSupertype object) {
        object.setDomains(new HashSet<Domain>());
        object.setLinks(new HashSet<CustomLink>());
        object.setCustomAspects(new HashSet<CustomProperties>());
    }

    private ModelGroup<?> createGroupInstance(Class<? extends ModelObject> entityClass,
            Key<UUID> key, String name, Unit unit) {
        if (entityClass == Person.class)// TODO: check does these come from the same classloader?
            return createPersonGroup(key, name, unit);
        if (entityClass == Document.class)
            return createDocumentGroup(key, name, unit);
        if (entityClass == Asset.class)
            return createAssetGroup(key, name, unit);
        if (entityClass == Process.class)
            return createProcessGroup(key, name, unit);
        if (entityClass == Control.class)
            return createControlGroup(key, name, unit);
        throw new IllegalArgumentException("No such Group for: " + entityClass);
    }

    private void setEntityLayerData(EntityLayerSupertype entityLayerSupertype, Key<UUID> newUuid,
            String name, Unit unit) {
        entityLayerSupertype.setId(newUuid);
        entityLayerSupertype.setName(name);
        entityLayerSupertype.setOwner(unit);
    }

}
