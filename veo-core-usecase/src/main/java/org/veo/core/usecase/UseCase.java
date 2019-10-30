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

/**
 * Superclass for all use cases. Each use case must provide an implementation of
 * input and output data structures.
 *
 * @author akoderman
 *
 * @param <I>
 * @param <O>
 */
public abstract class UseCase<I extends UseCase.InputData, O extends UseCase.OutputData> {

    public abstract O execute(I input);

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
}
