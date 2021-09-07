/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.persistence.entity.jpa;

import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import org.springframework.stereotype.Service;

import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.code.ModelValidationException;
import org.veo.core.entity.code.ModelValidator;
import org.veo.core.entity.code.ModelValidator.AbstractModelValidator;

import lombok.AllArgsConstructor;

/**
 * Validation Service that uses an injected JSR380 validator to check all
 * annotated validation rules before validating model invariants.
 */
@Service
@AllArgsConstructor
public class ValidationService {

    private Validator beanValidator;

    public void validate(Identifiable identifiable)
            throws ModelValidationException, ConstraintViolationException {
        // execute JSR 380 validations on model entities:
        Set<ConstraintViolation<Identifiable>> violations = beanValidator.validate(identifiable);
        if (!violations.isEmpty())
            throw new ConstraintViolationException(violations);

        if (!(identifiable instanceof Element))
            return;

        // execute additional model validations:
        Element entity = (Element) identifiable;
        AbstractModelValidator<Element> modelValidator = new ModelValidator.AbstractModelValidator<>() {
            @Override
            protected void doValidate(Element object, List<String> validationErrors) {
                return; // no additional validation needed
            }
        };
        modelValidator.validate(entity);
    }
}
