/*******************************************************************************
 * Copyright (c) 2018 Daniel Murygin.
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
package org.veo.rest.common;

import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.core.entity.DomainException;
import org.veo.core.entity.exception.NotFoundException;

@ControllerAdvice
public class VeriniceExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({ DomainException.class })
    protected ResponseEntity<ApiResponseBody> handle(DomainException exception) {
        return handle(exception, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ IllegalArgumentException.class })
    protected ResponseEntity<ApiResponseBody> handle(IllegalArgumentException exception) {
        return handle(exception, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ NotFoundException.class })
    protected ResponseEntity<ApiResponseBody> handle(NotFoundException exception) {
        return handle(exception, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({ NotImplementedException.class })
    protected ResponseEntity<ApiResponseBody> handle(NotImplementedException exception) {
        return handle(exception, HttpStatus.NOT_IMPLEMENTED);
    }

    private ResponseEntity<ApiResponseBody> handle(Throwable exception, HttpStatus status) {
        return new ResponseEntity<>(
                new ApiResponseBody(false, Optional.empty(), exception.getMessage()), status);
    }
}
