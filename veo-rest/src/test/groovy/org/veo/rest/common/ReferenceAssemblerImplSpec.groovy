/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
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
 ******************************************************************************/
package org.veo.rest.common

import org.veo.adapter.presenter.api.common.ReferenceAssembler
import org.veo.core.entity.Asset
import org.veo.core.entity.Control
import org.veo.core.entity.Incident
import org.veo.core.entity.Scenario
import org.veo.core.entity.Scope

import spock.lang.Specification
import spock.lang.Unroll

class ReferenceAssemblerImplSpec extends Specification {

    ReferenceAssembler referenceAssembler= new ReferenceAssemblerImpl()

    @Unroll
    def "parsed entity id for #url is #parsedId"() {
        expect:
        referenceAssembler.parseId(url) == parsedId
        where:
        url | parsedId
        'http://localhost:9000/assets/40331ed5-be07-4c69-bf99-553811ce5454' | '40331ed5-be07-4c69-bf99-553811ce5454'
        'http://localhost:9000/controls/c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6' | 'c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6'
        'http://localhost:9000/scopes/59d3c21d-2f21-4085-950d-1273056d664a' | '59d3c21d-2f21-4085-950d-1273056d664a'
        'http://localhost:9000/scenarios/f05ab334-c605-456e-8a78-9e1bc85b8509' | 'f05ab334-c605-456e-8a78-9e1bc85b8509'
        'http://localhost:9000/incidents/7b4aa38a-117f-40c0-a5e8-ee5a59fe79ac' | '7b4aa38a-117f-40c0-a5e8-ee5a59fe79ac'
    }

    @Unroll
    def "parsed type for #url is #type"() {
        expect:
        referenceAssembler.parseType(url) == type
        where:
        url | type
        'http://localhost:9000/assets/40331ed5-be07-4c69-bf99-553811ce5454' | Asset
        'http://localhost:9000/controls/c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6' | Control
        'http://localhost:9000/scopes/59d3c21d-2f21-4085-950d-1273056d664a' | Scope
        'http://localhost:9000/scenarios/f05ab334-c605-456e-8a78-9e1bc85b8509' | Scenario
        'http://localhost:9000/incidents/7b4aa38a-117f-40c0-a5e8-ee5a59fe79ac' | Incident
    }

    @Unroll
    def "target reference for #type and #id is #reference"() {
        expect:
        referenceAssembler.targetReferenceOf(type, id) == reference
        where:
        type | id | reference
        Asset | '40331ed5-be07-4c69-bf99-553811ce5454' | '/assets/40331ed5-be07-4c69-bf99-553811ce5454'
        Control| 'c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6' | '/controls/c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6'
        Scenario| 'f05ab334-c605-456e-8a78-9e1bc85b8509'|  '/scenarios/f05ab334-c605-456e-8a78-9e1bc85b8509'
        Incident| '7b4aa38a-117f-40c0-a5e8-ee5a59fe79ac' | '/incidents/7b4aa38a-117f-40c0-a5e8-ee5a59fe79ac'
        Scope | '59d3c21d-2f21-4085-950d-1273056d664a' | '/scopes/59d3c21d-2f21-4085-950d-1273056d664a'
    }

    @Unroll
    def "resources reference for #type and #id is #reference"() {
        expect:
        referenceAssembler.resourcesReferenceOf(type) == reference
        where:
        type | reference
        Asset |  '/assets{?unit,displayName}'
        Control| '/controls{?unit,displayName}'
        Scenario| '/scenarios{?unit,displayName}'
        Incident| '/incidents{?unit,displayName}'
        Scope | '/scopes{?unit,displayName}'
    }

    @Unroll
    def "searches reference for #type and #id is #reference"() {
        expect:
        referenceAssembler.searchesReferenceOf(type) == reference
        where:
        type | reference
        Asset |  '/assets/searches'
        Control| '/controls/searches'
        Scenario| '/scenarios/searches'
        Incident| '/incidents/searches'
        Scope | '/scopes/searches'
    }
}
