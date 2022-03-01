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
package org.veo.core.usecase

import org.apache.commons.codec.digest.DigestUtils

import org.veo.core.usecase.common.ETag

import spock.lang.Specification

public class ETagSpec extends Specification{

    def "create an etag"() {
        given:
        String id = UUID.randomUUID().toString()
        long version = 4
        String salt = "salt-for-etag"
        ETag.setSalt(salt)
        String idSaltVersion = id + "_" + salt + "_" + Long.toString(version)

        when:
        String eTag = ETag.from(id, version)
        String hash = DigestUtils.sha256Hex(idSaltVersion)

        then:
        eTag.equalsIgnoreCase(hash)
    }

    def "match an etag"() {
        given:
        String id = UUID.randomUUID().toString()
        long version = 4
        String salt = "salt-for-etag"
        ETag.setSalt(salt)
        String idSaltVersion = id + "_" + salt + "_" + Long.toString(version)

        when:
        String hash = DigestUtils.sha256Hex(idSaltVersion)

        then:
        ETag.matches(id, version, hash)
        ETag.matches(id, version, hash.toLowerCase())
        ETag.matches(id, version, hash.toUpperCase())
    }
}
