/*******************************************************************************
 * Copyright (c) 2018 Alexander Ben Nasrallah.
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
 *
 ******************************************************************************/
package org.veo.commons

import spock.lang.Specification;

public class VeoExceptionSpec extends Specification {

    def "get parametrized message"()  {
        given:
        def veoException = new VeoException(VeoException.Error.ELEMENT_NOT_FOUND, "element with uuid '%uuid%' not found", "uuid", "deadbeef");
        
        when:
        def message = veoException.getMessage();
        
        then:
        message == "element with uuid 'deadbeef' not found"
    }
}
