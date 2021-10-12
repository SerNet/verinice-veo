/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman
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
package org.veo.rest.test

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType

/**
 * VEO must honor the CORS settings when handling requests.
 */
class CORSRestTest extends VeoRestTest{

    def "get units with wrong CORS header"() {
        when: "the list of units is requested from a wrong origin"
        def (status, headers, body) = getWithOrigin('https://notreal.notverinice.example', '/units')

        then: "the request was denied"
        status == 403
        headers.getVary().toSorted() == [
            'Access-Control-Request-Headers',
            'Access-Control-Request-Method',
            'Origin'
        ]
        headers.getAccessControlAllowOrigin() == null
        body =~ /Invalid CORS request/
    }

    def "Get units with correct CORS header"() {
        when: "Request from a valid origin"
        def (status, headers, body) = getWithOrigin('https://domian.verinice.example', '/units')

        then: "the request was successful"
        status == 200
        headers.getAccessControlAllowOrigin() == 'https://domian.verinice.example'
        body =~ /displayName/
    }

    def "'null' origin is not allowed"() {
        when: "the list of units is requested with a 'null' origin"
        def (status, headers, body) = getWithOrigin('null', '/units')

        then: "the request was denied"
        status == 403
        body =~ /Invalid CORS request/
    }

    def getWithOrigin(String testOrigin, String relativeUri) {
        HttpHeaders headers = new HttpHeaders().tap {
            origin = testOrigin
        }
        def resp = exchange(relativeUri, HttpMethod.GET, headers, null)
        return [
            resp.statusCodeValue,
            resp.headers,
            resp.body.toString()
        ]
    }
}
