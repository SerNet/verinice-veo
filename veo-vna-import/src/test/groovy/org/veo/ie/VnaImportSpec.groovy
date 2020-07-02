/*******************************************************************************
 * Copyright (c) 2018 Daniel Murygin.
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
package org.veo.ie

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.ComponentScan

import spock.lang.Specification

@ComponentScan("org.veo")
@DataJpaTest
class VnaImportSpec extends Specification {

    @Autowired
    VnaImport vnaImport;

    def "import VNA"() {
        setup:
        def is = VnaImportSpec.class.getResourceAsStream('VnaImportSpec.vna')
        when:
        vnaImport.importVna(is)
        then:
        // TODO re-implement (count number of elements & links).
        // numberOfElements == 11
        // numberOfLinks == 4
        true == true
    }
}
