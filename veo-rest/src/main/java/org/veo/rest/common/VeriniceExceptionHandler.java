/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2018  Daniel Murygin.
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
package org.veo.rest.common;

import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import org.veo.adapter.presenter.api.DeviatingIdException;
import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.core.entity.DomainException;
import org.veo.core.entity.TranslationException;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.ReferenceTargetNotFoundException;
import org.veo.core.entity.exception.RiskConsistencyException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.entity.specification.MaxUnitsExceededException;
import org.veo.core.entity.specification.MissingAdminPrivilegesException;
import org.veo.core.usecase.common.ETagMismatchException;
import org.veo.core.usecase.domaintemplate.EntityAlreadyExistsException;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class VeriniceExceptionHandler {

  @ExceptionHandler({TranslationException.class})
  protected ResponseEntity<ApiResponseBody> handle(TranslationException exception) {
    return handle(exception, HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler({DomainException.class})
  protected ResponseEntity<ApiResponseBody> handle(DomainException exception) {
    return handle(exception, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({ClientBoundaryViolationException.class})
  protected ResponseEntity<ApiResponseBody> handle(ClientBoundaryViolationException exception) {
    log.warn("client boundary violation, mapping to 404", exception);
    return handle(
        new NotFoundException(exception.getEntityId(), exception.getEntityType()),
        HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler({IllegalArgumentException.class})
  protected ResponseEntity<ApiResponseBody> handle(IllegalArgumentException exception) {
    return handle(exception, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({EntityAlreadyExistsException.class})
  protected ResponseEntity<ApiResponseBody> handle(EntityAlreadyExistsException exception) {
    return handle(exception, HttpStatus.CONFLICT);
  }

  @ExceptionHandler({DeviatingIdException.class})
  protected ResponseEntity<ApiResponseBody> handle(DeviatingIdException exception) {
    return handle(exception, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({NotFoundException.class})
  protected ResponseEntity<ApiResponseBody> handle(NotFoundException exception) {
    return handle(exception, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler({NotImplementedException.class})
  protected ResponseEntity<ApiResponseBody> handle(NotImplementedException exception) {
    return handle(exception, HttpStatus.NOT_IMPLEMENTED);
  }

  @ExceptionHandler(ETagMismatchException.class)
  protected ResponseEntity<ApiResponseBody> handle(ETagMismatchException exception) {
    return handle(exception, HttpStatus.PRECONDITION_FAILED);
  }

  @ExceptionHandler({MissingAdminPrivilegesException.class})
  protected ResponseEntity<ApiResponseBody> handle(MissingAdminPrivilegesException exception) {
    return handle(exception, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler({MaxUnitsExceededException.class})
  protected ResponseEntity<ApiResponseBody> handle(MaxUnitsExceededException exception) {
    return handle(exception, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler({UnprocessableDataException.class})
  protected ResponseEntity<ApiResponseBody> handle(UnprocessableDataException exception) {
    return handle(exception, HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler({ReferenceTargetNotFoundException.class})
  protected ResponseEntity<ApiResponseBody> handle(ReferenceTargetNotFoundException exception) {
    return handle(exception, HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler({RiskConsistencyException.class})
  protected ResponseEntity<ApiResponseBody> handle(RiskConsistencyException exception) {
    return handle(exception, HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler(ClientNotActiveException.class)
  public ResponseEntity<ApiResponseBody> handleClientDecativatedExceptions(
      ClientNotActiveException ex) {
    return handle(ex, HttpStatus.FORBIDDEN);
  }

  private ResponseEntity<ApiResponseBody> handle(Throwable exception, HttpStatus status) {
    log.error("Error handling request", exception);
    return new ResponseEntity<>(
        new ApiResponseBody(false, Optional.empty(), exception.getMessage()), status);
  }
}
