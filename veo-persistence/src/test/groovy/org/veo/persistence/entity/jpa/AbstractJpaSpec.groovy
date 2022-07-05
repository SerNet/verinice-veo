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
package org.veo.persistence.entity.jpa

import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

import org.veo.core.entity.Element
import org.veo.core.entity.Unit
import org.veo.core.repository.PagingConfiguration
import org.veo.persistence.JpaTestConfig
import org.veo.persistence.VeoJpaConfiguration
import org.veo.persistence.access.ElementQueryImpl
import org.veo.persistence.access.jpa.ElementDataRepository
import org.veo.test.VeoSpec

@DataJpaTest
@ContextConfiguration(classes = [VeoJpaConfiguration, JpaTestConfig])
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
abstract class AbstractJpaSpec extends VeoSpec {

    List<Element> findByUnit(ElementDataRepository repository, Unit unit) {
        new ElementQueryImpl(repository, unit.client).whereOwnerIs(unit).execute(PagingConfiguration.UNPAGED).resultPage
    }
}
