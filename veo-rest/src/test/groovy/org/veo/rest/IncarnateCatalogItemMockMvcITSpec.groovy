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
import org.veo.core.entity.Unit
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.exception.UnprocessableDataException
import org.veo.core.entity.specification.ClientBoundaryViolationException

import spock.lang.Ignore

class IncarnateCatalogItemMockMvcITSpec extends CatalogSpec {
    def basePath ="units"

    @WithUserDetails("user@domain.example")
    def "retrieve the apply info "() {
        given: "the created catalogitems"

        when: "a request is made to the server"
        def result = getIncarnationDescriptions(unit,item1)

        then: "the parameter object is returned"
        result.parameters.size() == 1
    }

    @WithUserDetails("user@domain.example")
    def "retrieve the apply info for two items"() {
        given: "the created catalogitems"

        when: "a request is made to the server"
        def result = getIncarnationDescriptions(unit,item1, item2)

        then: "the parameter object is returned"
        result.parameters.size() == 2
    }

    @WithUserDetails("user@domain.example")
    def "retrieve the apply info for a anotner client's unit"() {
        given: "the created catalogitems"

        when: "trying to retrieve incarnation descriptions for other client's unit"
        get("/${basePath}/${unitSecondClient.id.uuidValue()}/incarnations?itemIds=${item1.id.uuidValue()}", 404)

        then: "a client boundary violation is detected"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "retrieve the apply info of another client's catalog item"() {
        given: "the created catalog items"

        when: "trying to retrieve incarnation descriptions for another client's catalog item"
        get("/${basePath}/${unit.id.uuidValue()}/incarnations?itemIds=${otherItem.id.uuidValue()}", 404)

        then: "a client boundary violation is detected"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "retrieve the apply info for item3 and post"() {
        given: "the created catalogitems"

        when: "a request is made to the server"
        def result = getIncarnationDescriptions(unit,item3)

        then: "the parameter object is returned"
        result.parameters.size() == 3

        when: "post the data"
        def postResult = postIncarnationDescriptions(unit,result)

        then: "the 3 elements are created"
        postResult.size() == 3
    }

    @WithUserDetails("user@domain.example")
    def "retrieve the apply info for item4 and post"() {
        given: "the created catalogitems"

        when: "a request is made to the server to create a c-3 element"
        def result = getIncarnationDescriptions(unit,item3)

        then: "it contains 3 elements to create, item1 item2 item3, because of the tailor references in item3"
        result.parameters.size() == 3

        when: "we create Item1/2/3"
        def postResult = postIncarnationDescriptions(unit,result)
        def elementList = postResult.collect{it.targetUri}

        then: "3 objects are created"
        postResult.size() == 3

        when: "we apply item4, the process links two previously created controls"
        result = getIncarnationDescriptions(unit,item4)

        then: "the parameter object is returned and the links are set to item1 and item2"
        result.parameters.size() == 1
        result.parameters[0].references.size() == 2
        elementList.contains(result.parameters[0].references[0].referencedElement.targetUri)
        elementList.contains(result.parameters[0].references[1].referencedElement.targetUri)

        when: "post the data to create item4 with the unaltered links set to item1 and item2"
        postResult = postIncarnationDescriptions(unit,result)

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

    @WithUserDetails("user@domain.example")
    def "retrieve the apply info for item4, relink and post"() {
        given: "the created catalogitems c-1 c-2 c-3"
        def result = getIncarnationDescriptions(unit,item3)
        def postResult = postIncarnationDescriptions(unit,result)
        def elementList = postResult.collect{it.targetUri}
        result = parseJson(get("/${basePath}/${unit.id.uuidValue()}/incarnations?itemIds=${item4.id.uuidValue()}"))

        when: "post the data to create item4 with the altered links set to c-3"
        result.parameters[0].references[0].referencedElement.targetUri = elementList[0]
        result.parameters[0].references[1].referencedElement.targetUri = elementList[0]

        postResult = postIncarnationDescriptions(unit,result)

        then: "the parameter object is returned"
        postResult.size() == 1
        postResult[0].searchesUri == "http://localhost/processes/searches"

        when: "we get the created process"
        def processResult = parseJson(get(postResult[0].targetUri))

        then: "the process is created and linked with c-3"
        processResult.links.link_to_item_1.target.targetUri[0] == elementList[0]
        processResult.links.link_to_item_2.target.targetUri[0] == elementList[0]
    }

    @WithUserDetails("user@domain.example")
    def "retrieve the apply info for item5 and post"() {
        given: "the created catalogitems"

        when: "a request is made to the server to create a p2 element"
        def result = getIncarnationDescriptions(unit,item5)

        then: "it contains 1 elements to create, item5"
        result.parameters.size() == 1

        when: "we create Item5"
        def postResult = postIncarnationDescriptions(unit,result)

        then: "1 objects are created"
        postResult.size() == 1

        when: "we get the created process"
        def processResult = parseJson(get(postResult[0].targetUri))

        then: "the process is created and the subtype is set"
        validateNewElementAgainstCatalogItem(processResult, item5, domain)
        processResult.owner.displayName == 'Test unit'
        processResult.domains[domain.id.uuidValue()].subType == "MY_SUBTYPE"
    }

    @WithUserDetails("user@domain.example")
    def "retrieve the apply info for item6 and post"() {
        given: "the created catalogitems and the control c1"
        def result = getIncarnationDescriptions(unit,item1)
        postIncarnationDescriptions(unit,result)

        when: "a request is made to the server to create a p3-all-features element"
        result = getIncarnationDescriptions(unit,item6)

        then: "it contains 1 elements to create, item6"
        result.parameters.size() == 1

        when: "we create Item6"
        def postResult = postIncarnationDescriptions(unit,result)

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

    @WithUserDetails("user@domain.example")
    def "retrieve the apply info for item7 and post"() {
        given: "the created catalogitems and the control p1, also the linked controls"
        def incarnationDescriptions = getIncarnationDescriptions(unit,item1,item2)
        postIncarnationDescriptions(unit,incarnationDescriptions)

        incarnationDescriptions = getIncarnationDescriptions(unit,item4)
        def processUri = postIncarnationDescriptions(unit,incarnationDescriptions)[0].targetUri

        when: "a request is made to the server to create a TOM1 element"
        incarnationDescriptions = getIncarnationDescriptions(unit,item7)

        then: "it contains 1 element to create: item7"
        incarnationDescriptions.parameters.size() == 1

        when: "we create Item7"
        def postResult = postIncarnationDescriptions(unit,incarnationDescriptions)

        then: "1 object is created"
        postResult.size() == 1

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
        incarnationDescriptions = getIncarnationDescriptions(unit,item6)
        postResult = postIncarnationDescriptions(unit,incarnationDescriptions)
        processUri = postResult[0].targetUri

        and: "we get the process"
        processResult = parseJson(get(processUri))

        then: "there is one link"
        processResult.links.size() == 1

        when: "we get the description for the tom"
        incarnationDescriptions = getIncarnationDescriptions(unit,item7)

        and: "we set the link to p3"
        incarnationDescriptions.parameters[0].references[0].referencedElement.targetUri = postResult[0].targetUri

        and:"create the tom and a link in p3"
        postResult = postIncarnationDescriptions(unit,incarnationDescriptions)

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

    @WithUserDetails("user@domain.example")
    def "retrieve the apply info for item3 and post in other client's unit"() {
        given: "the created catalogitems"

        when: "retrieving incarnation descriptions for item 3"
        def result = getIncarnationDescriptions(unit,item3)

        then: "the parameter object is returned"
        result.parameters.size() == 3

        when: "posting the incarnation descriptions in other client's unit"
        post("/${basePath}/${unitSecondClient.id.uuidValue()}/incarnations",result, 404)

        then: "a client boundary violation is detected"
        thrown(ClientBoundaryViolationException)
    }

    @WithUserDetails("user@domain.example")
    def "retrieve the apply info for zz1 und zz2 post in one step"() {
        given: "the created catalogitems"

        when: "a request is made to the server"
        def result = getIncarnationDescriptions(unit,zz1,zz2)

        then: "the parameter object is returned"
        result.parameters.size() == 2

        when: "post the data"
        def postResult = postIncarnationDescriptions(unit,result)

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
    }

    @WithUserDetails("user@domain.example")
    def "retrieve the apply info for zz1 und c1 post for an incomplete set"() {
        given: "the created catalogitems"

        when: "a request is made to the server"
        def result = getIncarnationDescriptions(unit,zz1,item1)

        then: "the parameter object is returned"
        result.parameters.size() == 2

        when: "post the data"
        post("/${basePath}/${unit.id.uuidValue()}/incarnations",result, 422)

        then: "the data is rejected"
        thrown(UnprocessableDataException)
    }

    @Ignore("VEO-2285")
    @WithUserDetails("user@domain.example")
    def "retrieve the apply info for processImpactExample and post"() {
        given: "the created catalogitems"

        when: "a request is made to the server to create a processImpactExample element"
        def result = getIncarnationDescriptions(unit,processImpactExample)

        then: "it contains 1 elements to create, processImpactExample"
        result.parameters.size() == 1

        when: "we create processImpactExample"
        def postResult = postIncarnationDescriptions(unit,result)

        then: "1 objects are created"
        postResult.size() == 1

        when: "we get the created process"
        def processResult = parseJson(get(postResult[0].targetUri))

        then: "the process is created and the risk values are set"
        validateNewElementAgainstCatalogItem(processResult, processImpactExample, domain)
        processResult.owner.displayName == 'Test unit'
        processResult.domains[domain.id.uuidValue()].riskValues.id.potentialImpacts.C == 2
    }

    @Ignore("VEO-2285")
    @WithUserDetails("user@domain.example")
    def "retrieve the apply info for controlImpactExample and post"() {
        given: "the created catalogitems"

        when: "a request is made to the server to create a controlImpactExample element"
        def result = getIncarnationDescriptions(unit,controlImpactExample)

        then: "it contains 1 elements to create, controlImpactExample"
        result.parameters.size() == 1

        when: "we create controlImpactExample"
        def postResult = postIncarnationDescriptions(unit,result)

        then: "1 objects are created"
        postResult.size() == 1

        when: "we get the created Control"
        def controlResult = parseJson(get(postResult[0].targetUri))

        then: "the control is created and the risk values are set"
        validateNewElementAgainstCatalogItem(controlResult, controlImpactExample, domain)
        controlResult.owner.displayName == 'Test unit'
        controlResult.domains[domain.id.uuidValue()].riskValues.id.implementationStatus == 1
    }

    @Ignore("VEO-2285")
    @WithUserDetails("user@domain.example")
    def "retrieve the apply info for scenarioProbabilityExample and post"() {
        given: "the created catalogitems"

        when: "a request is made to the server to create a scenarioProbabilityExample element"
        def result = getIncarnationDescriptions(unit,scenarioProbabilityExample)

        then: "it contains 1 elements to create, scenarioProbabilityExample"
        result.parameters.size() == 1

        when: "we create scenarioProbabilityExample"
        def postResult = postIncarnationDescriptions(unit,result)

        then: "1 objects are created"
        postResult.size() == 1

        when: "we get the created scenario"
        def scenarioResult = parseJson(get(postResult[0].targetUri))

        then: "the scenario is created and the risk values are set"
        validateNewElementAgainstCatalogItem(scenarioResult, scenarioProbabilityExample, domain)
        scenarioResult.owner.displayName == 'Test unit'
        scenarioResult.domains[domain.id.uuidValue()].riskValues.id.potentialProbability == 3
    }

    @WithUserDetails("user@domain.example")
    def "retrieve the apply info for part and composite and post"() {
        given: "the created catalogitems"

        when: "requesting incarnation descriptions for a composite and part"
        def result = getIncarnationDescriptions(unit,itemComposite, itemPart)

        then: "it contains 2 elements to create, part and composite"
        result.parameters.size() == 2

        when: "we create both"
        def postResult = postIncarnationDescriptions(unit,result)

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

    @WithUserDetails("user@domain.example")
    def "retrieve the apply info for part and post"() {
        given: "the created catalogitems"

        when: "fetching incarnation descriptions for a part"
        def getResult = getIncarnationDescriptions(unit, itemPart)

        then: "it contains 1 elements to create"
        getResult.parameters.size() == 1

        when: "we post an incomplete description"
        post("/${basePath}/${unit.id.uuidValue()}/incarnations",getResult, 422)

        then: "can not apply"
        def upEx = thrown(UnprocessableDataException)
        upEx.message == "CatalogItem null:zzzzzComposite not included in request but required by CTL-1:zzzzzPart."

        when: "we create a control"
        getResult = getIncarnationDescriptions(unit, item1)
        def postResult = postIncarnationDescriptions(unit,getResult)
        def compositeControlUri = postResult[0].targetUri

        and: "link the composite reference"
        getResult = getIncarnationDescriptions(unit, itemPart)
        getResult.parameters.references[0].first().put("referencedElement", ["targetUri": compositeControlUri])
        postResult = postIncarnationDescriptions(unit,getResult)

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
        getResult = getIncarnationDescriptions(unit, itemComposite)
        getResult.parameters.references[0].first().put("referencedElement", Map.of("targetUri", compositeControlUri))
        postResult = postIncarnationDescriptions(unit,getResult)
        def newComposite = parseJson(get(postResult[0].targetUri))

        then:
        postResult.size() == 1
        validateNewElementAgainstCatalogItem(newComposite, itemComposite, domain)
        newComposite.parts[0].targetUri == composite._self
    }

    private getIncarnationDescriptions(Unit unit, CatalogItem... items) {
        parseJson(get("/${basePath}/${unit.id.uuidValue()}/incarnations?itemIds=${items.collect{it.id.uuidValue()}.join(',')}"))
    }

    private postIncarnationDescriptions(Unit unit, incarnationDescriptions) {
        parseJson(post("/${basePath}/${unit.id.uuidValue()}/incarnations",incarnationDescriptions))
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
