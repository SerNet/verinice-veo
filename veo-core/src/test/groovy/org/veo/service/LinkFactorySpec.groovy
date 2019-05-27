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
package org.veo.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

import org.veo.core.VeoCoreConfiguration
import org.veo.model.Element
import org.veo.persistence.ElementRepository
import spock.lang.Specification

@DataJpaTest(showSql=false)
@ActiveProfiles("test")
@ContextConfiguration(classes=VeoCoreConfiguration)
class LinkFactorySpec extends Specification {

    @Autowired
    LinkFactory linkFactory

    @Autowired
    ElementRepository elementRepository


    def "create a simple link"(){
        when:
        def link = linkFactory.createLink([
            'id': '0815'
        ])
        then:
        link.uuid == '0815'
    }

    def "create a link between two objects"(){
        given:
        def anakinId = '1'
        def lukeId = '2'
        def linkId = '4711'

        def anakin = new Element(anakinId).with {
            title = 'Anakin'
            typeId = 'person'
            it
        }
        def luke = new Element(lukeId).with {
            title = 'Luke'
            typeId = 'person'
            it
        }
        elementRepository.saveAll([anakin, luke])

        when:
        def link = linkFactory.createLink([
            'id': linkId,
            'type': 'father',
            'source': anakin.getUuid(),
            'target': luke.getUuid()])

        then:
        link.uuid == linkId
        link.typeId == 'father'
        link.source == anakin
        link.destination == luke
    }
}
