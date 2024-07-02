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

import spock.lang.Specification

class ElementDomainAssociationDataSpec extends Specification{
    def "transient instances are never equal"() {
        given: "two aspects with the same parameters"
        def commonOwner = Mock(Element)
        def commonDomain = Mock(Domain)
        def aspect1 = new ElementDomainAssociationData(commonDomain, commonOwner, "a", "NEW")
        def aspect2 = new ElementDomainAssociationData(commonDomain, commonOwner, "a", "NEW")

        expect: "objects are not equal"
        aspect1 != aspect2
    }

    def "managed instances with different IDs are not equal"() {
        given: "two aspects with different IDs"
        def commonOwner = Mock(Element)
        def commonDomain = Mock(Domain)
        def uuid1 = UUID.randomUUID()
        def uuid2 = UUID.randomUUID()
        def aspect1 = new ElementDomainAssociationData(commonDomain, commonOwner, "a", "NEW").tap {
            dbId = uuid1
        }
        def aspect2 = new ElementDomainAssociationData(commonDomain, commonOwner, "a", "NEW").tap {
            dbId = uuid2
        }

        expect: "objects are not equal"
        aspect1 != aspect2
    }

    def "managed instances with the same ID are equal"() {
        given: "two aspects with the same ID"
        def commonOwner = Mock(Element)
        def commonDomain = Mock(Domain)
        def uuid = UUID.randomUUID()

        def aspect1 = new ElementDomainAssociationData(commonDomain, commonOwner, "a", "new").tap {
            dbId = uuid
        }
        def aspect2 = new ElementDomainAssociationData(commonDomain, commonOwner, "a", "new").tap {
            dbId = uuid
        }

        expect: "objects are equal"
        aspect1 == aspect2

        and: 'have the same hashCode'
        aspect1.hashCode() == aspect2.hashCode()
    }
}
