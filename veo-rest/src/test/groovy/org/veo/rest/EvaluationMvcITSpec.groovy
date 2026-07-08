/*******************************************************************************
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
 ******************************************************************************/
package org.veo.rest

import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
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
            owner:[targetUri:"/units/$unitId"],
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
}
