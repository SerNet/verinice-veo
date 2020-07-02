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
package org.veo.persistence;

import org.springframework.stereotype.Service;

import org.veo.core.entity.transform.TransformContextProvider;
import org.veo.core.entity.transform.TransformEntityToTargetContext;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetContext;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityContext;

@Service
public class TransformContextProviderImpl implements TransformContextProvider {

    @Override
    public TransformEntityToTargetContext createEntityToTargetContext() {
        return DataEntityToTargetContext.getCompleteTransformationContext();
    }

    @Override
    public TransformTargetToEntityContext createTargetToEntityContext() {
        return DataTargetToEntityContext.getCompleteTransformationContext();
    }

}
