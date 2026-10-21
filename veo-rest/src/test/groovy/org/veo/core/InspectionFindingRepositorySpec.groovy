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

import org.veo.core.entity.ElementType
import org.veo.core.entity.TaskConfiguration
import org.veo.core.entity.TranslatedText
import org.veo.core.entity.condition.ConstantExpression
import org.veo.core.entity.inspection.Severity
import org.veo.core.repository.InspectionFindingRepository
import org.veo.persistence.access.jpa.InspectionFindingDataRepository

import jakarta.transaction.Transactional

@Transactional()
class InspectionFindingRepositorySpec extends VeoSpringSpec {

    @Autowired
    InspectionFindingRepository inspectionFindingRepository

    @Autowired
    InspectionFindingDataRepository dataRepository

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

    def "task-related details are persisted"() {
        given: "an incident with a linked person and an applicable inspection"
        def client = createTestClient()
        newDomain(client) {
            applyElementTypeDefinition(newElementTypeDefinition(ElementType.INCIDENT, it) {
                links.culprit = newLinkDefinition(ElementType.PERSON, "PS")
            })
            applyInspection('all-broken', newInspection {
                condition = new ConstantExpression(true)
                description = TranslatedText.of {'Things are broken.'}
                severity = Severity.ERROR
                elementType = ElementType.INCIDENT
            })
        }
        client = clientRepository.save(client)
        def domain = client.domains.first()
        def unit = unitDataRepository.save(newUnit(client))
        def person = personDataRepository.save(newPerson(unit) {
            associateWithDomain(domain, "PS", "Awake")
        })
        def incident = incidentDataRepository.save(newIncident(unit) {
            associateWithDomain(domain, 'INCT', 'Detected')
            applyLink(newCustomLink(person,"culprit", domain))
        })

        when: "evaluating the incident"
        inspectionFindingRepository.updateInspections(domain,false)
        def findings = dataRepository.findAllByElementIsAndDomainIs(incident, domain)

        then: "the inspection description is used as a task summary and no responsible person is determined"
        findings.size() == 1
        with(findings.first()) {
            it.inspectionId == "all-broken"
            it.element == incident
            it.taskSummary.translations[EN] == "Things are broken."
            it.severity == Severity.ERROR
            it.responsible == null
        }

        when: "re-evaluating with a task config"
        domain.taskConfiguration = new TaskConfiguration([
            "all-broken": new TaskConfiguration.InspectionTaskTemplate(
                    TranslatedText.of {"Fix what you've broken!"},
                    "culprit"
                    )
        ])
        inspectionFindingRepository.updateInspections(domain, true)
        findings = dataRepository.findAllByElementIsAndDomainIs(incident, domain)

        then: "the summary and responsibility from the task template are applied"
        findings.size() == 1
        with(findings.first()) {
            it.inspectionId == "all-broken"
            it.element == incident
            it.taskSummary.translations[EN] == "Fix what you've broken!"
            it.severity == Severity.ERROR
            it.responsible == person
        }

        when: "multiple responsible persons are linked"
        def person2 = personDataRepository.save(newPerson(unit) {
            associateWithDomain(domain, "PS", "Awake")
        })
        incident.applyLink(newCustomLink(person2, "culprit", domain))
        inspectionFindingRepository.updateInspections(domain, true)
        findings = dataRepository.findAllByElementIsAndDomainIs(incident, domain)

        then: "no one is really responsible"
        findings.size() == 1
        with(findings.first()) {
            it.inspectionId == "all-broken"
            it.responsible == null
        }
    }
}
