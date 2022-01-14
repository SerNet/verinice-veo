/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Finn Westendorf
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

package de.sernet.gendocs

import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor

//A single attribute of a CustomAspect or -Link schema
@TupleConstructor
@EqualsAndHashCode
class Attribute {
    String key
    String title
    String translation
    String type
    String format
    List<AttributeType> oneOf
}
