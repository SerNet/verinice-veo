/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.core.entity.code;

import java.util.List;

import org.veo.core.entity.DomainException;
import org.veo.core.entity.specification.EntitySpecification;

public class EntityValidationException extends DomainException {

    private static final long serialVersionUID = 2434637292465687030L;
    private List<String> validationErrors;

    public <TEntity> EntityValidationException(TEntity entity,
            EntitySpecification<TEntity> failedSpecification) {
        super(String.format("%s failed on %s", failedSpecification.getClass()
                                                                  .getSimpleName(),
                            entity.toString()));
    }

}
