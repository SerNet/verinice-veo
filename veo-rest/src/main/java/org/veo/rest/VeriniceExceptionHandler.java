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
package org.veo.rest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import org.veo.commons.VeoException;
import org.veo.commons.VeoException.Error;

@ControllerAdvice
public class VeriniceExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({ VeoException.class })
    protected ResponseEntity<Object> handleVeoException(VeoException veoException,
            WebRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        Error error = veoException.getError();
        switch (error) {
        case ELEMENT_EXISTS:
            status = HttpStatus.CONFLICT;
            break;
        case ELEMENT_NOT_FOUND:
            status = HttpStatus.NOT_FOUND;
            break;
        case AUTHENTICATION_REQUIRED:
            status = HttpStatus.UNAUTHORIZED;
            break;
        }
        String bodyOfResponse = veoException.getMessage();
        return handleExceptionInternal(veoException, bodyOfResponse, new HttpHeaders(), status,
                request);
    }

}
