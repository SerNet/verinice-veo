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
package org.veo.adapter.gateway.interactor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.veo.core.usecase.IUseCaseInteractor;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.InputData;
import org.veo.core.usecase.UseCase.OutputData;

/**
 * Provides a use case interactor with asynchronous callback.
 *
 */
public class UseCaseInteractor implements IUseCaseInteractor {

    @Override
    public <R, I extends InputData, O extends OutputData> CompletableFuture<R> execute(
            UseCase<I, O> useCase, I input, Function<O, R> outputMapper) {

        return CompletableFuture.supplyAsync(() -> input).thenApplyAsync(useCase::execute)
                .thenApplyAsync(outputMapper);
    }

}
