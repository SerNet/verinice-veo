/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman.
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
package org.veo.persistence.entity

import org.veo.core.entity.Asset
import org.veo.core.entity.Client
import org.veo.core.entity.Process
import org.veo.core.entity.Unit
import org.veo.persistence.entity.jpa.ScopeData
import org.veo.test.VeoSpec

class ScopeSpec extends VeoSpec {
    Client client

    Unit unit

    def setup() {
        this.client = newClient()
        this.unit = newUnit(client)
    }

    def "A scope can contain composites of different types"() {
        given: "two composites of different types"
        Asset composite1 = newAsset(unit) {
            name = "Composite 1"
        }
        Process composite2 = newProcess(unit) {
            name = "Composite 2"
        }

        when: "a scope is created with those composites"
        def scope = new ScopeData()
        scope.addMember(composite1)
        scope.addMember(composite2)

        then: "the scope contains both composites"
        scope.members.size() == 2
    }
}
