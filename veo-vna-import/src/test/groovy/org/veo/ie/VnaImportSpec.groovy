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
 *
 ******************************************************************************/
package org.veo.ie

import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.veo.persistence.PersistenceApplication
import org.veo.service.ElementService
import org.veo.service.LinkService
import org.veo.service.ie.VnaImport

import spock.lang.Specification

@ContextConfiguration
@DataJpaTest(showSql=false)
@ActiveProfiles("test")
class VnaImportSpec extends Specification {

    @Autowired
    VnaImport vnaImport;

    @Autowired
    ElementService elementService;
    
    @Autowired
    LinkService linkService;

    def "import VNA"(){
        setup:
            def is = VnaImportSpec.class.getResourceAsStream('VnaImportSpec.vna')
            def bytes = is.bytes
        when:
            def vna = vnaImport.importVna(bytes)
            Iterator elements = elementService.findAll().iterator()
            int numberOfElements = 0
            while(elements.hasNext()) {
                numberOfElements++
                elements.next()
            }
            Iterator links = linkService.getAll().iterator()
            int numberOfLinks = 0
            while(links.hasNext()) {
                numberOfLinks++
                links.next()
            }
        then:
            numberOfElements == 11
            numberOfLinks == 4
    }
}
