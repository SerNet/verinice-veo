/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
package org.veo.core.usecase.base

import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.usecase.repository.UnitRepository

import spock.lang.Specification

class UnitHierarchyProviderTest extends Specification{

    def unitRepository = Mock(UnitRepository)
    def sut = new UnitHierarchyProvider(unitRepository)

    def "finds all units in root" () {
        given: "a multi level unit hierarchy"
        def rootId = Key.newUuid()
        def root = Mock(Unit)
        def rootChild0 = Mock(Unit)
        def rootChild0Child0 = Mock(Unit)
        def rootChild0Child1 = Mock(Unit)
        def rootChild1 = Mock(Unit)
        def rootChild1Child0 = Mock(Unit)

        unitRepository.findById(rootId) >> Optional.of(root)
        unitRepository.findByParent(root) >> [rootChild0, rootChild1]
        unitRepository.findByParent(rootChild0) >> [
            rootChild0Child0,
            rootChild0Child1
        ]
        unitRepository.findByParent(rootChild0Child0) >> []
        unitRepository.findByParent(rootChild0Child1) >> []
        unitRepository.findByParent(rootChild1) >> [rootChild1Child0]
        unitRepository.findByParent(rootChild1Child0) >> []
        when:
        def output = sut.findAllInRoot(rootId)
        then:
        output == [
            root,
            rootChild0,
            rootChild0,
            rootChild0Child0,
            rootChild0Child1,
            rootChild1,
            rootChild1Child0] as Set
    }
}
