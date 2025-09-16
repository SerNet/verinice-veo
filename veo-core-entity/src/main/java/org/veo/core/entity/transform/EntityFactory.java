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
package org.veo.core.entity.transform;

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
import org.veo.core.entity.ElementType;
import org.veo.core.entity.Incident;
import org.veo.core.entity.ItemUpdateType;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.SystemMessage;
import org.veo.core.entity.Unit;
import org.veo.core.entity.UpdateReference;
import org.veo.core.entity.UserConfiguration;
import org.veo.core.entity.definitions.ElementTypeDefinition;

/**
 * @author urszeidler
 */
public interface EntityFactory {
  CustomAspect createCustomAspect(String type, Domain domain);

  Person createPerson(String name, Unit unit);

  Process createProcess(String name, Unit unit);

  Client createClient(UUID id, String name);

  Asset createAsset(String name, Unit unit);

  Control createControl(String name, Unit unit);

  Incident createIncident(String name, Unit unit);

  Scenario createScenario(String name, Unit unit);

  Unit createUnit(String name);

  Document createDocument(String name, Unit parent);

  /**
   * Reconstitutes a domain without the reference to its owning client. Adding it to a client is the
   * caller's responsibility.
   */
  Domain createDomain(String name, String authority, String templateVersion);

  CustomLink createCustomLink(Element linkTarget, Element linkSource, String type, Domain domain);

  Scope createScope(String name, Unit owner);

  DomainTemplate createDomainTemplate(
      String name, String authority, String templateVersion, UUID id);

  CatalogItem createCatalogItem(DomainBase domain);

  UpdateReference createUpdateReference(CatalogItem catalogItem, ItemUpdateType updateType);

  ElementTypeDefinition createElementTypeDefinition(ElementType elementType, DomainBase owner);

  ProfileItem createProfileItem(Profile profile);

  Profile createProfile(DomainBase domainTemplate);

  UserConfiguration createUserConfiguration(Client client, String username, String applicationId);

  SystemMessage createSystemMessage();
}
