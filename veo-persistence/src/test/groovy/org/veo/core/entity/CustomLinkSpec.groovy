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

import org.veo.persistence.entity.jpa.CustomLinkData

import spock.lang.Specification

class CustomLinkSpec extends Specification{

    def "transient instances are never equal"() {
        given: "two aspects with the same parameters"
        def commonSource = Mock(Element)
        def commonTarget = Mock(Element)

        def link1 = new CustomLinkData().tap {
            setType("a")
            setSource(commonSource)
            setTarget(commonTarget)
        }
        def link2 = new CustomLinkData().tap {
            setType("a")
            setSource(commonSource)
            setTarget(commonTarget)
        }

        expect: "objects are not equal"
        link1 != link2
    }

    def "managed instances with different IDs are not equal"() {
        given: "two aspects with different IDs"
        def commonSource = Mock(Element)
        def commonTarget = Mock(Element)
        def uuid1 = UUID.randomUUID()
        def uuid2 = UUID.randomUUID()
        def link1 = new CustomLinkData().tap {
            setType("a")
            setSource(commonSource)
            setTarget(commonTarget)
            dbId = uuid1
        }
        def link2 = new CustomLinkData().tap {
            setType("a")
            setSource(commonSource)
            setTarget(commonTarget)
            dbId = uuid2
        }

        expect: "objects are not equal"
        link1 != link2
    }

    def "managed instances with the same ID are equal"() {
        given: "two aspects with the same ID"
        def commonSource = Mock(Element)
        def commonTarget = Mock(Element)
        def uuid = UUID.randomUUID()

        def link1 = new CustomLinkData().tap {
            setType("a")
            setSource(commonSource)
            setTarget(commonTarget)
            dbId = uuid
        }
        def link2 = new CustomLinkData().tap {
            setType("a")
            setSource(commonSource)
            setTarget(commonTarget)
            dbId = uuid
        }

        expect: "objects are equal"
        link1 == link2

        and: 'have the same hashCode'
        link1.hashCode() == link2.hashCode()
    }
}
