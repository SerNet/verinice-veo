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
import org.veo.core.entity.GroupType;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelGroup;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Unit;
import org.veo.core.entity.groups.AssetGroup;
import org.veo.core.entity.groups.ControlGroup;
import org.veo.core.entity.groups.DocumentGroup;
import org.veo.core.entity.groups.PersonGroup;
import org.veo.core.entity.groups.ProcessGroup;

/**
 * @author urszeidler
 */
public interface EntityFactory {
    Asset createAsset();

    Client createClient();

    Control createControl();

    CustomLink createCustomLink();

    CustomProperties createCustomProperties();

    Document createDocument();

    Domain createDomain();

    Person createPerson();

    Process createProcess();

    Unit createUnit();

    Person createPerson(Key<UUID> id, String name, Unit unit);

    Process createProcess(Key<UUID> id, String name, Unit unit);

    Client createClient(Key<UUID> id, String name);

    Asset createAsset(Key<UUID> id, String name, Unit unit);

    Control createControl(Key<UUID> id, String name, Unit unit);

    Unit createUnit(Key<UUID> id, String name, Unit unit);

    Document createDocument(Key<UUID> id, String name, Unit parent);

    Domain createDomain(Key<UUID> id, String name);

    CustomLink createCustomLink(String name, EntityLayerSupertype linkTarget,
            EntityLayerSupertype linkSource);

    CustomProperties createCustomProperties(String type);

    ModelGroup<?> createGroup(GroupType groupType);

    PersonGroup createPersonGroup();

    AssetGroup createAssetGroup();

    ProcessGroup createProcessGroup();

    DocumentGroup createDocumentGroup();

    ControlGroup createControlGroup();
}
