/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
package org.veo.core.usecase;

import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.transaction.Transactional;

/**
 * A base-class for use-cases that require a transaction.
 *
 * @param <I> the inputdata type
 * @param <O> the output data type
 */
public interface TransactionalUseCase<I extends UseCase.InputData, O extends UseCase.OutputData>
    extends UseCase<I, O> {

  @Transactional(Transactional.TxType.REQUIRED)
  @Override
  default <R> R executeAndTransformResult(Supplier<I> inputSupplier, Function<O, R> resultMapper) {

    return resultMapper.apply(execute(inputSupplier.get()));
  }

  @Transactional(Transactional.TxType.REQUIRED)
  @Override
  default <R> R executeAndTransformResult(I input, Function<O, R> resultMapper) {
    return resultMapper.apply(execute(input));
  }

  default Isolation getIsolation() {
    return Isolation.DEFAULT;
  }

  default boolean isReadOnly() {
    return true;
  }

  enum Isolation {
    DEFAULT,
    REPEATABLE_READ,
    SERIALIZABLE;
  }
}
