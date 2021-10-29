/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Urs Zeidler.
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

import java.util.HashSet;
import java.util.UUID;
import java.util.function.Function;

import org.veo.core.entity.Asset;
import org.veo.core.entity.Catalog;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementOwner;
import org.veo.core.entity.Incident;
import org.veo.core.entity.ItemUpdateType;
import org.veo.core.entity.Key;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.Unit;
import org.veo.core.entity.UpdateReference;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.persistence.entity.jpa.AssetData;
import org.veo.persistence.entity.jpa.CatalogData;
import org.veo.persistence.entity.jpa.CatalogItemData;
import org.veo.persistence.entity.jpa.ClientData;
import org.veo.persistence.entity.jpa.ControlData;
import org.veo.persistence.entity.jpa.CustomAspectData;
import org.veo.persistence.entity.jpa.CustomLinkData;
import org.veo.persistence.entity.jpa.DocumentData;
import org.veo.persistence.entity.jpa.DomainData;
import org.veo.persistence.entity.jpa.DomainTemplateData;
import org.veo.persistence.entity.jpa.IncidentData;
import org.veo.persistence.entity.jpa.LinkTailoringReferenceData;
import org.veo.persistence.entity.jpa.PersonData;
import org.veo.persistence.entity.jpa.ProcessData;
import org.veo.persistence.entity.jpa.ScenarioData;
import org.veo.persistence.entity.jpa.ScopeData;
import org.veo.persistence.entity.jpa.TailoringReferenceData;
import org.veo.persistence.entity.jpa.UnitData;
import org.veo.persistence.entity.jpa.UpdateReferenceData;

public class EntityDataFactory implements EntityFactory {

    @Override
    public CustomAspect createCustomAspect(String type) {
        var aspect = new CustomAspectData();
        aspect.setType(type);
        return aspect;
    }

    @Override
    public Person createPerson(String name, ElementOwner unit) {
        Person person = new PersonData();
        setElementData(person, name, unit);
        return person;
    }

    @Override
    public Process createProcess(String name, ElementOwner unit) {
        Process process = new ProcessData();
        setElementData(process, name, unit);
        return process;
    }

    @Override
    public Client createClient(Key<UUID> id, String name) {
        Client client = new ClientData();
        client.setId(id);
        client.setName(name);
        client.setDomains(new HashSet<>());
        return client;
    }

    @Override
    public Asset createAsset(String name, ElementOwner unit) {
        Asset asset = new AssetData();
        setElementData(asset, name, unit);
        return asset;
    }

    @Override
    public Control createControl(String name, ElementOwner unit) {
        Control control = new ControlData();
        setElementData(control, name, unit);
        return control;
    }

    @Override
    public Incident createIncident(String name, ElementOwner unit) {
        Incident incident = new IncidentData();
        setElementData(incident, name, unit);
        return incident;
    }

    @Override
    public Scenario createScenario(String name, ElementOwner unit) {
        Scenario scenario = new ScenarioData();
        setElementData(scenario, name, unit);
        return scenario;
    }

    @Override
    public Unit createUnit(String name, Unit parent) {
        Unit unit = new UnitData();
        unit.setName(name);
        unit.setParent(parent);
        if (parent != null) {
            unit.setClient(parent.getClient());
        }
        unit.setDomains(new HashSet<>());
        return unit;
    }

    @Override
    public Document createDocument(String name, ElementOwner parent) {
        Document document = new DocumentData();
        setElementData(document, name, parent);
        return document;
    }

    @Override
    public Domain createDomain(String name, String authority, String templateVersion,
            String revision) {
        Domain domain = new DomainData();
        domain.setId(Key.newUuid());
        domain.setName(name);
        domain.setAuthority(authority);
        domain.setTemplateVersion(templateVersion);
        domain.setRevision(revision);

        return domain;
    }

    @Override
    public CustomLink createCustomLink(Element linkTarget, Element linkSource, String type) {
        CustomLink link = new CustomLinkData();
        link.setType(type);
        link.setTarget(linkTarget);
        link.setSource(linkSource);
        return link;
    }

    @Override
    public Scope createScope(String name, ElementOwner owner) {
        var group = new ScopeData();
        group.setName(name);
        group.setOwnerOrContainingCatalogItem(owner);
        return group;
    }

    private void setElementData(Element element, String name, ElementOwner unit) {
        element.setName(name);
        element.setOwnerOrContainingCatalogItem(unit);
    }

    @Override
    public Catalog createCatalog(DomainTemplate owner) {
        Catalog catalog = new CatalogData();
        catalog.setDomainTemplate(owner);
        owner.addToCatalogs(catalog);
        return catalog;
    }

    @Override
    public DomainTemplate createDomainTemplate(String name, String authority,
            String templateVersion, String revision, Key<UUID> id) {
        DomainTemplate domainTemplate = new DomainTemplateData();
        domainTemplate.setId(id);
        domainTemplate.setName(name);
        domainTemplate.setAuthority(authority);
        domainTemplate.setTemplateVersion(templateVersion);
        domainTemplate.setRevision(revision);

        return domainTemplate;
    }

    @Override
    public CatalogItem createCatalogItem(Catalog catalog,
            Function<CatalogItem, Element> elementFactory) {
        CatalogItem catalogItem = new CatalogItemData();
        catalogItem.setCatalog(catalog);
        Element element = elementFactory.apply(catalogItem);
        element.setContainingCatalogItem(catalogItem);
        catalogItem.setElement(element);
        catalog.getCatalogItems()
               .add(catalogItem);
        return catalogItem;
    }

    @Override
    public TailoringReference createTailoringReference(CatalogItem catalogItem,
            TailoringReferenceType referenceType) {
        TailoringReferenceData tailoringReference = new TailoringReferenceData();
        tailoringReference.setOwner(catalogItem);
        tailoringReference.setReferenceType(referenceType);
        catalogItem.getTailoringReferences()
                   .add(tailoringReference);
        return tailoringReference;
    }

    @Override
    public LinkTailoringReference createLinkTailoringReference(CatalogItem catalogItem,
            TailoringReferenceType referenceType) {
        LinkTailoringReference tailoringReference = new LinkTailoringReferenceData();
        tailoringReference.setOwner(catalogItem);
        tailoringReference.setReferenceType(referenceType);
        catalogItem.getTailoringReferences()
                   .add(tailoringReference);
        return tailoringReference;
    }

    @Override
    public UpdateReference createUpdateReference(CatalogItem catalogItem,
            ItemUpdateType updateType) {
        UpdateReferenceData updateReference = new UpdateReferenceData();
        updateReference.setOwner(catalogItem);
        updateReference.setUpdateType(updateType);
        catalogItem.getUpdateReferences()
                   .add(updateReference);
        return updateReference;
    }
}
