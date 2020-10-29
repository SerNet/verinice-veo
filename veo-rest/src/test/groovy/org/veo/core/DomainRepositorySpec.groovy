/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
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
package org.veo.core

import javax.validation.ConstraintViolationException

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan

import org.veo.core.entity.Domain
import org.veo.persistence.access.DomainRepositoryImpl

@SpringBootTest(classes = DomainRepositorySpec.class)
@ComponentScan("org.veo")
class DomainRepositorySpec extends VeoSpringSpec {

    @Autowired
    private DomainRepositoryImpl domainRepository


    def "cannot violate the composition association between Client and Domain"() {
        given: "a domain"
        Domain domain = newDomain {
            name = "27001"
            description = "ISO/IEC"
            abbreviation = "ISO"
        }

        when: "the domain is saved without client association"
        domainRepository.save(domain)

        then: "an exception is raised"
        thrown(ConstraintViolationException)

        when: "the domain is loaded"
        def savedDomain = domainRepository.findById(domain.id)

        then: "it was not saved"
        savedDomain.isEmpty()
    }
}
