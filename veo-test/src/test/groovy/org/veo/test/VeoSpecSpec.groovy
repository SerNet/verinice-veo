/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.test

import org.veo.core.entity.Client
import org.veo.core.entity.Unit

class VeoSpecSpec extends VeoSpec {

    def "Create a new asset with a name"() {
        given:
        def assetName = 'Asset 1'
        def unit = Mock(Unit)

        when:
        def asset = newAsset unit, {
            name = assetName
        }

        then:
        asset.name == assetName
        asset.owner == unit
    }

    def "Create a new client with a name"() {
        given:
        def clientName = 'Client 1'

        when:
        def client = newClient {
            name = clientName
        }

        then:
        client.name == clientName
    }

    def "Create a new unit"() {
        given:
        def client = Mock(Client)

        when:
        def unit = newUnit(client)

        then:
        unit instanceof Unit
        unit != null
    }

    def "Create a new unit with a given id" () {
        given:
        def id = UUID.randomUUID()
        def client = Mock(Client)

        when:
        def unit = newUnit client, {
            it.id = id
        }

        then:
        unit instanceof Unit
        unit != null
        unit.id == id
        unit.client == client
    }
}