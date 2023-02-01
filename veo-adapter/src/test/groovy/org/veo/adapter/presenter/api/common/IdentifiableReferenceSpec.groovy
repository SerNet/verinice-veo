/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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

import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Key

import spock.lang.Issue
import spock.lang.Specification

class IdentifiableReferenceSpec extends Specification {

    @Issue('VEO-560')
    def "create IdRef for CatalogItem"() {
        given:
        CatalogItem catalogItem = Stub {
            getId() >> Key.newUuid()
            getModelInterface() >> CatalogItem
            getDisplayName() >> null
        }
        ReferenceAssembler referenceAssembler = Mock()

        when:
        def mor = IdRef.from(catalogItem, referenceAssembler)

        then:
        mor.displayName == null
    }
}
