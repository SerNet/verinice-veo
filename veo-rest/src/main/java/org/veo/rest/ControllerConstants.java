/*******************************************************************************
 * Copyright (c) 2019 Daniel Murygin.
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
package org.veo.rest;

/**
 * This class contains constants for the REST controllers.
 */
// @SecurityRequirement(name = RestApplication.SECURITY_SCHEME_BEARER_AUTH)
public final class ControllerConstants {

    static final String PARENT_PARAM = "parent";
    static final String UUID_PARAM = "uuid";
    static final String UUID_REGEX = "[a-fA-F\\d]{8}(?:-[a-fA-F\\d]{4}){3}-[a-fA-F\\d]{12}";
    static final String UNIT_PARAM = "unit";

    static final String UUID_DEFINITION = "This is the normalized UUID representation:\n"
            + "* a block of 8 HEX chars followed by\n* 3 blocks of 4 HEX chars followed by\n"
            + "* a block of 12 HEX chars.";
    static final String UUID_EXAMPLE = "f35b982c-8ad4-4515-96ee-df5fdd4247b9";

    private ControllerConstants() {
    }

}
