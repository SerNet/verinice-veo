/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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

class UserConfigurationRestTest extends VeoRestTest {

    def setup() {
        postNewUnit("test-unit").resourceId
    }

    def "Create a configuration with different users"() {
        when: "create configurations for two users"
        put("/user-configurations/appId1", [test: "value1"],null, 201)
        put("/user-configurations/appId1", [test: "value2"],null, 201, UserType.SECONDARY_CLIENT_USER)

        then: "each user has a separated configuration"
        get("/user-configurations/appId1").body == [test: "value1"]
        get("/user-configurations/appId1", 200, UserType.SECONDARY_CLIENT_USER).body == [test: "value2"]

        when:
        delete("/user-configurations/appId1", 204, UserType.SECONDARY_CLIENT_USER)

        then:
        get("/user-configurations/appId1", 404, UserType.SECONDARY_CLIENT_USER)

        and:
        get("/user-configurations/appId1").body == [test: "value1"]
    }
}
