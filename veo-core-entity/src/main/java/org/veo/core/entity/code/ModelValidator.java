/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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
package org.veo.core.entity.code;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.veo.core.entity.ModelObject;

/**
 * Provides access to the model validator.
 */
public final class ModelValidator {

    /**
     * Base class for all model validators.
     */
    public abstract static class AbstractModelValidator<T extends ModelObject> {

        /**
         * Performs checks for domain model invariants. Should use
         * <code>EntitySpecification</code> as an intention revealing interface for
         * implemented rules when possible.
         *
         * @param object
         *            a valid ModelObject. Valid means it has fully annotated with JSR
         *            380 bean validation annotations with no validation errors.
         *            Field-level checks will not be performed by this method.
         */
        public final void validate(T object) {
            LinkedList<String> validationErrors = new LinkedList<>();
            doValidate(object, validationErrors);
            if (!validationErrors.isEmpty()) {
                throw new ModelValidationException("Validation failed for " + object,
                        Collections.unmodifiableList(validationErrors));
            }
        }

        /**
         * This method may be used to implement additional checks for specific types.
         *
         * @param object
         *            a valid model object type
         * @param validationErrors
         *            an output parameter for validation error messages
         */
        protected abstract void doValidate(T object, List<String> validationErrors);

    }

    public static class ModelObjectValidator extends AbstractModelValidator<ModelObject> {

        @Override
        public void doValidate(ModelObject object, List<String> validationErrors) {
            // no specific validation is done for this type
        }
    }

    public static void validate(ModelObject target) {
        new ModelObjectValidator().validate(target);
    }

    private ModelValidator() {

    }
}
