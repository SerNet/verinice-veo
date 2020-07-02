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
import javax.validation.Valid;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.veo.core.entity.Control;
import org.veo.core.entity.transform.TransformEntityToTargetContext;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetContext;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetTransformer;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityContext;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityTransformer;

@Entity(name = "control")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class ControlData extends EntityLayerSupertypeData {

    /**
     * transform the given entity 'Control' to the corresponding 'ControlData' with
     * the DataEntityToTargetContext.getCompleteTransformationContext().
     */
    public static ControlData from(@Valid Control control) {
        return from(control, DataEntityToTargetContext.getCompleteTransformationContext());
    }

    /**
     * Transform the given data object 'ControlData' to the corresponding 'Control'
     * entity with the DataEntityToTargetContext.getCompleteTransformationContext().
     */
    public Control toControl() {
        return toControl(DataTargetToEntityContext.getCompleteTransformationContext());
    }

    public static ControlData from(@Valid Control control,
            TransformEntityToTargetContext tcontext) {
        if (tcontext instanceof DataEntityToTargetContext) {
            return DataEntityToTargetTransformer.transformControl2Data((DataEntityToTargetContext) tcontext,
                                                                       control);
        }
        throw new IllegalArgumentException("Wrong context type");
    }

    public Control toControl(TransformTargetToEntityContext tcontext) {
        if (tcontext instanceof DataTargetToEntityContext) {
            return DataTargetToEntityTransformer.transformData2Control((DataTargetToEntityContext) tcontext,
                                                                       this);
        }
        throw new IllegalArgumentException("Wrong context type");
    }

}
