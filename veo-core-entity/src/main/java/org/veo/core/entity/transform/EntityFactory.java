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
package org.veo.core.entity.transform;

import java.util.UUID;

import org.veo.core.entity.Asset;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.CustomProperties;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Key;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.Unit;

/**
 * @author urszeidler
 */
public interface EntityFactory {
    CustomProperties createCustomProperties();

    Person createPerson(String name, Unit unit);

    Process createProcess(String name, Unit unit);

    Client createClient(Key<UUID> id, String name);

    Asset createAsset(String name, Unit unit);

    Control createControl(String name, Unit unit);

    Incident createIncident(String name, Unit unit);

    Scenario createScenario(String name, Unit unit);

    Unit createUnit(String name, Unit unit);

    Document createDocument(String name, Unit parent);

    /**
     * Reconstitutes a domain without the reference to its owning client. Adding it
     * to a client is the caller's responsibility.
     */
    Domain createDomain(String name);

    CustomLink createCustomLink(String name, EntityLayerSupertype linkTarget,
            EntityLayerSupertype linkSource);

    Scope createScope(String name, Unit owner);
}
