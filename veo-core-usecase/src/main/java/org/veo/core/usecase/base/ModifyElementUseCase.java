/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.core.usecase.base;

import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Element;
import org.veo.core.repository.ElementRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.common.ETagMismatchException;

import lombok.Value;

public abstract class ModifyElementUseCase<T extends Element> implements
        TransactionalUseCase<ModifyElementUseCase.InputData<T>, ModifyElementUseCase.OutputData<T>> {

    private final ElementRepository<T> repo;

    public ModifyElementUseCase(ElementRepository<T> repo) {
        this.repo = repo;
    }

    @Override
    public OutputData<T> execute(InputData<T> input) {
        T entity = input.getElement();
        entity.checkSameClient(input.getAuthenticatedClient());
        var storedEntity = repo.findById(input.element.getId())
                               .orElseThrow();
        checkETag(storedEntity, input);
        entity.version(input.username, storedEntity);
        checkClientBoundaries(input, storedEntity);
        checkSubTypeChange(entity, storedEntity);
        // The designator is read-only so it must stay the same.
        entity.setDesignator(storedEntity.getDesignator());
        DomainSensitiveElementValidator.validate(entity);
        return new OutputData<T>(repo.save(entity));
    }

    private void checkSubTypeChange(T newElement, T oldElement) {
        oldElement.getDomains()
                  .forEach(domain -> {
                      oldElement.getSubType(domain)
                                .ifPresent(oldSubType -> {
                                    var newSubType = newElement.getSubType(domain)
                                                               .orElseThrow((() -> new IllegalArgumentException(
                                                                       "Cannot remove a sub type from an existing element")));
                                    if (!newSubType.equals(oldSubType)) {
                                        throw new IllegalArgumentException(
                                                "Cannot change a sub type on an existing element");
                                    }
                                });
                  });
    }

    private void checkETag(Element storedElement, InputData<? extends Element> input) {
        if (!ETag.matches(storedElement.getId()
                                       .uuidValue(),
                          storedElement.getVersion(), input.getETag())) {
            throw new ETagMismatchException(
                    String.format("The eTag does not match for the element with the ID %s",
                                  storedElement.getId()
                                               .uuidValue()));
        }
    }

    protected void checkClientBoundaries(InputData<? extends Element> input, Element storedEntity) {
        Element entity = input.getElement();
        entity.checkSameClient(storedEntity.getOwner()
                                           .getClient());
        entity.checkSameClient(input.getAuthenticatedClient());
    }

    @Valid
    @Value
    public static class InputData<T> implements UseCase.InputData {

        @Valid
        T element;

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
