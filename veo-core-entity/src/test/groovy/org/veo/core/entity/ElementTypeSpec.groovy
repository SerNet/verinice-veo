/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
package org.veo.core.entity

import spock.lang.Specification

class ElementTypeSpec extends Specification {

    def "ElementType #type has a matching EntityType value"() {
        when:
        def singularTerm = type.singularTerm

        then:
        singularTerm != null

        when:
        def entityType = type.entityType

        then:
        entityType.name() == type.name().toUpperCase(Locale.US)

        where:
        type << ElementType.values()
    }
}
