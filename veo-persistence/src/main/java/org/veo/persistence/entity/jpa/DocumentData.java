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

import org.veo.core.entity.Document;
import org.veo.core.entity.transform.TransformEntityToTargetContext;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetContext;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetTransformer;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityContext;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityTransformer;

@Entity(name = "document")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class DocumentData extends EntityLayerSupertypeData {

    /**
     * transform the given entity 'Document' to the corresponding 'DocumentData'
     * with the DataEntityToTargetContext.getCompleteTransformationContext().
     */
    public static DocumentData from(@Valid Document document) {
        return from(document, DataEntityToTargetContext.getCompleteTransformationContext());
    }

    /**
     * Transform the given data object 'DocumentData' to the corresponding
     * 'Document' entity with the
     * DataEntityToTargetContext.getCompleteTransformationContext().
     */
    public Document toDocument() {
        return toDocument(DataTargetToEntityContext.getCompleteTransformationContext());
    }

    public static DocumentData from(@Valid Document document,
            TransformEntityToTargetContext tcontext) {
        if (tcontext instanceof DataEntityToTargetContext) {
            return DataEntityToTargetTransformer.transformDocument2Data((DataEntityToTargetContext) tcontext,
                                                                        document);
        }
        throw new IllegalArgumentException("Wrong context type");
    }

    public Document toDocument(TransformTargetToEntityContext tcontext) {
        if (tcontext instanceof DataTargetToEntityContext) {
            return DataTargetToEntityTransformer.transformData2Document((DataTargetToEntityContext) tcontext,
                                                                        this);
        }
        throw new IllegalArgumentException("Wrong context type");
    }

}
