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
package org.veo.persistence.entity.jpa;

import javax.persistence.Entity;

import org.veo.core.entity.ModelObject;
import org.veo.core.entity.ModelPackage;
import org.veo.core.entity.Process;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity(name = "process")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class ProcessData extends EntityLayerSupertypeData implements Process {

    @Override
    public Class<? extends ModelObject> getModelInterface() {
        return Process.class;
    }

    @Override
    public String getModelType() {
        return ModelPackage.ELEMENT_PROCESS;
    }

}
