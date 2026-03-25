/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2026  Jochen Kemnade
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
package org.veo.rest.common

import org.springframework.validation.FieldError

import spock.lang.Specification

class VeriniceExceptionHandlerSpec extends Specification{

    def handler = new VeriniceExceptionHandler()

    def "can handle multiple errors for the same field"() {
        when:
        def response = handler.handle([
            new FieldError("obj", "field", "This is bad."),
            new FieldError("obj", "field", "This is invalid.")
        ])

        then:
        with(response) {
            it.statusCode.value() == 400
            it.body == [field:'This is bad., This is invalid.']
        }
    }
}
