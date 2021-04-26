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

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;

import lombok.Value;

/**
 * Superclass for all use cases. Each use case must provide an implementation of
 * input and output data structures.
 *
 * @author akoderman
 *
 * @param <I>
 *            the inputdata type
 * @param <O>
 *            the output data type
 */
public interface UseCase<I extends UseCase.InputData, O extends UseCase.OutputData> {

    O execute(I input);

    /**
     * Uses the inputSupplier to get the Input, execute the usecase with the input,
     * use the result resultMapper to produce a R.
     *
     * Override this Method and annotate it with @Transactional in the concrete
     * usescase when you need to transform the input and/or the output.
     */
    default <R> R executeAndTransformResult(Supplier<I> inputSupplier,
            Function<O, R> resultMapper) {
        return resultMapper.apply(execute(inputSupplier.get()));
    }

    /**
     * Execute the usecase with the input, use the result resultMapper to produce a
     * R.
     *
     * Override this Method and annotate it with @Transactional in the concrete use
     * case when you need to transform only the output.
     */
    default <R> R executeAndTransformResult(I input, Function<O, R> resultMapper) {
        return resultMapper.apply(execute(input));
    }

    /**
     * The input data structure that is particular to this use case.
     *
     * InputData should be an immutable value object.
     */
    public interface InputData {

    }

    /**
     * The output data structure that is particular to this use case.
     *
     * OutputData should be an immutable value object.
     */
    public interface OutputData {

    }

    public static final class EmptyOutput implements OutputData {

        public static final EmptyOutput INSTANCE = new EmptyOutput();

        private EmptyOutput() {

        }
    }

    @Valid
    @Value
    public static class IdAndClient implements UseCase.InputData {
        Key<UUID> id;
        Client authenticatedClient;
    }

}