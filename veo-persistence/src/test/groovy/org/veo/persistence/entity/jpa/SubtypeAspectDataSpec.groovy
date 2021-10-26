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
package org.veo.persistence.entity.jpa

import org.veo.core.entity.Domain
import org.veo.core.entity.Element
import org.veo.core.entity.InvalidSubTypeException

import spock.lang.Specification

class SubtypeAspectDataSpec extends Specification{
    def "domain affects identity"() {
        given: "two aspects with different domains"
        def commonOwner = Mock(Element)
        def aspect1 = new SubTypeAspectData(Mock(Domain), commonOwner, "a", "NEW")
        def aspect2 = new SubTypeAspectData(Mock(Domain), commonOwner, "a", "NEW")
        expect: "different hashCodes"
        aspect1.hashCode() != aspect2.hashCode()
        aspect1 != aspect2
    }

    def "owner affects identity"() {
        given: "two aspects with different owners"
        def commonDomain = Mock(Domain)
        def aspect1 = new SubTypeAspectData(commonDomain, Mock(Element), "a", "NEW")
        def aspect2 = new SubTypeAspectData(commonDomain, Mock(Element), "a", "NEW")
        expect: "different hashCodes"
        aspect1.hashCode() != aspect2.hashCode()
        aspect1 != aspect2
    }

    def "subType does not affect identity"() {
        given: "two aspects with different sub types"
        def commonDomain = Mock(Domain)
        def commonOwner = Mock(Element)
        def aspect1 = new SubTypeAspectData(commonDomain, commonOwner, "a", "NEW")
        def aspect2 = new SubTypeAspectData(commonDomain, commonOwner, "b", "NEW")
        expect: "same hashCodes"
        aspect1.hashCode() == aspect2.hashCode()
        aspect1 == aspect2
    }

    def "status does not affect identity"() {
        given: "two aspects with different status"
        def commonDomain = Mock(Domain)
        def commonOwner = Mock(Element)
        def aspect1 = new SubTypeAspectData(commonDomain, commonOwner, "a", "NEW")
        def aspect2 = new SubTypeAspectData(commonDomain, commonOwner, "a", "OLD")
        expect: "same hashCodes"
        aspect1.hashCode() == aspect2.hashCode()
        aspect1 == aspect2
    }
}
