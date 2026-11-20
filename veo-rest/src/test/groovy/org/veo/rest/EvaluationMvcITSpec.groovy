/*
 * verinice.veo
 * Copyright (C) 2026  Jonas Jordan
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
package org.veo.rest

import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
import org.veo.core.entity.TaskConfiguration
import org.veo.core.entity.TranslatedText
import org.veo.core.entity.condition.ConstantExpression
import org.veo.core.entity.condition.PartCountExpression
import org.veo.core.entity.decision.ExpressiveDecision

@WithUserDetails("user@domain.example")
class EvaluationMvcITSpec extends VeoMvcSpec{
    Domain domain
    String unitId

    def setup() {
        def client = createTestClient()
        domain = newDomain(client) {
            applyElementTypeDefinition(newElementTypeDefinition(ElementType.INCIDENT, it) {
                subTypes.accident = newSubTypeDefinition {}
            })
        }
        client = clientRepository.save(client)
        domain = client.domains.first()
        unitId = unitDataRepository.save(newUnit(client) {
            name = "Test unit"
            domains = [domain]
        }).idAsString
    }

    def "reports sorted decision results"() {
        given:
        domain.applyDecision("last",new ExpressiveDecision().tap {
            name = new TranslatedText([
                (Locale.ENGLISH): "last",
                (Locale.GERMAN): "letzte",
            ])
            elementType = ElementType.INCIDENT
            elementSubType = "accident"
            expression = new ConstantExpression(4.99)
            sortKey = "c"
        })
        domain.applyDecision("first",new ExpressiveDecision().tap {
            name = new TranslatedText([
                (Locale.ENGLISH): "first",
                (Locale.GERMAN): "erste",
            ])
            elementType = ElementType.INCIDENT
            elementSubType = "accident"
            expression = new ConstantExpression(true)
            sortKey = "a"
        })
        domain.applyDecision("doesntMatter",new ExpressiveDecision().tap {
            name = new TranslatedText([
                (Locale.ENGLISH): "doesn't matter",
                (Locale.GERMAN): "egal",
            ])
            elementType = ElementType.INCIDENT
            elementSubType = "accident"
            expression = new PartCountExpression("accident")
        })
        domain.applyDecision("middle",new ExpressiveDecision().tap {
            name = new TranslatedText([
                (Locale.ENGLISH): "middle",
                (Locale.GERMAN): "mittlere",
            ])
            elementType = ElementType.INCIDENT
            elementSubType = "accident"
            expression = new ConstantExpression("sandwhiched")
            sortKey = "b"
        })
        domainDataRepository.save(domain)

        when:
        def results = parseJson(post("/domains/${domain.id}/incidents/evaluation", [
            name: "my incident",
            subType: "accident",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"],
        ], 200))

        then:
        results.inspectionFindings.size() == 4
        results.inspectionFindings*.description*.en == [
            "first: yes",
            "middle: sandwhiched",
            "last: 4.99",
            "doesn't matter: 0",
        ]
    }

    def "decisions are evaluated when an element is associated with a domain"() {
        given: "a decision in the domain"
        domain.applyDecision("alwaysTrue", new ExpressiveDecision().tap {
            name = new TranslatedText([(Locale.ENGLISH): "always true"])
            elementType = ElementType.INCIDENT
            elementSubType = "accident"
            expression = new ConstantExpression(true)
        })
        domainDataRepository.save(domain)

        and: "an incident that is not yet associated with the domain"
        def incidentId = executeInTransaction {
            def unit = unitDataRepository.findById(UUID.fromString(unitId)).get()
            incidentDataRepository.save(newIncident(unit) {
                name = "my incident"
            }).idAsString
        }

        when: "associating the incident with the domain"
        post("/domains/${domain.id}/incidents/$incidentId", [
            subType: "accident",
            status : "NEW",
        ], 200)

        then: "the decision has been evaluated"
        parseJson(get("/domains/${domain.id}/incidents/$incidentId")).decisionResults.alwaysTrue?.value == true
    }

    def "findings are shown for unclear responsibilities"() {
        given: "two inspection tasks in the domain"
        domain.applyElementTypeDefinition(newElementTypeDefinition(ElementType.DOCUMENT, domain) {
            it.subTypes.specList = newSubTypeDefinition {}
            it.links.maintainer = newLinkDefinition(ElementType.PERSON, "techie")
            it.translations[EN] = [
                maintainer: "Maintainer",
                document_specList_singular: "Spec list",
                document_specList_plural: "Spec lists",
                document_specList_status_NEW: "new",
            ]
        })
        domain.applyInspection("checkA", newInspection {
            it.elementType = ElementType.DOCUMENT
            it.condition = new ConstantExpression(false)
        })
        domain.taskConfiguration = new TaskConfiguration([
            checkA: new TaskConfiguration.InspectionTaskTemplate(
                    TranslatedText.of {"Check thing A"},
                    "maintainer",
                    ),
        ])
        domain = domainDataRepository.save(domain)

        and:
        def person1Id = executeInTransaction {
            def unit = unitDataRepository.findById(UUID.fromString(unitId)).get()
            personDataRepository.save(newPerson(unit) {}).idAsString
        }
        def person2Id = executeInTransaction {
            def unit = unitDataRepository.findById(UUID.fromString(unitId)).get()
            personDataRepository.save(newPerson(unit) {}).idAsString
        }

        when: "an element has no responsibility links"
        def findings = parseJson(post("/domains/${domain.id}/documents/evaluation", [
            name: "docer",
            subType: "specList",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"],
        ], 200)).inspectionFindings

        then:
        findings.size() == 1
        findings[0].severity == "HINT"
        findings[0].description.en == "No person is responsible for the task type 'Check thing A'. To assign a responsible person, please link them as 'Maintainer'."

        when: "an element has multiple responsibility links"
        findings = parseJson(post("/domains/${domain.id}/documents/evaluation", [
            name: "docer",
            subType: "specList",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"],
            links: [
                maintainer: [
                    [target: [targetUri: "/persons/$person1Id"]],
                    [target: [targetUri: "/persons/$person2Id"]],
                ]
            ]
        ], 200)).inspectionFindings

        then:
        findings.size() == 1
        findings[0].severity == "WARNING"
        findings[0].description.en == "Multiple persons are responsible for the task type 'Check thing A'. A task requires a single responsible person. Please remove all 'Maintainer' links except for one."

        when: "an element has one responsibility link"
        findings = parseJson(post("/domains/${domain.id}/documents/evaluation", [
            name: "docer",
            subType: "specList",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"],
            links: [
                maintainer: [
                    [target: [targetUri: "/persons/$person1Id"]],
                ]
            ]
        ], 200)).inspectionFindings

        then:
        findings.size() == 0

        when: "multiple task templates are affected"
        domain.applyInspection("checkB", newInspection {
            it.elementType = ElementType.DOCUMENT
            it.condition = new ConstantExpression(false)
        })
        domain.taskConfiguration = new TaskConfiguration([
            checkA: new TaskConfiguration.InspectionTaskTemplate(
                    TranslatedText.of {"Check thing A"},
                    "maintainer",
                    ),
            checkB: new TaskConfiguration.InspectionTaskTemplate(
                    TranslatedText.of {"Check thing B"},
                    "maintainer",
                    ),
        ])
        domainDataRepository.save(domain)

        and: "an element has no responsibility links"
        findings = parseJson(post("/domains/${domain.id}/documents/evaluation", [
            name: "docer",
            subType: "specList",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"],
        ], 200)).inspectionFindings

        then:
        findings.size() == 1
        findings[0].severity == "HINT"
        findings[0].description.en == "No person is responsible for the following task type(s):\n" +
                "• Check thing A\n" +
                "• Check thing B\n" +
                "To assign a responsible person, please link them as 'Maintainer'."

        when: "an element has multiple responsibility links"
        findings = parseJson(post("/domains/${domain.id}/documents/evaluation", [
            name: "docer",
            subType: "specList",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"],
            links: [
                maintainer: [
                    [target: [targetUri: "/persons/$person1Id"]],
                    [target: [targetUri: "/persons/$person2Id"]],
                ]
            ]
        ], 200)).inspectionFindings

        then:
        findings.size() == 1
        findings[0].severity == "WARNING"
        findings[0].description.en == "Multiple persons are responsible for the following task type(s):\n" +
                "• Check thing A\n" +
                "• Check thing B\n" +
                "A task requires a single responsible person. Please remove all 'Maintainer' links except for one."

        when: "an element has one responsibility link"
        findings = parseJson(post("/domains/${domain.id}/documents/evaluation", [
            name: "docer",
            subType: "specList",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"],
            links: [
                maintainer: [
                    [target: [targetUri: "/persons/$person1Id"]],
                ]
            ]
        ], 200)).inspectionFindings

        then:
        findings.size() == 0
    }
}
