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
package org.veo.rest.interactor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.validation.Valid;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.InputData;
import org.veo.core.usecase.UseCase.OutputData;
import org.veo.core.usecase.UseCaseInteractor;

/**
 * Provides a use case interactor with asynchronous callback.
 *
 * Input is provided in the format expected by the use case. A mapper can be
 * used to transform the input before this method is called.
 *
 * The output is provided in the format produced by the use case. The mapping
 * function given as the last parameter will be called asynchronously to
 * transform the result and return it to the caller.
 */
@Service
@Validated
public class UseCaseInteractorImpl implements UseCaseInteractor {

    @Override
    public <R, I extends InputData, O extends OutputData> CompletableFuture<R> execute(
            UseCase<I, O> useCase, @Valid I input, // TODO implement test to make sure all marked
                                                   // complex types in fields are validated
            Function<O, R> outputMapper) {
        return CompletableFuture.supplyAsync(() -> input)
                                .thenApplyAsync(useCase::execute)
                                .thenApplyAsync(outputMapper);
    }

    @Override
    /**
     * Validation of the use case input is accomplished using JSR-380 annotations
     * and the validator provided by the spring application context (see above).
     *
     * Therefore this method does not need to be implemented here. Instead of
     * passing "validated(input)" we annotate the method parameter: "@Valid I
     * input".
     */
    public <I extends InputData> void validated(I input) {
        // implementation not required, see JavaDoc
    }

}
