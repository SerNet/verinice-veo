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
    def "target affects hashCode"() {
        given: "links with different targets"
        def commonSource = Mock(EntityLayerSupertype)
        def link1 = new CustomLinkData().tap {
            setType("a")
            setSource(commonSource)
            setTarget(Mock(EntityLayerSupertype))
        }
        def link2 = new CustomLinkData().tap {
            setType("a")
            setSource(commonSource)
            setTarget(Mock(EntityLayerSupertype))
        }
        expect: "different hashCodes"
        link1.hashCode() != link2.hashCode()
        link1 != link2
    }

    def "type affects hashCode"() {
        given: "two links with different types"
        def commonSource = Mock(EntityLayerSupertype)
        def commonTarget = Mock(EntityLayerSupertype)
        def link1 = new CustomLinkData().tap {
            setSource(commonSource)
            setTarget(commonTarget)
            setType("a")
        }
        def link2 = new CustomLinkData().tap {
            setSource(commonSource)
            setTarget(commonTarget)
            setType("b")
        }
        expect: "different hashCodes"
        link1.hashCode() != link2.hashCode()
        link1 != link2
    }

    def "name does not affect hashCode"() {
        given: "two links with different names"
        def commonSource = Mock(EntityLayerSupertype)
        def commonTarget = Mock(EntityLayerSupertype)
        def link1 = new CustomLinkData().tap {
            setName("my name")
            setSource(commonSource)
            setTarget(commonTarget)
            setType("a")
        }
        def link2 = new CustomLinkData().tap {
            setName("another name")
            setSource(commonSource)
            setTarget(commonTarget)
            setType("a")
        }
        expect: "identical hashCodes"
        link1.hashCode() == link2.hashCode()
        link1 == link2
    }
}
