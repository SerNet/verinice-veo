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

import spock.lang.Specification

class SearchQueryDtoSpec extends Specification {


    public static final String ENCODED = "q1Yqzcss8UxRslIyNLQwTE4zNNVNSbE00jU2NjfTTTIyNtJNSzFLMrCwTDNJMrVU0lFKL8ovLQipLEhVssorzcnRUUrJLC7ISaz0S8yFCtUCAA=="

    def "encode search query"() {
        given: "a search query"
        def q = SearchQueryDto.builder()
                .unitId(UUID.nameUUIDFromBytes("testing testing 1,2,3".bytes).toString())
                .build();

        when: "it is encoded as an ID string"
        def searchId = q.getSearchId()

        then: "encoding worked as expected"
        searchId == ENCODED
    }

    def "decode search query"() {
        given: "a search id"
        String searchId = ENCODED

        when: "it is decoded into a search query"
        def query = SearchQueryDto.decodeFromSearchId(searchId)

        then: "the query is reconstructed correctly"
        query.getUnitId() == UUID.nameUUIDFromBytes("testing testing 1,2,3".bytes).toString()
        query.getDisplayName() == null
    }

    def "encoded search query must not contain reserved URI characters"() {
        given: "a search object that leads to a '/' character in base64 encoding:"
        def q = SearchQueryDto.builder()
                .unitId("59c9e163-88e5-4ebd-886a-2b21470bd19d")
                .build();

        when: "it is encoded as an ID string"
        def searchId = q.getSearchId()

        then: "the ID contains only unreserved URI characters"
        // Note: '=' will be percent-encoded in a URL but is unproblematic and therefore used
        // by base64url as padding character, see RFC 4648 sec. 5.
        searchId ==~ /^[a-zA-Z0-9\-._~=]*$/
    }
}