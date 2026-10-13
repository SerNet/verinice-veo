/*
 * verinice.veo
 * Copyright (C) 2026  Jochen Kemnade.
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
 */
package org.veo.core

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.entity.TranslatedText
import org.veo.core.entity.condition.ConstantExpression
import org.veo.core.repository.InspectionFindingRepository

import jakarta.transaction.Transactional

@Transactional()
class InspectionFindingRepositorySpec extends VeoSpringSpec {

    @Autowired
    InspectionFindingRepository inspectionFindingRepository

    def "Create and retrieve inspection findings"() {
        given:
        def client = createTestClient()
        newDomain(client) {
            applyInspection('everything-is-bad', newInspection {
                condition = new ConstantExpression(true)
                description = TranslatedText.of {'fix it'}
            })
        }
        client = clientRepository.save(client)
        def domain = client.domains.first()
        def unit = unitDataRepository.save(newUnit(client))
        def process = processDataRepository.save(newProcess(unit) {
            associateWithDomain(domain, 'PROC', 'Incomplete')
        })

        when:
        def findings = inspectionFindingRepository.findInspections(process, domain)

        then:
        findings.empty

        when:
        inspectionFindingRepository.updateInspections(domain, false)
        findings = inspectionFindingRepository.findInspections(process, domain)

        then:
        findings.size() == 1
        with(findings.first()) {
            it.description.translations[Locale.ENGLISH] == 'fix it'
        }

        when:
        inspectionFindingRepository.removeInspections(domain)
        findings = inspectionFindingRepository.findInspections(process, domain)

        then:
        findings.empty
    }
}
