/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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
package org.veo.rest.interactor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.validation.Valid;

import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.BackOffPolicyBuilder;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

import org.veo.core.usecase.RetryableUseCase;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.InputData;
import org.veo.core.usecase.UseCase.OutputData;
import org.veo.core.usecase.UseCaseInteractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides a use case interactor with asynchronous callback.
 *
 * <p>Input is provided in the format expected by the use case. A mapper can be used to transform
 * the input before this method is called.
 *
 * <p>The output is provided in the format produced by the use case. The mapping function given as
 * the last parameter will be called asynchronously to transform the result and return it to the
 * caller.
 */
@Service
@Validated
@Slf4j
@RequiredArgsConstructor
public class UseCaseInteractorImpl implements UseCaseInteractor {

  private final PlatformTransactionManager transactionManager;

  @Override
  @Async
  public <R, I extends InputData, O extends OutputData> CompletableFuture<R> execute(
      UseCase<I, O> useCase, Supplier<I> inputSupplier, Function<O, R> outputMapper) {
    log.info("Executing {} with {}", useCase, inputSupplier);
    return doExecuteWithRetry(
        useCase, () -> useCase.executeAndTransformResult(inputSupplier, outputMapper));
  }

  @Override
  @Async
  public <R, I extends InputData, O extends OutputData> CompletableFuture<R> execute(
      UseCase<I, O> useCase,
      @Valid I input, // TODO implement test to make sure all marked
      // complex types in fields are validated
      Function<O, R> outputMapper) {

    log.info("Executing {}", useCase);
    log.debug("Input: {}", input);
    return doExecuteWithRetry(
        useCase, () -> useCase.executeAndTransformResult(input, outputMapper));
  }

  private <R, I extends InputData, O extends OutputData> CompletableFuture<R> doExecuteWithRetry(
      UseCase<I, O> useCase, Supplier<R> resultSupplier) {

    if (useCase instanceof RetryableUseCase r) {
      RetryTemplate retryTemplate = new RetryTemplate();

      SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(r.getMaxAttempts());
      retryTemplate.setRetryPolicy(retryPolicy);

      BackOffPolicy backOffPolicy =
          BackOffPolicyBuilder.newBuilder().delay(30l).maxDelay(500l).build();
      retryTemplate.setBackOffPolicy(backOffPolicy);

      return retryTemplate.execute(ctx -> doExecuteWithIsolation(useCase, resultSupplier));
    }
    return doExecuteWithIsolation(useCase, resultSupplier);
  }

  private <R, I extends InputData, O extends OutputData>
      CompletableFuture<R> doExecuteWithIsolation(
          UseCase<I, O> useCase, Supplier<R> resultSupplier) {
    if (useCase instanceof TransactionalUseCase<?, ?> t) {
      TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
      transactionTemplate.setReadOnly(t.isReadOnly());
      TransactionalUseCase.Isolation isolation = t.getIsolation();
      switch (isolation) {
        case DEFAULT:
          transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_DEFAULT);
          break;
        case REPEATABLE_READ:
          transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
          break;
        case SERIALIZABLE:
          transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
          break;
        default:
          throw new IllegalStateException("Unhandled isolation level: " + isolation);
      }
      return CompletableFuture.completedFuture(
          transactionTemplate.execute(status -> resultSupplier.get()));
    }
    return CompletableFuture.completedFuture(resultSupplier.get());
  }

  @Override
  /**
   * Validation of the use case input is accomplished using JSR-380 annotations and the validator
   * provided by the spring application context (see above).
   *
   * <p>Therefore this method does not need to be implemented here. Instead of passing
   * "validated(input)" we annotate the method parameter: "@Valid I input".
   */
  public <I extends InputData> void validated(I input) {
    // implementation not required, see JavaDoc
  }
}
