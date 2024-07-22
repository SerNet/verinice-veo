/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler
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

import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Domain
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.exception.UnprocessableDataException
import org.veo.core.entity.specification.ClientBoundaryViolationException

import spock.lang.Ignore
import spock.lang.Issue

@WithUserDetails("user@domain.example")
class IncarnateCatalogItemMockMvcITSpec extends CatalogSpec {
    def "retrieve the apply info "() {
        when: "a request is made to the server"
        def result = getIncarnationDescriptions([item1], "MANUAL")

        then: "the parameter object is returned"
        result.parameters.size() == 1
    }

    def "retrieve the apply info for two items"() {
        when: "a request is made to the server"
        def result = getIncarnationDescriptions([item1, item2], "MANUAL")

        then: "the parameter object is returned"
        result.parameters.size() == 2
    }

    def "retrieve the apply info for a anotner client's unit"() {
        when: "trying to retrieve incarnation descriptions for other client's unit"
        get("/units/$unitSecondClient.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$item1.symbolicIdAsString", 404)

        then: "a client boundary violation is detected"
        thrown(ClientBoundaryViolationException)
    }

    def "retrieve the apply info of another client's catalog item"() {
        when: "trying to retrieve incarnation descriptions for another client's catalog item"
        get("/units/$unit.idAsString/domains/$domain3.idAsString/incarnation-descriptions?itemIds=${otherItem.symbolicIdAsString}", 404)

        then:
        thrown(NotFoundException)
    }

    def "retrieve the apply info for item3 and post"() {
        when: "a request is made to the server"
        def result = getIncarnationDescriptions([item3], "MANUAL")

        then: "the parameter object is returned"
        result.parameters.size() == 3

        when: "post the data"
        def postResult = postIncarnationDescriptions(result)

        then: "the 3 elements are created"
        postResult.size() == 3
    }

    def "retrieve the apply info for item4 and post"() {
        when: "a request is made to the server to create a c-3 element"
        def result = getIncarnationDescriptions([item3], "MANUAL")

        then: "it contains 3 elements to create, item1 item2 item3, because of the tailor references in item3"
        result.parameters.size() == 3

        when: "we create Item1/2/3"
        def postResult = postIncarnationDescriptions(result)
        def elementList = postResult.collect{it.targetUri}

        then: "3 objects are created"
        postResult.size() == 3

        when: "we apply item4, the process links two previously created controls"
        result = getIncarnationDescriptions([item4], "MANUAL")

        then: "the parameter object is returned and the links are set to item1 and item2"
        result.parameters.size() == 1
        result.parameters[0].references.size() == 2
        elementList.contains(result.parameters[0].references[0].referencedElement.targetUri)
        elementList.contains(result.parameters[0].references[1].referencedElement.targetUri)

        when: "post the data to create item4 with the unaltered links set to item1 and item2"
        postResult = postIncarnationDescriptions(result)

        then: "the parameter object is returned"
        postResult.size() == 1
        postResult[0].searchesUri == "http://localhost/processes/searches"

        when: "we get the created process"
        def processResult = parseJson(get(postResult[0].targetUri))

        then: "the process is created and linked with the controls created from item1 and item2"
        validateNewElementAgainstCatalogItem(processResult, item4, domain)
        processResult.owner.displayName == 'Test unit'
        processResult.links.link_to_item_1.domains.size() == 1
        processResult.links.link_to_item_2.domains.size() == 1
        elementList.contains(processResult.links.link_to_item_1.target.targetUri[0])
        elementList.contains(processResult.links.link_to_item_2.target.targetUri[0])
    }

    def "retrieve the apply info for item4, relink and post"() {
        given: "the created catalogitems c-1 c-2 c-3"
        def result = getIncarnationDescriptions([item3], "MANUAL")
        def postResult = postIncarnationDescriptions(result)
        def elementList = postResult.collect{it.targetUri}
        result = parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$item4.symbolicIdAsString&mode=MANUAL"))

        when: "post the data to create item4 with the altered links set to c-3"
        result.parameters[0].references[0].referencedElement.targetUri = elementList[0]
        result.parameters[0].references[1].referencedElement.targetUri = elementList[0]

        postResult = postIncarnationDescriptions(result)

        then: "the parameter object is returned"
        postResult.size() == 1
        postResult[0].searchesUri == "http://localhost/processes/searches"

        when: "we get the created process"
        def processResult = parseJson(get(postResult[0].targetUri))

        then: "the process is created and linked with c-3"
        processResult.links.link_to_item_1.target.targetUri[0] == elementList[0]
        processResult.links.link_to_item_2.target.targetUri[0] == elementList[0]
    }

    def "retrieve the apply info for item5 and post"() {
        when: "a request is made to the server to create a p2 element"
        def result = getIncarnationDescriptions([item5], "MANUAL")

        then: "it contains 1 elements to create, item5"
        result.parameters.size() == 1

        when: "we create Item5"
        def postResult = postIncarnationDescriptions(result)

        then: "1 objects are created"
        postResult.size() == 1

        when: "we get the created process"
        def processResult = parseJson(get(postResult[0].targetUri))

        then: "the process is created and the subtype is set"
        validateNewElementAgainstCatalogItem(processResult, item5, domain)
        processResult.owner.displayName == 'Test unit'
        processResult.domains[domain.id.uuidValue()].subType == "MY_SUBTYPE"
    }

    def "retrieve the apply info for item6 and post"() {
        given: "the created catalogitems and the control c1"
        def result = getIncarnationDescriptions([item1], "MANUAL")
        postIncarnationDescriptions(result)

        when: "a request is made to the server to create a p3-all-features element"
        result = getIncarnationDescriptions([item6], "MANUAL")

        then: "it contains 1 elements to create, item6"
        result.parameters.size() == 1

        when: "we create Item6"
        def postResult = postIncarnationDescriptions(result)

        then: "1 object is created"
        postResult.size() == 1

        when: "we get the created process"
        def processResult = parseJson(get(postResult[0].targetUri))

        then: "the process is created and all the features are set"
        validateNewElementAgainstCatalogItem(processResult, item6, domain)
        processResult.owner.displayName == 'Test unit'
        processResult.domains[domain.id.uuidValue()].subType == "MY_SUBTYPE"
        processResult.domains[domain.id.uuidValue()].status == "START"
        with(processResult) {
            customAspects.size() == 2
            with(customAspects) {
                process_resilience.attributes.size() == 1
                process_resilience.attributes.process_resilience_impact == "process_resilience_impact_low"
                process_processingDetails.attributes.size() == 2
                process_processingDetails.attributes.process_processingDetails_comment  == "my comment"
                process_processingDetails.attributes.process_processingDetails_operatingStage == "process_processingDetails_operatingStage_operation"
            }
        }
    }

    def "retrieve the apply info for item7 and post"() {
        given: "the created catalogitems and the control p1, also the linked controls"
        def incarnationDescriptions = getIncarnationDescriptions([item1, item2], "MANUAL")
        postIncarnationDescriptions(incarnationDescriptions)

        incarnationDescriptions = getIncarnationDescriptions([item4], "MANUAL")
        def processUri = postIncarnationDescriptions(incarnationDescriptions)[0].targetUri

        when: "a request is made to the server to create a TOM1 element"
        incarnationDescriptions = getIncarnationDescriptions([item7], "MANUAL")

        then: "it contains 1 element to create: item7"
        incarnationDescriptions.parameters.size() == 1

        when: "we create Item7"
        def postResult = postIncarnationDescriptions(incarnationDescriptions)

        then: "1 object is created"
        postResult.size() == 1
        postResult[0].name == item7.name
        with(postResult[0]) {
            designator != null
            id != null
        }

        when: "we get the created tom"
        def tomResult = parseJson(get(postResult[0].targetUri))

        then: "the tom is created and all the features are set"
        validateNewElementAgainstCatalogItem(tomResult, item7, domain)
        tomResult.owner.displayName == 'Test unit'
        tomResult.domains[domain.id.uuidValue()].subType == "CTL_TOM"
        tomResult.domains[domain.id.uuidValue()].status == "NEW1"

        when: "we get the linked process"
        def processResult = parseJson(get(processUri))

        then: "the process has a new link pointing to the created tom"
        processResult.links.size() == 3
        processResult.links.externallinktest.target.targetUri[0] == postResult[0].targetUri

        when: "we link to another created process (p3-all-features)"
        incarnationDescriptions = getIncarnationDescriptions([item6], "MANUAL")
        postResult = postIncarnationDescriptions(incarnationDescriptions)
        processUri = postResult[0].targetUri

        and: "we get the process"
        processResult = parseJson(get(processUri))

        then: "there is one link"
        processResult.links.size() == 1

        when: "we get the description for the tom"
        incarnationDescriptions = getIncarnationDescriptions([item7], "MANUAL")

        and: "we set the link to p3"
        incarnationDescriptions.parameters[0].references[0].referencedElement.targetUri = postResult[0].targetUri

        and:"create the tom and a link in p3"
        postResult = postIncarnationDescriptions(incarnationDescriptions)

        and: "we get the created tom"
        tomResult = parseJson(get(postResult[0].targetUri))

        then: "the tom is created and all the features are set"
        validateNewElementAgainstCatalogItem(tomResult, item7, domain)
        tomResult.owner.displayName == 'Test unit'
        tomResult.domains[domain.id.uuidValue()].subType == "CTL_TOM"

        when: "we get the linked process"
        processResult = parseJson(get(processUri))

        then: "the process has a new link pointing to the created tom"
        processResult.links.size() == 2
        processResult.links.externallinktest.target.targetUri[0] == postResult[0].targetUri
    }

    def "retrieve the apply info for item3 and post in other client's unit"() {
        when: "retrieving incarnation descriptions for item 3"
        def result = getIncarnationDescriptions([item3], "MANUAL")

        then: "the parameter object is returned"
        result.parameters.size() == 3

        when: "posting the incarnation descriptions in other client's unit"
        post("/units/$unitSecondClient.idAsString/incarnations",result, 404)

        then: "a client boundary violation is detected"
        thrown(ClientBoundaryViolationException)
    }

    def "retrieve the apply info for zz1 und zz2 post in one step"() {
        when: "a request is made to the server"
        def result = getIncarnationDescriptions([zz1, zz2], "MANUAL")

        then: "the parameter object is returned"
        result.parameters.size() == 2

        when: "post the data"
        def postResult = postIncarnationDescriptions(result)

        then: "the 2 elements are created"
        postResult.size() == 2

        def zz1Result = parseJson(get(postResult[0].targetUri))
        def zz2Result = parseJson(get(postResult[1].targetUri))

        validateNewElementAgainstCatalogItem(zz1Result, zz1, domain)
        validateNewElementAgainstCatalogItem(zz2Result, zz2, domain)

        with(zz1Result.links.link_to_zz2) {
            target.displayName[0].endsWith("zz2")
            with(attributes) {
                control_comment[0] == "another comment"
                control_operatingStage[0] == "Updated"
            }
        }
        with(zz2Result.links.link_to_zz1) {
            target.displayName[0].endsWith("zz1")
            with(attributes) {
                control_comment[0] == "comment of the link to zz1"
                control_another_attribute[0] == "test"
            }
        }

        when: "we get zz1 in resolve all mode"
        result = parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$zz1.symbolicIdAsString"))

        then: "only zz1 is included and the reference ist set to the corresponding element in the unit."
        result.parameters.size() == 1
        result.parameters[0].item.name == 'zz1'
        result.parameters[0].references[0].referencedElement.id == zz2Result.id

        when: "we get zz1 in resolve all mode but exclude links"
        result = parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$zz1.symbolicIdAsString&exclude=LINK"))

        then: "only zz1 is included"
        result.parameters.size() == 1

        when: "we get zz1 in resolve all mode but only include parts"
        result = parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$zz1.symbolicIdAsString&include=PART"))

        then: "only zz1 is included"
        result.parameters.size() == 1

        when: "we get zz1 in resolve manual mode"
        result = parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$zz1.symbolicIdAsString&mode=MANUAL"))

        then: "only zz1 is included and the reference ist set to the corresponding element in the unit."
        result.parameters.size() == 1
        result.parameters[0].references.first().referencedElement.targetUri == zz2Result._self
    }

    def "retrieve the apply info for zz1 und c1 post for an incomplete set"() {
        when: "a request is made to the server"
        def result = getIncarnationDescriptions([zz1, item1], "MANUAL")

        then: "the parameter object is returned"
        result.parameters.size() == 2

        when: "post the data"
        post("/units/$unit.idAsString/incarnations",result, 422)

        then: "the data is rejected"
        thrown(UnprocessableDataException)
    }

    def "retrieve the apply info for processImpactExample and post"() {
        when: "a request is made to the server to create a processImpactExample element"
        def result = getIncarnationDescriptions([processImpactExample], "MANUAL")

        then: "it contains 1 elements to create, processImpactExample"
        result.parameters.size() == 1

        when: "we create processImpactExample"
        def postResult = postIncarnationDescriptions(result)

        then: "1 objects are created"
        postResult.size() == 1

        when: "we get the created process"
        def processResult = parseJson(get(postResult[0].targetUri))

        then: "the process is created and the risk values are set"
        validateNewElementAgainstCatalogItem(processResult, processImpactExample, domain)
        processResult.owner.displayName == 'Test unit'
        processResult.domains[domain.id.uuidValue()].riskValues.id.potentialImpacts.C == 2
    }

    def "retrieve the apply info for scenarioProbabilityExample and post"() {
        when: "a request is made to the server to create a scenarioProbabilityExample element"
        def result = getIncarnationDescriptions([scenarioProbabilityExample])

        then: "it contains 2 elements to create, scenarioProbabilityExample and the linked control"
        result.parameters.size() == 2

        when: "we create scenarioProbabilityExample and the linked control"
        def postResult = postIncarnationDescriptions(result)

        then: "2 objects are created"
        postResult.size() == 2

        when: "we get the created scenario and the control"
        def scenarioResult = parseJson(get(postResult[0].targetUri))
        def controlResult = parseJson(get(postResult[1].targetUri))

        then: "the control is created and the risk values are set"
        validateNewElementAgainstCatalogItem(controlResult, controlImpactExample, domain)
        controlResult.domains[domain.id.uuidValue()].riskValues.id.implementationStatus == 1

        and: "the scenario is created and the risk values are set"
        validateNewElementAgainstCatalogItem(scenarioResult, scenarioProbabilityExample, domain)
        scenarioResult.owner.displayName == 'Test unit'
        scenarioResult.domains[domain.id.uuidValue()].riskValues.id.potentialProbability == 3
    }

    def "retrieve the apply info for part and composite and post"() {
        when: "requesting incarnation descriptions for a composite and part"
        def result = getIncarnationDescriptions([itemComposite, itemPart], "MANUAL")

        then: "it contains 2 elements to create, part and composite"
        result.parameters.size() == 2

        when: "we create both"
        def postResult = postIncarnationDescriptions(result)

        then: "2 objects are created"
        postResult.size() == 2

        when: "we get the created scenario"
        def composite = parseJson(get(postResult[0].targetUri))
        def part = parseJson(get(postResult[1].targetUri))

        then: "the element is created and the part is set"
        validateNewElementAgainstCatalogItem(composite, itemComposite, domain)
        validateNewElementAgainstCatalogItem(part, itemPart, domain)
        composite.parts.size() == 1
        composite.parts[0].targetUri == part._self
    }

    def "retrieve the apply info for part and post"() {
        when: "fetching incarnation descriptions for a part"
        def getResult = getIncarnationDescriptions([itemPart], "MANUAL")

        then: "it contains 1 elements to create"
        getResult.parameters.size() == 1

        when: "we post an incomplete description"
        post("/units/$unit.idAsString/incarnations",getResult, 422)

        then: "can not apply"
        def upEx = thrown(UnprocessableDataException)
        upEx.message == "CatalogItem itemComposite not included in request but required by itemPart."

        when: "we create a control"
        getResult = getIncarnationDescriptions([item1], "MANUAL")
        def postResult = postIncarnationDescriptions(getResult)
        def compositeControlUri = postResult[0].targetUri

        and: "link the composite reference"
        getResult = getIncarnationDescriptions([itemPart], "MANUAL")
        getResult.parameters.references[0].first().put("referencedElement", ["targetUri": compositeControlUri])
        postResult = postIncarnationDescriptions(getResult)

        then:
        postResult.size() == 1

        when:
        def part = parseJson(get(postResult[0].targetUri))
        def composite = parseJson(get(compositeControlUri))

        then: "the element is created and the part is set"
        validateNewElementAgainstCatalogItem(part, itemPart, domain)
        composite.parts.size() == 1
        composite.parts[0].targetUri == part._self

        when: "we create the composite and link"
        getResult = getIncarnationDescriptions([itemComposite], "MANUAL")
        getResult.parameters.references[0].first().put("referencedElement", Map.of("targetUri", compositeControlUri))
        postResult = postIncarnationDescriptions(getResult)
        def newComposite = parseJson(get(postResult[0].targetUri))

        then:
        postResult.size() == 1
        validateNewElementAgainstCatalogItem(newComposite, itemComposite, domain)
        newComposite.parts[0].targetUri == composite._self

        when: "we get itemPart in resolve all mode"
        def result = parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$itemPart.symbolicIdAsString"))

        then: "only the part is included with a reference to the existing composite element"
        result.parameters.size() == 1
        result.parameters[0].item.displayName == "itemPart"
        result.parameters[0].references[0].referencedElement.displayName ==~ /CTL-\d+ itemComposite/

        when: "we get itemPart in resolve all mode but exclude links"
        result = parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$itemPart.symbolicIdAsString&exclude=LINK"))

        then: "only the part is included with a reference to the existing composite element"
        result.parameters.size() == 1
        result.parameters[0].item.displayName == "itemPart"
        result.parameters[0].references[0].referencedElement.displayName ==~ /CTL-\d+ itemComposite/

        when: "we get itemPart in resolve all mode but exclude composite"
        result = parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$itemPart.symbolicIdAsString&exclude=COMPOSITE"))

        then: "one is included without reference"
        result.parameters.size() == 1
        result.parameters[0].item.displayName  =="itemPart"
        result.parameters[0].references.size() == 0

        when: "we get itemPart in manual mode but exclude composite"
        result = parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$itemPart.symbolicIdAsString&exclude=COMPOSITE&mode=MANUAL"))

        then: "one is included without reference"
        result.parameters.size() == 1
        result.parameters[0].item.displayName  =="itemPart"
        result.parameters[0].references.size() == 0

        when: "we get itemComposite in resolve all mode but exclude links"
        result = parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$itemComposite.symbolicIdAsString&exclude=LINK"))

        then: "only the composite is included with a reference to the existing part element"
        result.parameters.size() == 1
        result.parameters[0].item.displayName == "itemComposite"
        result.parameters[0].references[0].referencedElement.displayName ==~ /CTL-\d+ itemPart/

        when: "we get itemComposite in resolve all mode but exclude PART"
        result = parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$itemComposite.symbolicIdAsString&exclude=PART"))

        then: "one is included without reference"
        result.parameters.size() == 1
        result.parameters[0].item.displayName  =="itemComposite"
        result.parameters[0].references.size() == 0

        when: "we get itemComposite in manual mode but exclude PART"
        result = parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$itemComposite.symbolicIdAsString&exclude=PART&mode=MANUAL"))

        then: "one is included without reference"
        result.parameters.size() == 1
        result.parameters[0].item.displayName  =="itemComposite"
        result.parameters[0].references.size() == 0
    }

    def "retrieve the apply info for part linked to existing composite"() {
        when: "we create the composite by removing the part reference"
        def incarnationDescriptions = getIncarnationDescriptions([itemComposite], "MANUAL")
        incarnationDescriptions.parameters.first().references.clear()

        and: "post"
        def elementRefs = parseJson(post("/units/$unit.idAsString/incarnations",incarnationDescriptions, 201))

        then: "the composite is created"
        elementRefs.size() == 1

        when: "we create the part in default mode"
        incarnationDescriptions = getIncarnationDescriptions([itemPart])

        then: "the parameter is set to the existing composite"
        incarnationDescriptions.parameters.size() == 1
        incarnationDescriptions.parameters.first().references.size() == 1
        incarnationDescriptions.parameters.first().references.first().referencedElement.targetUri == elementRefs.targetUri.first()
    }

    def "retrieve the apply info for part linked to existing composite in manual mode"() {
        when: "we create the composite by removing the part reference"
        def incarnationDescriptions = getIncarnationDescriptions([itemComposite], "MANUAL")
        incarnationDescriptions.parameters.first().references.clear()

        and: "post"
        def elementRefs = parseJson(post("/units/$unit.idAsString/incarnations",incarnationDescriptions, 201))

        then: "the composite is created"
        elementRefs.size() == 1

        when: "we create the part in manual mode"
        incarnationDescriptions = getIncarnationDescriptions([itemPart], "MANUAL")

        then: "the parameter is set to the existing composite"
        incarnationDescriptions.parameters.size() == 1;
        incarnationDescriptions.parameters.first().references.size() == 1
        incarnationDescriptions.parameters.first().references.first().referencedElement.targetUri == elementRefs.targetUri.first()
    }

    def "retrieve the apply info for composite linked to existing part"() {
        when: "we create the part by removing the composite reference"
        def incarnationDescriptions = getIncarnationDescriptions([itemPart], "MANUAL")
        incarnationDescriptions.parameters.first().references.clear()

        and: "post"
        def elementRef = parseJson(post("/units/$unit.idAsString/incarnations",incarnationDescriptions, 201))

        then: "the part is created"
        elementRef.size() == 1

        when: "we create the composite"
        incarnationDescriptions = getIncarnationDescriptions([itemComposite], "MANUAL")

        then: "the parameter is set to the existing part"
        incarnationDescriptions.parameters.size() == 1;
        incarnationDescriptions.parameters.first().references.size() == 1
        incarnationDescriptions.parameters.first().references.first().referencedElement.targetUri == elementRef.targetUri.first()
    }

    def "reapply in normal mode"() {
        when: "creating incarnation descriptions for a composite with part"
        def incarnationDescriptions = getIncarnationDescriptions([itemComposite], "DEFAULT", "ALWAYS")

        and: "post"
        def elementRef = parseJson(post("/units/$unit.idAsString/incarnations",incarnationDescriptions, 201))

        then: "the part is created"
        elementRef.size() == 2

        when: "we delete the composite part"
        def compositeUri = elementRef[0].targetUri
        delete(elementRef[1].targetUri)

        then: "the part is gone"
        with(parseJson(get(compositeUri))) {
            name == 'itemComposite'
            parts.size() == 0
        }

        when:"creating incarnation descriptions using existing incarnations"
        incarnationDescriptions = getIncarnationDescriptions([itemComposite], "DEFAULT", "ALWAYS")

        then: "the parameter is set to the existing part"
        incarnationDescriptions.parameters.size() == 1
        incarnationDescriptions.parameters.first().references.size() == 1

        when: "incarnating"
        elementRef = parseJson(post("/units/$unit.idAsString/incarnations",incarnationDescriptions, 201))

        then: "the part is created"
        with(parseJson(get(elementRef[0].targetUri))) {
            elementRef.size() == 1
            name == 'itemPart'
        }

        and: "the reference is restored"
        with(parseJson(get(compositeUri))) {
            name == 'itemComposite'
            parts.size() == 1
            parts[0].name == 'itemPart'
        }

        when:"all parts are present and we create incarnation descriptions using existing incarnations"
        incarnationDescriptions = getIncarnationDescriptions([itemComposite], "DEFAULT", "ALWAYS")

        then: "nothing would be incarnated"
        incarnationDescriptions.parameters.size() == 0;
    }

    def "apply item with scope"() {
        when: "applying an item that has a scope reference"
        def memberUri = postIncarnationDescriptions(getIncarnationDescriptions([itemMember]))[0].targetUri

        and: "fetching the scopes"
        def scopes = parseJson(get("/scopes")).items

        then: "the scope from the catalog has been applied"
        scopes.size() == 1
        scopes[0].name == "itemScope"

        and: "it references the created member"
        scopes[0].members.size() == 1
        scopes[0].members[0].name == "itemMember"
        scopes[0].members[0].targetUri == memberUri
    }

    def "apply processImpactExample"() {
        when: "applying process and control with scenario"
        def processUri = postIncarnationDescriptions(getIncarnationDescriptions([processImpactExample]))[0].targetUri
        def result = postIncarnationDescriptions(getIncarnationDescriptions([controlImpactExample]))
        def controlUri = result[0].targetUri
        def scenarioUri = result[1].targetUri

        and: "fetching the process"
        def process = parseJson(get(processUri))
        def control = parseJson(get(controlUri))

        then: "the process from the catalog has been applied"
        process.name == "processImpactExample"

        when: "we delete the scenario"
        delete(scenarioUri)
        and : "we reapply the control"
        result = postIncarnationDescriptions(getIncarnationDescriptions([controlImpactExample],"DEFAULT","ALWAYS"))

        then: "the scenario is restored"
        result.size() == 1
        result[0].name == "scenarioProbabilityExample"
    }

    def "apply processImpact without scenario"() {
        when: "incarnating a process"
        def processUri = postIncarnationDescriptions(getIncarnationDescriptions([processImpactExample]))[0].targetUri

        and:"create the control without scenario"
        def result = postIncarnationDescriptions(
                parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$controlImpactExample.symbolicIdAsString&exclude=LINK"))
                )

        def controlUri = result[0].targetUri

        and: "fetching the process"
        def process = parseJson(get(processUri))
        def control = parseJson(get(controlUri))

        then: "the process from the catalog has been applied"
        process.name == "processImpactExample"

        when : "we reapply the item member"
        result = postIncarnationDescriptions(getIncarnationDescriptions([controlImpactExample],"DEFAULT","ALWAYS"))

        then: "the scenario is created"
        result.size() == 1
        result[0].name == "scenarioProbabilityExample"
    }

    @Ignore @Issue('verinice-veo#2969')
    def "apply processImpact serveral control incarnations"() {
        when: "incarnating a process"
        def processUri = postIncarnationDescriptions(getIncarnationDescriptions([processImpactExample]))[0].targetUri

        and:"create the control without scenario"
        3.times {
            postIncarnationDescriptions(
                    parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$controlImpactExample.symbolicIdAsString&exclude=LINK"))
                    )
        }

        and: "fetching the process"
        def process = parseJson(get(processUri))

        then: "the process from the catalog has been applied"
        process.name == "processImpactExample"

        when : "we reapply the control"
        def result = postIncarnationDescriptions(getIncarnationDescriptions([controlImpactExample],"DEFAULT","ALWAYS"))
        def scenario = parseJson(get(result[0].targetUri))

        def controls = parseJson(get("/domains/${domain.id.uuidValue()}/controls"))

        then: "the scenario is created"
        result.size() == 1
        result[0].name == "scenarioProbabilityExample"

        and: "the link is in all controls restored"
        controls.items.size() == 3
        controls.items[0].links.control_relevantAppliedThreat[0].target.name == result[0].name
        controls.items[1].links.control_relevantAppliedThreat[0].target.name == result[0].name
        controls.items[2].links.control_relevantAppliedThreat[0].target.name == result[0].name
    }

    def "threat overview"() {
        when: "incarnating a process"
        def processUri = postIncarnationDescriptions(getIncarnationDescriptions([processImpactExample]))[0].targetUri

        and:"create the control without scenario"
        def result = postIncarnationDescriptions(
                parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$controlImpactExample.symbolicIdAsString&exclude=LINK"))
                )

        def controlUri = result[0].targetUri

        and: "fetching the process"
        def process = parseJson(get(processUri))
        def control = parseJson(get(controlUri))

        then: "the process from the catalog has been applied"
        process.name == "processImpactExample"

        when: "we add a controlImplementation to the process"
        get("/domains/${domain.id.uuidValue()}/processes/${process.id}").with{
            def body = parseJson(it)
            body.controlImplementations= [
                [
                    control: [targetUri: (controlUri)],
                    description: "no reasons",
                ]
            ]
            put(body._self, body, ["If-Match": getETag(it)])
        }

        and : "we execute the threatOverview action"
        result = parseJson(post("/domains/${domain.id.uuidValue()}/processes/${process.id}/actions/threatOverview/execution",null, 200))

        then: "the scenario and risk is created"
        result.createdEntities.size() == 2
        result.createdEntities.designator ==~ ["RSK-1", "SCN-1"]

        when:"we get the control again"
        control = parseJson(get(controlUri))

        then: "the control is now linked to the scenario"
        control.links.control_relevantAppliedThreat[0].target.name == "scenarioProbabilityExample"
    }

    def "threat overview and existing scenario"() {
        when: "applying a process, an unlinked control and a scenario"
        def processUri = postIncarnationDescriptions(getIncarnationDescriptions([processImpactExample]))[0].targetUri

        def controlUri = postIncarnationDescriptions(
                parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$controlImpactExample.symbolicIdAsString&exclude=LINK"))
                )[0].targetUri
        def scenarioUri = postIncarnationDescriptions(
                parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$scenarioProbabilityExample.symbolicIdAsString&exclude=LINK_EXTERNAL"))
                )[0].targetUri

        and: "fetching the elements"
        def process = parseJson(get(processUri))
        def control = parseJson(get(controlUri))
        def scenario = parseJson(get(scenarioUri))

        then: "the control is not linked"
        control.links.size() == 0

        when: "we add a controlImplementation to the process"
        get("/domains/${domain.id.uuidValue()}/processes/${process.id}").with{
            def body = parseJson(it)
            body.controlImplementations= [
                [
                    control: [targetUri: (controlUri)],
                    description: "no reasons",
                ]
            ]
            put(body._self, body, ["If-Match": getETag(it)])
        }

        and : "we execute the threatOverview action"
        def result = parseJson(post("/domains/${domain.id.uuidValue()}/processes/${process.id}/actions/threatOverview/execution",null, 200))

        then: "the risk is created"
        result.createdEntities.size() == 1
        result.createdEntities[0].designator == "RSK-1"

        when:"we get the control again"
        control = parseJson(get(controlUri))

        then: "the control is now linked to the scenario"
        control.links.control_relevantAppliedThreat[0].target.name == scenario.name
    }

    def "threat overview two processes"() {
        when: "incarnating a process"
        def processUri = postIncarnationDescriptions(getIncarnationDescriptions([processImpactExample]))[0].targetUri

        and:"create the control without scenario"
        def result = postIncarnationDescriptions(
                parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$controlImpactExample1.symbolicIdAsString&exclude=LINK"))
                )

        def controlUri1 = result[0].targetUri

        and: "fetching the process"
        def process = parseJson(get(processUri))

        then: "the process from the catalog has been applied"
        process.name == "processImpactExample"

        when: "we add a controlImplementation to the process"
        get("/domains/${domain.id.uuidValue()}/processes/${process.id}").with{
            def body = parseJson(it)
            body.controlImplementations= [
                [
                    control: [targetUri: (controlUri1)],
                    description: "no reasons",
                ]
            ]
            put(body._self, body, ["If-Match": getETag(it)])
        }

        and : "we execute the threatOverview action"
        result = parseJson(post("/domains/${domain.id.uuidValue()}/processes/${process.id}/actions/threatOverview/execution",null, 200))

        then: "the scenario and risk are created"
        result.createdEntities.size() == 2
        result.createdEntities.designator ==~ ["RSK-1", "SCN-1"]

        when:"we get the control again"
        def control = parseJson(get(controlUri1))

        then: "the control is now linked to the scenario"
        control.links.control_relevantAppliedThreat[0].target.name == "scenarioProbabilityExample2"

        when: "we create a new process"
        processUri = postIncarnationDescriptions(getIncarnationDescriptions([item4]))[0].targetUri
        process = parseJson(get(processUri))

        and: "another control linked with another scenario"
        def controlUri2 = postIncarnationDescriptions(
                parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$controlImpactExample2.symbolicIdAsString&exclude=LINK"))
                )[0].targetUri

        and: "we add a controlImplementation to the process"
        get("/domains/${domain.id.uuidValue()}/processes/${process.id}").with{
            def body = parseJson(it)
            body.controlImplementations= [
                [
                    control: [targetUri: (controlUri1)],
                    description: "no reasons",
                ],
                [
                    control: [targetUri: (controlUri2)],
                    description: "no reasons",
                ]
            ]
            put(body._self, body, ["If-Match": getETag(it)])
        }

        and : "we execute the threatOverview action"
        result = parseJson(post("/domains/${domain.id.uuidValue()}/processes/${process.id}/actions/threatOverview/execution",null, 200))

        then: "the risks are created and the missing scenario"
        result.createdEntities.size() == 3
        result.createdEntities.designator ==~ ["RSK-3", "RSK-2", "SCN-2"]

        when:"we get the control again"
        control = parseJson(get(controlUri1))
        def control1 = parseJson(get(controlUri2))

        then: "the control is now linked to the scenario"
        control.links.control_relevantAppliedThreat[0].target.name == "scenarioProbabilityExample2"
        control1.links.control_relevantAppliedThreat.target.name ==~ [
            'scenarioProbabilityExample1',
            'scenarioProbabilityExample2'
        ]
    }

    @Ignore @Issue('verinice-veo#852')
    def "apply processImpact and existing scenario"() {
        when: "applying a process, an unlinked control and a scenario"
        def processUri = postIncarnationDescriptions(getIncarnationDescriptions([processImpactExample]))[0].targetUri

        def result = postIncarnationDescriptions(
                parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$controlImpactExample.symbolicIdAsString&exclude=LINK"))
                )
        def scenarioUri = postIncarnationDescriptions(
                parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=$scenarioProbabilityExample.symbolicIdAsString&exclude=LINK_EXTERNAL"))
                )[0].targetUri

        and: "fetching the control"
        def control = parseJson(get(result[0].targetUri))

        then: "the control is created but not the scenario"
        result.size() == 1
        control.name == "zzzzc-impact"

        and: "the the control is not linked"
        control.links.size() == 0

        when : "we reapply the control"
        result = postIncarnationDescriptions(getIncarnationDescriptions([controlImpactExample],"DEFAULT","ALWAYS"))
        control = parseJson(get(result[0].targetUri))
        def scenario = parseJson(get(scenarioUri))

        then: "the scenario is not restored"
        result.size() == 1
        control.links.control_relevantAppliedThreat[0].target.name == scenario.name
    }

    private getIncarnationDescriptions(Collection<CatalogItem> items, String mode = "DEFAULT",  String useExistingIncarnations="FOR_REFERENCED_ITEMS") {
        parseJson(get("/units/$unit.idAsString/domains/$domain.idAsString/incarnation-descriptions?itemIds=${items.collect{it.symbolicIdAsString}.join(',')}&mode=$mode&useExistingIncarnations=$useExistingIncarnations"))
    }

    private postIncarnationDescriptions(incarnationDescriptions) {
        parseJson(post("/units/$unit.idAsString/incarnations",incarnationDescriptions))
    }

    private validateNewElementAgainstCatalogItem(element, CatalogItem catalogItem, Domain domain) {
        verifyAll(catalogItem) {
            it.name == element.name
            it.abbreviation == element.abbreviation
            it.description == element.description
            it.customAspects.size() == element.customAspects.size()
        }
        verifyAll(element) {
            it.domains.size() == 1
            it.domains[domain.id.uuidValue()] != null
        }
        element.links.each {
            assert it.value.domains.size() == 1
            assert it.value.domains[0].targetUri[0].endsWith(domain.id.uuidValue())
        }
        element.customAspects.each {
            assert it.value.domains.size() == 1
            assert it.value.domains[0].targetUri.endsWith(domain.id.uuidValue())
        }
        true
    }
}
