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

import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Unit;

/**
 * The entity layer will call a validator to ensure integrity of
 * business objects at runtime.
 * 
 *  The validator implementation must support the Java API for JavaBeans Validation 2.0
 *  as defined in JSR-380.
 *
 */
public interface IEntityValidator {
    
    public boolean validate(EntityLayerSupertype entity);
    
    public boolean validate(Client client);
    
    public boolean validate(Unit unit);

}
