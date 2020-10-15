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
package org.veo.core.usecase.base;

import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.common.ETagMismatchException;
import org.veo.core.usecase.repository.EntityLayerSupertypeRepository;

import lombok.Value;

public abstract class ModifyEntityUseCase<T extends EntityLayerSupertype, R>
        extends UseCase<ModifyEntityUseCase.InputData<T>, ModifyEntityUseCase.OutputData<T>, R> {

    private final EntityLayerSupertypeRepository<T> repo;

    public ModifyEntityUseCase(EntityLayerSupertypeRepository<T> repo) {
        this.repo = repo;
    }

    @Override
    public OutputData<T> execute(InputData<T> input) {
        T entity = input.getEntity();
        entity.checkSameClient(input.getAuthenticatedClient());
        var storedEntity = repo.findById(input.entity.getId())
                               .orElseThrow();
        checkETag(storedEntity, input);
        entity.version(input.username, storedEntity);
        checkClientBoundaries(input, storedEntity);
        return new OutputData<T>(repo.save(entity));
    }

    private void checkETag(EntityLayerSupertype storedElement,
            InputData<? extends EntityLayerSupertype> input) {
        if (!ETag.matches(storedElement.getId()
                                       .uuidValue(),
                          storedElement.getVersion(), input.getETag())) {
            throw new ETagMismatchException(
                    String.format("The eTag does not match for the element with the ID %s",
                                  storedElement.getId()
                                               .uuidValue()));
        }
    }

    protected void checkClientBoundaries(InputData<? extends EntityLayerSupertype> input,
            EntityLayerSupertype storedEntity) {
        EntityLayerSupertype entity = input.getEntity();
        entity.checkSameClient(storedEntity.getOwner()
                                           .getClient());
        entity.checkSameClient(input.getAuthenticatedClient());
    }

    @Valid
    @Value
    public static class InputData<T> implements UseCase.InputData {

        @Valid
        T entity;

        @Valid
        Client authenticatedClient;

        String eTag;

        String username;
    }

    @Valid
    @Value
    public static class OutputData<T> implements UseCase.OutputData {

        @Valid
        T entity;

    }
}
