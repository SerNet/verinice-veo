/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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
package org.veo.core.entity.specification;

import java.util.List;

import org.veo.core.entity.AccountProvider;
import org.veo.core.entity.ClientOwned;
import org.veo.core.entity.code.EntityValidationException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EntityValidator {
    private final AccountProvider accountProvider;

    public void validate(Object entity) throws EntityValidationException {
        List.of(new TypedValidator<>(ClientOwned.class,
                new SameClientSpecification(accountProvider.getCurrentUserAccount()
                                                           .getClient())))
            .forEach(v -> v.validateIfApplicable(entity));
    }

    @RequiredArgsConstructor
    static class TypedValidator<T> {
        private final Class<T> type;
        private final EntitySpecification<T> specification;

        public void validateIfApplicable(Object entity) {
            if (type.isAssignableFrom(entity.getClass())) {
                var castEntity = type.cast(entity);
                if (!specification.test(castEntity)) {
                    throw new EntityValidationException(castEntity, specification);
                }
            }
        }
    }
}
