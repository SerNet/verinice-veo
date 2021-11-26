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
package org.veo.adapter.presenter.api.common

import spock.lang.Issue
import spock.lang.Specification

class ApiResponseBodySpec extends Specification {

    public static final String BAD_TEMPLATE = "\"JsonSchemaValidation failed: \$.owner.targetUri: does not match the uri pattern ^[A-Za-z][A-Za-z0-9+.-]*:(\\\\/\\\\/([A-Za-z0-9._~\\\\-%!\$&'()*+,;=:]*@)?[A-Za-z0-9._~\\\\-!\$&'()*+,;=%:\\\\[\\\\]]*(:[0-9]*)?)?[A-Za-z0-9._~\\\\-%!\$&'()*+,;=:@\\\\/]*([?][A-Za-z0-9._~\\\\-%!\$&'()*+,;=:@\\\\/?]*)?([#][A-Za-z0-9._~\\\\-%!\$&'()*+,;=:@\\\\/?]*)?\\\"\"\n" +
    "}\n"

    @Issue('VEO-1000')
    def "Parse message with invalid template"() {
        when: "invalid template is used as message without formatting"
        def body = new ApiResponseBody(true, BAD_TEMPLATE)

        then: "the message is returned"
        noExceptionThrown()
        body.getMessage() == BAD_TEMPLATE
    }

    @Issue('VEO-1000')
    def "Parse message with invalid template and arguments"() {
        when: "invalid template is used as template with arguments"
        new ApiResponseBody(true, BAD_TEMPLATE, "brace for impact")

        then: "an exception is thrown"
        thrown(UnknownFormatConversionException)
    }

    @Issue('VEO-1000')
    def "Parse message with invalid template no arguments"() {
        when: "invalid template is passed with null argument"
        def body = new ApiResponseBody(true, BAD_TEMPLATE, null)

        then: "the message is returned"
        noExceptionThrown()
        body.getMessage() == BAD_TEMPLATE
    }
}
