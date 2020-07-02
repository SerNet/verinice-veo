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

import org.veo.core.entity.Person;
import org.veo.core.entity.transform.TransformEntityToTargetContext;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetContext;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetTransformer;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityContext;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityTransformer;

@Entity(name = "person")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class PersonData extends EntityLayerSupertypeData {

    /**
     * transform the given entity 'Person' to the corresponding 'PersonData' with
     * the DataEntityToTargetContext.getCompleteTransformationContext().
     */
    public static PersonData from(@Valid Person person) {
        return from(person, DataEntityToTargetContext.getCompleteTransformationContext());
    }

    /**
     * Transform the given data object 'PersonData' to the corresponding 'Person'
     * entity with the DataEntityToTargetContext.getCompleteTransformationContext().
     */
    public Person toPerson() {
        return toPerson(DataTargetToEntityContext.getCompleteTransformationContext());
    }

    public static PersonData from(@Valid Person person, TransformEntityToTargetContext tcontext) {
        if (tcontext instanceof DataEntityToTargetContext) {
            return DataEntityToTargetTransformer.transformPerson2Data((DataEntityToTargetContext) tcontext,
                                                                      person);
        }
        throw new IllegalArgumentException("Wrong context type");
    }

    public Person toPerson(TransformTargetToEntityContext tcontext) {
        if (tcontext instanceof DataTargetToEntityContext) {
            return DataTargetToEntityTransformer.transformData2Person((DataTargetToEntityContext) tcontext,
                                                                      this);
        }
        throw new IllegalArgumentException("Wrong context type");
    }

}
