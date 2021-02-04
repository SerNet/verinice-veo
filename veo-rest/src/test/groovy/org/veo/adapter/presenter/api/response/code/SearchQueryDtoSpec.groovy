/*******************************************************************************
 * Copyright (c) 2020 Urs Zeidler.
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
package org.veo.adapter.presenter.api.response.code

import org.veo.adapter.presenter.api.dto.SearchQueryDto
import org.veo.adapter.presenter.api.dto.UuidQueryConditionDto

import spock.lang.Specification

class SearchQueryDtoSpec extends Specification {
    def LEGACY_SEARCH_ID = "q1Yqzcss8UxRsqpWKkvMKU0tVrKKVjI0tDBMTjM01U1JsTTSNTY2N9NNMjI20k1LMUsysLBMM0kytVSKrdVRSsksLshJrPRLzE1VssorzcmpBQA="

    def unitUuid = UUID.nameUUIDFromBytes("testing testing 1,2,3".bytes).toString()

    def "encode search query"() {
        given: "a search query"
        def q = SearchQueryDto.builder()
                .unitId(new UuidQueryConditionDto().tap{
                    values = [unitUuid] as Set
                })
                .build()

        when: "it is encoded as an ID string"
        def searchId = q.getSearchId()

        then: "a search ID is created"
        searchId != null

        when: "it is decoded back into a search query"
        def decodedQuery = SearchQueryDto.decodeFromSearchId(searchId)

        then: "the query is reconstructed correctly"
        decodedQuery.getUnitId().values == [unitUuid] as Set
        decodedQuery.getDisplayName() == null
    }

    def "encoded search query must not contain reserved URI characters"() {
        given: "a search object that leads to a '/' character in base64 encoding:"
        def q = SearchQueryDto.builder()
                .unitId(new UuidQueryConditionDto().tap{
                    values = [unitUuid] as Set
                })
                .build()

        when: "it is encoded as an ID string"
        def searchId = q.getSearchId()

        then: "the ID contains only unreserved URI characters"
        // Note: '=' will be percent-encoded in a URL but is unproblematic and therefore used
        // by base64url as padding character, see RFC 4648 sec. 5.
        searchId ==~ /^[a-zA-Z0-9\-._~=]*$/
    }

    def "legacy search query DTO can still be decoded"() {
        when:
        def decoded = SearchQueryDto.decodeFromSearchId(LEGACY_SEARCH_ID)
        then:
        decoded != null
        decoded.unitId.values == [unitUuid] as Set
    }
}