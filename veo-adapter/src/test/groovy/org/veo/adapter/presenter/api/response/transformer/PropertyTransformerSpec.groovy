/*******************************************************************************
 * Copyright (c) 2020 Jonas Jordan.
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
package org.veo.adapter.presenter.api.response.transformer

import org.veo.core.entity.CustomProperties

import spock.lang.Specification

class PropertyTransformerSpec extends Specification {

    private PropertyTransformer sut;

    def setup() {
        sut = new PropertyTransformer()
    }

    def 'should transform attributes to properties'() {
        given: 'a map of attributes and a target aspect entity'
        def attributes = [
            'p1': 'v1',
            'p2': 'v2',
            'p3': 10,
            'p4': ['1', '2', '3'],
            'p5': true
        ]
        def aspect = Mock(CustomProperties)

        when: 'the attributes are mapped'
        sut.applyDtoPropertiesToEntity(attributes, aspect)

        then: 'they are applied to the entity'
        1 * aspect.setProperty('p1', 'v1')
        1 * aspect.setProperty('p2', 'v2')
        1 * aspect.setProperty('p3', 10)
        1 * aspect.setProperty('p4', ['1', '2', '3'])
        1 * aspect.setProperty('p5', true)
    }

    def 'should throw exception for unsupported type'() {
        given: 'a map with an object'
        def attributes = [
            'p1': {
            }
        ]
        def aspect = Mock(CustomProperties)

        when: 'the attribute is mapped'
        sut.applyDtoPropertiesToEntity(attributes, aspect)

        then: 'an exception is thrown'
        thrown IllegalArgumentException
    }

    def 'should throw exception for unsupported list item type'() {
        given: 'a map with a list that includes an integer'
        def attributes = [
            'p1': ['ok', 12]]
        def aspect = Mock(CustomProperties)

        when: 'the attribute is mapped'
        sut.applyDtoPropertiesToEntity(attributes, aspect)

        then: 'an exception is thrown'
        thrown IllegalArgumentException
    }
}
