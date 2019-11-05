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
 *
 * Contributors:
 *     Alexander Koderman <ak@sernet.de> - initial API and implementation
 ******************************************************************************/
package org.veo.core.entity.validation;

/**
 * This factory provides a concrete validator implementation at runtime.
 *
 */
public class EntityValidatorRegistry {
    
    private IEntityValidator validator;
    
    private static EntityValidatorRegistry registry;
    
    private EntityValidatorRegistry(IEntityValidator validator) {
        this.validator = validator;
    }
    
    /**
     * Create a registry with the given validator.
     * 
     */
    public static void initialize(IEntityValidator validator) {
        registry = new EntityValidatorRegistry(validator);
    }

    /**
     * Get a validator from the registry.
     * 
     * @return
     * @throws MissingValidatorException when the validator registry was not properly initialized
     */
    public static IEntityValidator getValidator() {
        if (registry == null)
            throw new MissingValidatorException("The validator registry was not initialized.");
        if (registry.getValidatorInstance() == null)
            throw new MissingValidatorException("The validator registry was not initialized with an entity validator.");
        return registry.getValidatorInstance();
    }
    
    private IEntityValidator getValidatorInstance() {
        return this.validator;
    }
}
