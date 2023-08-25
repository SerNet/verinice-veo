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

import static java.util.stream.Collectors.toSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import org.veo.core.entity.Asset;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.Incident;
import org.veo.core.entity.ItemUpdateType;
import org.veo.core.entity.Key;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.RiskTailoringReference;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.Unit;
import org.veo.core.entity.UpdateReference;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.persistence.entity.jpa.AssetData;
import org.veo.persistence.entity.jpa.CatalogItemData;
import org.veo.persistence.entity.jpa.CatalogTailoringReferenceData;
import org.veo.persistence.entity.jpa.ClientData;
import org.veo.persistence.entity.jpa.ControlData;
import org.veo.persistence.entity.jpa.CustomAspectData;
import org.veo.persistence.entity.jpa.CustomLinkData;
import org.veo.persistence.entity.jpa.DocumentData;
import org.veo.persistence.entity.jpa.DomainData;
import org.veo.persistence.entity.jpa.DomainTemplateData;
import org.veo.persistence.entity.jpa.ElementTypeDefinitionData;
import org.veo.persistence.entity.jpa.IncidentData;
import org.veo.persistence.entity.jpa.LinkTailoringReferenceData;
import org.veo.persistence.entity.jpa.PersonData;
import org.veo.persistence.entity.jpa.ProcessData;
import org.veo.persistence.entity.jpa.ProfileData;
import org.veo.persistence.entity.jpa.ProfileItemData;
import org.veo.persistence.entity.jpa.ProfileLinkTailoringReferenceData;
import org.veo.persistence.entity.jpa.ProfileRiskTailoringReferenceData;
import org.veo.persistence.entity.jpa.ProfileTailoringReferenceData;
import org.veo.persistence.entity.jpa.ScenarioData;
import org.veo.persistence.entity.jpa.ScopeData;
import org.veo.persistence.entity.jpa.UnitData;
import org.veo.persistence.entity.jpa.UpdateReferenceData;

public class EntityDataFactory implements EntityFactory {

  @Override
  public CustomAspect createCustomAspect(String type, Domain domain) {
    return new CustomAspectData(type, new HashMap<>(), domain);
  }

  @Override
  public Person createPerson(String name, Unit unit) {
    Person person = new PersonData();
    setElementData(person, name, unit);
    return person;
  }

  @Override
  public Process createProcess(String name, Unit unit) {
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
  public Asset createAsset(String name, Unit unit) {
    Asset asset = new AssetData();
    setElementData(asset, name, unit);
    return asset;
  }

  @Override
  public Control createControl(String name, Unit unit) {
    Control control = new ControlData();
    setElementData(control, name, unit);
    return control;
  }

  @Override
  public Incident createIncident(String name, Unit unit) {
    Incident incident = new IncidentData();
    setElementData(incident, name, unit);
    return incident;
  }

  @Override
  public Scenario createScenario(String name, Unit unit) {
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
  public Document createDocument(String name, Unit parent) {
    Document document = new DocumentData();
    setElementData(document, name, parent);
    return document;
  }

  @Override
  public Domain createDomain(String name, String authority, String templateVersion) {
    Domain domain = new DomainData();
    domain.setId(Key.newUuid());
    domain.setName(name);
    domain.setAuthority(authority);
    domain.setTemplateVersion(templateVersion);
    initElementTypeDefinitions(domain);
    return domain;
  }

  @Override
  public CustomLink createCustomLink(
      Element linkTarget, Element linkSource, String type, Domain domain) {
    CustomLink link = new CustomLinkData();
    link.setDomain(domain);
    link.setType(type);
    link.setTarget(linkTarget);
    link.setSource(linkSource);
    return link;
  }

  @Override
  public Scope createScope(String name, Unit owner) {
    var group = new ScopeData();
    group.setName(name);
    group.setOwner(owner);
    return group;
  }

  private void setElementData(Element element, String name, Unit unit) {
    element.setName(name);
    element.setOwner(unit);
  }

  @Override
  public DomainTemplate createDomainTemplate(
      String name, String authority, String templateVersion, Key<UUID> id) {
    DomainTemplate domainTemplate = new DomainTemplateData();
    domainTemplate.setId(id);
    domainTemplate.setName(name);
    domainTemplate.setAuthority(authority);
    domainTemplate.setTemplateVersion(templateVersion);
    initElementTypeDefinitions(domainTemplate);

    return domainTemplate;
  }

  @Override
  public CatalogItem createCatalogItem(DomainBase domain) {
    CatalogItem catalogItem = new CatalogItemData();
    catalogItem.setOwner(domain);
    domain.getCatalogItems().add(catalogItem);
    return catalogItem;
  }

  @Override
  public TailoringReference<CatalogItem> createTailoringReference(
      CatalogItem catalogItem, TailoringReferenceType referenceType) {
    CatalogTailoringReferenceData tailoringReference = new CatalogTailoringReferenceData();
    tailoringReference.setOwner(catalogItem);
    tailoringReference.setReferenceType(referenceType);
    catalogItem.getTailoringReferences().add(tailoringReference);
    return tailoringReference;
  }

  @Override
  public LinkTailoringReference<CatalogItem> createLinkTailoringReference(
      CatalogItem catalogItem, TailoringReferenceType referenceType) {
    LinkTailoringReference<CatalogItem> tailoringReference = new LinkTailoringReferenceData();
    tailoringReference.setOwner(catalogItem);
    tailoringReference.setReferenceType(referenceType);
    catalogItem.getTailoringReferences().add(tailoringReference);
    return tailoringReference;
  }

  @Override
  public ElementTypeDefinition createElementTypeDefinition(String elementType, DomainBase owner) {
    var definition = new ElementTypeDefinitionData();
    definition.setElementType(elementType);
    definition.setOwner(owner);
    return definition;
  }

  @Override
  public UpdateReference createUpdateReference(CatalogItem catalogItem, ItemUpdateType updateType) {
    UpdateReferenceData updateReference = new UpdateReferenceData();
    updateReference.setOwner(catalogItem);
    updateReference.setUpdateType(updateType);
    catalogItem.getUpdateReferences().add(updateReference);
    return updateReference;
  }

  private void initElementTypeDefinitions(DomainBase domainTemplate) {
    domainTemplate.setElementTypeDefinitions(
        EntityType.ELEMENT_TYPES.stream()
            .map(t -> createElementTypeDefinition(t.getSingularTerm(), domainTemplate))
            .collect(toSet()));
  }

  @Override
  public ProfileItem createProfileItem(Profile profile) {
    ProfileItemData profileData = new ProfileItemData();
    profileData.setOwner(profile);
    return profileData;
  }

  @Override
  public Profile createProfile(DomainBase domainTemplate) {
    Profile profile = new ProfileData();
    profile.setOwner(domainTemplate);
    return profile;
  }

  @Override
  public TailoringReference<ProfileItem> createTailoringReference(
      ProfileItem profileItem, TailoringReferenceType referenceType) {
    ProfileTailoringReferenceData tailoringReference = new ProfileTailoringReferenceData();
    tailoringReference.setOwner(profileItem);
    tailoringReference.setReferenceType(referenceType);
    profileItem.getTailoringReferences().add(tailoringReference);
    return tailoringReference;
  }

  @Override
  public LinkTailoringReference<ProfileItem> createLinkTailoringReference(
      ProfileItem profileItem, TailoringReferenceType referenceType) {
    LinkTailoringReference<ProfileItem> tailoringReference =
        new ProfileLinkTailoringReferenceData();
    tailoringReference.setOwner(profileItem);
    tailoringReference.setReferenceType(referenceType);
    profileItem.getTailoringReferences().add(tailoringReference);
    return tailoringReference;
  }

  @Override
  public RiskTailoringReference createProfileRiskTailoringreference(ProfileItem profileItem) {
    ProfileRiskTailoringReferenceData riskTailoringReferenceData =
        new ProfileRiskTailoringReferenceData();
    riskTailoringReferenceData.setOwner(profileItem);
    riskTailoringReferenceData.setReferenceType(TailoringReferenceType.RISK);
    profileItem.getTailoringReferences().add(riskTailoringReferenceData);
    return riskTailoringReferenceData;
  }
}
