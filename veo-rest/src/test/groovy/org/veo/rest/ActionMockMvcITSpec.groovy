/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Control
import org.veo.core.entity.EntityType
import org.veo.core.entity.Scenario
import org.veo.core.entity.TailoringReferenceType
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.CatalogItemData

@WithUserDetails("user@domain.example")
class ActionMockMvcITSpec extends VeoMvcSpec{
    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository

    String domainId
    String unitId

    def setup() {
        def client = createTestClient()
        def domain = newDomain(client) { d ->
            name = "IT-Grundschutz"
            EntityType.RISK_AFFECTED_TYPES.forEach {
                applyElementTypeDefinition(newElementTypeDefinition(it.singularTerm, d) {
                    subTypes.NotMyType = newSubTypeDefinition {}
                })
            }
            applyElementTypeDefinition(newElementTypeDefinition("control", d) {
                subTypes.RemoteControl = newSubTypeDefinition {}
                links.control_relevantAppliedThreat = newLinkDefinition(Scenario.SINGULAR_TERM, "Scarynario")
            })
            applyElementTypeDefinition(newElementTypeDefinition("scenario", d) {
                subTypes.Scarynario = newSubTypeDefinition {}
            })
            def scenario1 = newCatalogItem(d) {
                name = "bad things happen"
                elementType = Scenario.SINGULAR_TERM
                subType = "Scarynario"
                status = "NEW"
            }
            def scenario2 = newCatalogItem(d) {
                name = "worse things happen"
                elementType = Scenario.SINGULAR_TERM
                subType = "Scarynario"
                status = "NEW"
            }

            def subControl1 = newCatalogItem(d) {
                name = "remotely controlled"
                elementType = Control.SINGULAR_TERM
                subType = "RemoteControl"
                status = "NEW"
                addLinkTailoringReference(TailoringReferenceType.LINK, scenario1, "control_relevantAppliedThreat", [:])
                scenario1.addLinkTailoringReference(TailoringReferenceType.LINK_EXTERNAL, it, "control_relevantAppliedThreat", [:])
            }

            def subControl2 = newCatalogItem(d) {
                name = "con troll"
                elementType = Control.SINGULAR_TERM
                subType = "RemoteControl"
                status = "NEW"
                addLinkTailoringReference(TailoringReferenceType.LINK, scenario2, "control_relevantAppliedThreat", [:])
                scenario2.addLinkTailoringReference(TailoringReferenceType.LINK_EXTERNAL, it, "control_relevantAppliedThreat", [:])
            }
            catalogItems = [
                scenario1,
                scenario2,
                subControl1,
                subControl2,
                newCatalogItem(d) {
                    name = "super troll"
                    elementType = Control.SINGULAR_TERM
                    subType = "RemoteControl"
                    status = "NEW"
                    addTailoringReference(TailoringReferenceType.PART, subControl1)
                    addTailoringReference(TailoringReferenceType.PART, subControl2)
                    subControl1.addTailoringReference(TailoringReferenceType.COMPOSITE, it)
                    subControl2.addTailoringReference(TailoringReferenceType.COMPOSITE, it)
                }
            ]
        }
        client = clientRepository.save(client)
        domainId = domain.idAsString
        unitId = unitRepository.save(newUnit(client) {
            name = "Test unit"
            domains = [domain]
        }).idAsString
    }

    def "risk-analysis action can be performed for #type.pluralTerm"() {
        given: "three incarnated controls without links"
        def superControlId = incarnate("super troll", ["LINK"])
        def subControl1Id = parseJson(get("/controls?name=remotely controlled")).items[0].id
        def subControl2Id = parseJson(get("/controls?name=con troll")).items[0].id

        and: "an asset that implements the controls"
        def elementId = parseJson(post("/domains/$domainId/$type.pluralTerm", [
            name: "device",
            subType: "NotMyType",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"],
            controlImplementations: [
                [control: ["targetUri": "/controls/$superControlId"]],
            ]
        ])).resourceId

        expect: "control links to be absent"
        parseJson(get("/domains/$domainId/controls/$subControl1Id")).links == [:]
        parseJson(get("/domains/$domainId/controls/$subControl2Id")).links == [:]

        and: "asset risks to be absent"
        parseJson(get("/$type.pluralTerm/$elementId/risks")) == []

        when: "looking up available actions"
        def actions = parseJson(get("/domains/$domainId/$type.pluralTerm/$elementId/actions"))

        then: "risk analysis is returned"
        actions.size() == 1
        actions[0].id == "threatOverview"
        actions[0].name.de == "Gefährdungsübersicht erstellen"

        when: "performing the action"
        def result = parseJson(post("/domains/$domainId/$type.pluralTerm/$elementId/actions/threatOverview/execution", null, 200))

        then: "two new scenarios and two new risks are reported"
        result.createdEntities.size() == 4
        result.createdEntities*.designator.findAll { it.startsWith("SCN-") }.size() == 2
        result.createdEntities*.designator.findAll { it.startsWith("RSK-") }.size() == 2

        and: "the control links from the catalog have been applied"
        parseJson(get("/domains/$domainId/controls/$subControl1Id")).links.control_relevantAppliedThreat*.target*.name == ["bad things happen"]
        parseJson(get("/domains/$domainId/controls/$subControl2Id")).links.control_relevantAppliedThreat*.target*.name == ["worse things happen"]

        and: "risks have been created for the linked scenarios"
        parseJson(get("/$type.pluralTerm/$elementId/risks"))*.scenario*.name ==~ [
            "bad things happen",
            "worse things happen"
        ]

        when: "performing the action again"
        result = parseJson(post("/domains/$domainId/$type.pluralTerm/$elementId/actions/threatOverview/execution", null, 200))

        then: "nothing has happened"
        result.createdEntities.empty

        where:
        type << EntityType.RISK_AFFECTED_TYPES
    }

    private String incarnate(String itemName, Collection<String> exclude) {
        def id = parseJson(get("/domains/$domainId/catalog-items"))
                .items
                .find { it -> it.name == itemName }
                .id
        def descs = parseJson(get("/units/$unitId/incarnations?itemIds=$id&exclude=${exclude.join(",")}"))
        return parseJson(post("/units/$unitId/incarnations", descs)).first().id
    }
}
