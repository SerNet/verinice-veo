/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
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
package org.veo.core.entity;

import org.veo.core.entity.groups.AssetGroup;
import org.veo.core.entity.groups.ControlGroup;
import org.veo.core.entity.groups.DocumentGroup;
import org.veo.core.entity.groups.IncidentGroup;
import org.veo.core.entity.groups.PersonGroup;
import org.veo.core.entity.groups.ProcessGroup;
import org.veo.core.entity.groups.ScenarioGroup;

public enum GroupType {
    Asset(Asset.class, AssetGroup.class), Control(Control.class, ControlGroup.class), Document(
            Document.class, DocumentGroup.class), Incident(Incident.class,
                    IncidentGroup.class), Scenario(Scenario.class, ScenarioGroup.class), Person(
                            Person.class,
                            PersonGroup.class), Process(Process.class, ProcessGroup.class);

    public final Class<? extends EntityLayerSupertype> entityClass;
    public final Class<? extends ModelGroup<?>> groupClass;

    GroupType(Class<? extends EntityLayerSupertype> entityClass,
            Class<? extends ModelGroup<?>> groupClass) {
        this.entityClass = entityClass;
        this.groupClass = groupClass;
    }
}
