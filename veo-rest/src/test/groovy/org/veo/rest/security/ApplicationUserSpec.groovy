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
package org.veo.rest.security

import org.springframework.security.oauth2.jwt.Jwt

import spock.lang.Specification

public class ApplicationUserSpec extends Specification {

    def "A user can be extracted from a Jwt token"() {
        when:
        def clientId = UUID.randomUUID().toString()
        def jwt = new Jwt.Builder('foo').header('Foo', 'Bar').claim('groups', "/veo_client:$clientId").build()
        ApplicationUser user = ApplicationUser.authenticatedUser(jwt)

        then:
        user != null
        user.clientId == clientId
    }

    def "A helpful error message is produced if a user does not belong to any groups"() {
        when:
        def jwt = new Jwt.Builder('foo').header('Foo', 'Bar').claim('name', 'Arthur').build()
        ApplicationUser.authenticatedUser(jwt)

        then:
        Exception e = thrown()
        e.message =~ /Expected 1 client/
    }
}
