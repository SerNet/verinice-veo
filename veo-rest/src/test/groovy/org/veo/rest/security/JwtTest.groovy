/*******************************************************************************
 * Copyright (c) 2018 Jochen Kemnade.
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
package org.veo.rest.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.SignatureException
import spock.lang.Specification

class JwtTest extends Specification {

    def "test token lifecycle"() {
        setup:
        def signingKey = JwtKeyLoader.getPrivateJwtKey()
        def verificationKey = JwtKeyLoader.getPublicJwtKey()
        def token = Jwts.builder()
                .setSubject("admin")
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60))
                .signWith(SignatureAlgorithm.RS512, signingKey)
                .compact()

        when:
        def jwt = Jwts.parser()
                .setSigningKey(verificationKey)
                .parseClaimsJws(token)

        then:
        jwt.getBody().getSubject() == "admin"
    }

    def "reject broken signatures"() {
        setup:
        def signingKey = JwtKeyLoader.getPrivateJwtKey()
        def verificationKey = JwtKeyLoader.getPublicJwtKey()
        def token = Jwts.builder()
                .setSubject("admin")
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60))
                .signWith(SignatureAlgorithm.RS512, signingKey)
                .compact()

        def tokenWithoutSignature = token.substring(0, token.lastIndexOf('.'))
        def fakeSignature = ".lv4M2TWqAo3tuljvnvlSCqUg3WfzUoUQDHBxbcKErPf5eGXGz7XlJkCPFSmzOjOiSry3mtqPjTcnDrkNwQkngEsRGdDWAvnS9DoQ2rkr75ERtDyUB33I28JMj4mQZaaDb9qM05dhdNnEaGmk2d3rZ3Jsdol5UTTm6fUNtItdaHIe274n6cdQ1SOAMywmiDZS58vvEnfdPhajh6TZ9fJv0OVyJqpJSeXDy6gKdDVZyiB93QPE1Yw7We8IT2CXyT437ntkcBJR_Z00zAwXoxgr6Aj3C-2wlvPFkEC3iyN-RLHPB19bghFAAyWh2RORphkVdjSg-enYsTbq-9w6shE0Pg"

        when:
        Jwts.parser()
                .setSigningKey(verificationKey)
                .parse(tokenWithoutSignature + fakeSignature)

        then:
        SignatureException ex = thrown()
    }
}
