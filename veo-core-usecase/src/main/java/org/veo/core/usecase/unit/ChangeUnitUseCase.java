/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
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
package org.veo.core.usecase.unit;

import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Unit;
import org.veo.core.entity.code.ModelUtils;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.entity.specification.SameClientSpecification;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.UnitRepository;
import org.veo.core.usecase.unit.GetUnitsUseCase.InputData;
import org.veo.core.usecase.unit.GetUnitsUseCase.OutputData;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract superclass for all operations that change an asset. The
 * <code>update()</code> method must be overwritten to make all necessary
 * changes to the asset.
 */
@Slf4j
public abstract class ChangeUnitUseCase<R>
        extends UseCase<ChangeUnitUseCase.InputData, ChangeUnitUseCase.OutputData, R> {

    private final UnitRepository unitRepository;

    public ChangeUnitUseCase(UnitRepository repository) {
        this.unitRepository = repository;
    }

    /**
     * Find a persisted unit object and reinstantiate it. Throws a domain exception
     * if the requested unit object was not found in the repository.
     */
    @Override
    public OutputData execute(InputData input) {
        log.info("Updating unit with id {}", input.getChangedUnit()
                                                  .getId()
                                                  .uuidValue());

        return unitRepository.findById(input.getChangedUnit()
                                            .getId())
                             .map(u -> checkSameClient(u, input))
                             .map(u -> update(u, input))
                             .map(u -> save(u, input))
                             .map(this::output)
                             .orElseThrow(() -> new NotFoundException("Unit %s was not found.",
                                     input.getChangedUnit()
                                          .getId()
                                          .uuidValue()));
    }

    protected abstract Unit update(Unit storedUnit, InputData input);

    protected Unit save(Unit unit, InputData input) {
        // Notice: by changing the context here it would be possible to change the view
        // of the entity that is being
        // returned after the save.
        // i.e. to exclude all references and collections:
        // "dataToEntityContext.partialUnit();"
        unit.setClient(input.getClient());
        ModelUtils.incrementVersion(unit);
        return this.unitRepository.save(unit);
    }

    private OutputData output(Unit unit) {
        return new OutputData(unit);
    }

    // TODO VEO-124 this check should always be done implicitly by UnitImpl or
    // ModelValidator. Without this check, it would be possible to overwrite
    // objects from other clients with our own clientID, thereby hijacking these
    // objects!
    private Unit checkSameClient(Unit storedUnit, InputData input) {
        log.info("Comparing clients {} and {}", input.getClient()
                                                     .getId()
                                                     .uuidValue(),
                 storedUnit.getClient()
                           .getId()
                           .uuidValue());
        if (!(new SameClientSpecification<>(
                input.getClient()).isSatisfiedBy(storedUnit.getClient()))) {
            throw new ClientBoundaryViolationException("The client boundary would be "
                    + "violated by the attempted operation on element: " + this.toString()
                    + " from client " + input.getClient()
                                             .toString());
        }
        return storedUnit;
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        Unit changedUnit;
        Client client;
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        Unit unit;

    }
}
