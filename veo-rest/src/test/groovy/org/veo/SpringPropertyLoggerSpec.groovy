/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Ben Nasrallah.
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
package org.veo

import org.slf4j.Logger
import org.springframework.core.env.Environment

import spock.lang.Specification

public class SpringPropertyLoggerSpec extends Specification {

    def "log specified spring properties" () {
        given: "a logger"
        Logger logger = Mock()

        and: "an environment"
        Environment env = Mock()
        1 * env.getProperty("veo.logging.properties") >> "prop1,prop2,prop3"
        1 * env.getProperty("prop1") >> "val1"
        1 * env.getProperty("prop2") >> ""

        when: "spring properties get logged"
        new SpringPropertyLogger().logProperties(logger, env);

        then: "all specified properties get logged"
        1 * logger.debug('spring property {}: {}', 'prop1', 'val1')
        1 * logger.debug('spring property {}: {}', 'prop2', '')
        1 * logger.debug('spring property {}: {}', 'prop3', '')
    }
}
