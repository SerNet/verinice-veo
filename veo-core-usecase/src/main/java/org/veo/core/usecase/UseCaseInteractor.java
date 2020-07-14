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
package org.veo.core.usecase;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.validation.Valid;

/**
 * All use cases can be executed using this interface.
 *
 * It should be implemented by an adapter that has the capability to map input
 * and output data structures between the use case format and the formats
 * provided by and expected by the caller (i.e. Controller, Presenter, ...)
 *
 * Use case execution is usually an expensive operation, involving long running
 * calculations and database queries. They are handled asynchronously to avoid
 * blocking the caller's thread until the result is ready.
 */
public interface UseCaseInteractor {

    /**
     * This function is used to execute a use case with an asynchronous callback. It
     * gives the use cases the option to control the transaction from the input to
     * the output. This is often needed as we use lazy loading and therefore need to
     * do all in an open session. @see UseCase
     *
     * @param <R>
     *            the type of the result of the function
     * @param <I>
     *            the type of the input expected by the use case
     * @param <O>
     *            the type of the output produced by the use case
     * @param useCase
     *            the actual use case to execute
     * @param inputSupplier
     *            the inputSuplier to provide the expected input for the use case
     * @param outputMapper
     *            a function that will be called asynchronously on successful
     *            termination of the use case.It is called with the result in the
     *            form produced by the use case. The function converts the output
     *            data into a format expected by the caller. The function may use
     *            additional helper classes to do the mapping.
     * @return
     */
    public <R, I extends UseCase.InputData, O extends UseCase.OutputData> CompletableFuture<R> execute(
            UseCase<I, O, R> useCase, Supplier<I> inputSupplier, Function<O, R> outputMapper);

    /**
     * This function is used to execute a use case with an asynchronous callback.
     *
     * @param <R>
     *            the type of the result of the function
     * @param <I>
     *            the type of the input expected by the use case
     * @param <O>
     *            the type of the output produced by the use case
     * @param useCase
     *            the actual use case to execute
     * @param input
     *            the input in the form expected by the use case
     * @param outputMapper
     *            a function that will be called asynchronously on successful
     *            termination of the use case.It is called with the result in the
     *            form produced by the use case. The function converts the output
     *            data into a format expected by the caller. The function may use
     *            additional helper classes to do the mapping.
     * @return
     */
    public <R, I extends UseCase.InputData, O extends UseCase.OutputData> CompletableFuture<R> execute(
            UseCase<I, O, R> useCase, @Valid I input, Function<O, R> outputMapper);

    /**
     * The input data must be validated before it is passed to the use case. The
     * validator provided in the implementation must implement the JSR-380
     * specification to be able to process constraint annotations inside entities.
     *
     * If may implement a different method to ensure the integrity of all entities
     * if it knows about all constraints in some other way (i.e. invariants
     * described in OCL).
     *
     * @param <I>
     *            the input passed to the
     * @param input
     */
    public <I extends UseCase.InputData> void validated(I input);
}
