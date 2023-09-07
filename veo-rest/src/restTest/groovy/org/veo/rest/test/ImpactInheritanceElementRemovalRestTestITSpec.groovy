/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Alexander Koderman
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
package org.veo.rest.test

import groovy.util.logging.Slf4j
import spock.lang.Unroll

@Slf4j
class ImpactInheritanceElementRemovalRestTestITSpec extends VeoRestTest {

    String unitId
    List<String> assetIDs

    def setup() {
        unitId = postNewUnit().resourceId
    }

    @Unroll
    // Test impact inheritance in this graph:
    // ```mermaid
    //graph TD
    //    A(Asset 0) -->|asset_asset_app| B(Asset 1)
    //    B -->|asset_asset_dat| C(Asset 2)
    //    B -->|asset_asset_dat| F(Asset 5)
    //    C -->|asset_asset_dat| D(Asset 3)
    //    F -->|asset_asset_dat| G(Asset 6)
    //    G -->|asset_asset_dat| D
    //    D -->|asset_asset_dat| E(Asset 4)
    //    B -->|asset_asset_dat| H(Asset 7)
    //    H -->|asset_asset_dat| D
    //    D -->|asset_asset_dat| B
    //```
    def "With circle: When removing element #removeElement then impact #checkImpactBefore / #checkImpactAfter (before/after) is correctly determined for element no. #checkAsset with starting impact #firstImpact / #secondImpact for element numbers #firstAsset / #secondAsset because #reasons"() {
        given: "a graph of eight assets"
        assetIDs = [] as List
        (0..7).each { i ->
            assetIDs << post("/domains/$dsgvoDomainId/assets", [
                name   : "asset-${i}",
                subType: "AST_Datatype",
                status : "NEW",
                owner  : [targetUri: "$baseUrl/units/$unitId"],
            ]).body.resourceId
        }

        // create links:
        putLinks("${assetIDs[0]}", ["${assetIDs[1]}"])
        putLinks("${assetIDs[1]}", [
            "${assetIDs[2]}",
            "${assetIDs[7]}",
            "${assetIDs[5]}"
        ])
        putLinks("${assetIDs[2]}", ["${assetIDs[3]}"])
        putLinks("${assetIDs[5]}", ["${assetIDs[6]}"])
        putLinks("${assetIDs[6]}", ["${assetIDs[3]}"])
        putLinks("${assetIDs[3]}", [
            "${assetIDs[4]}",
            "${assetIDs[1]}"
        ])
        putLinks("${assetIDs[7]}", ["${assetIDs[3]}"])

        when: "impact values are set"
        putImpactValue(assetIDs[firstAsset], firstImpact)
        putImpactValue(assetIDs[secondAsset], secondImpact)

        then: "inherited impact is correctly calculated"
        def dsra = get("/domains/$dsgvoDomainId/assets/${assetIDs[checkAsset]}").body?.riskValues?.DSRA
        if (checkImpactBefore != null) {
            assert dsra != null
        }
        // if checkImpactBefore is null, dsra may have null values or not exist at all
        // if dsra exists, make sure it has the expected values
        dsra?.with {
            assert potentialImpactsEffective.C == checkImpactBefore
            assert potentialImpactsCalculated.C == checkImpactBefore
        }

        when: "an element is removed"
        delete("/assets/${assetIDs[removeElement]}")

        then: "the impact was correctly recalculated"
        with(get("/domains/$dsgvoDomainId/assets/${assetIDs[checkAsset]}").body.riskValues.DSRA) {
            potentialImpactsEffective.C == checkImpactAfter
            potentialImpactsCalculated.C == checkImpactAfter
        }

        where:
        firstAsset | firstImpact | secondAsset | secondImpact | checkAsset | checkImpactAfter | checkImpactBefore | removeElement | reasons
        0          | 3           | 1           | 1            | 2          | 1                | null              | 3             | "asset2 is no longer part of loop"
        1          | 3           | 0           | 1            | 6          | 3                | null              | 3             | "asset6 is no longer part of loop"
        2          | 2           | 5           | 3            | 3          | 3                | null              | 1             | "asset3 is no longer part of loop"
        7          | 3           | 2           | 2            | 4          | 3                | null              | 1             | "asset4 was always outside the loop, receives its value via asset3"
    }

    @Unroll
    // Test impact inheritance in this graph:
    // ```mermaid
    //graph TD
    //    A(Asset 0) -->|asset_asset_app| B(Asset 1)
    //    B -->|asset_asset_dat| C(Asset 2)
    //    B -->|asset_asset_dat| F(Asset 5)
    //    C -->|asset_asset_dat| D(Asset 3)
    //    F -->|asset_asset_noninheriting| G(Asset 6)
    //    G -->|asset_asset_noninheriting| D
    //    D -->|asset_asset_dat| E(Asset 4)
    //    B -->|asset_asset_dat| H(Asset 7)
    //    H -->|asset_asset_dat| D
    //```
    def "When removing element #removeElement then impact #checkImpactBefore / #checkImpactAfter (before/after) is correctly determined for element no. #checkAsset with starting impact #firstImpact / #secondImpact for element numbers #firstAsset / #secondAsset because #reasons"() {
        given: "a graph of eight assets"
        assetIDs = [] as List
        (0..7).each { i ->
            assetIDs << post("/domains/$dsgvoDomainId/assets", [
                name   : "asset-${i}",
                subType: "AST_Datatype",
                status : "NEW",
                owner  : [targetUri: "$baseUrl/units/$unitId"],
            ]).body.resourceId
        }

        // create links:
        putLinks("${assetIDs[0]}", ["${assetIDs[1]}"])
        putLinks("${assetIDs[1]}", [
            "${assetIDs[2]}",
            "${assetIDs[7]}",
            "${assetIDs[5]}"
        ])
        putLinks("${assetIDs[2]}", ["${assetIDs[3]}"])
        putLinks("${assetIDs[5]}", ["${assetIDs[6]}"], "asset_asset_noninheriting")
        putLinks("${assetIDs[6]}", ["${assetIDs[3]}"], "asset_asset_noninheriting")
        putLinks("${assetIDs[3]}", ["${assetIDs[4]}"])
        putLinks("${assetIDs[7]}", ["${assetIDs[3]}"])

        when: "impact values are set"
        putImpactValue(assetIDs[firstAsset], firstImpact)
        putImpactValue(assetIDs[secondAsset], secondImpact)

        then: "inherited impact is correctly calculated"
        def dsra = get("/domains/$dsgvoDomainId/assets/${assetIDs[checkAsset]}").body?.riskValues?.DSRA
        if (checkImpactBefore != null) {
            assert dsra != null
        }
        // if checkImpactBefore is null, dsra may have null values or not exist at all
        // if dsra exists, make sure it has the expected values
        dsra?.with {
            assert potentialImpactsEffective.C == checkImpactBefore
            assert potentialImpactsCalculated.C == checkImpactBefore
        }

        when: "an element is removed"
        delete("/assets/${assetIDs[removeElement]}")

        then: "the impact was correctly recalculated"
        def dsraAfter = get("/domains/$dsgvoDomainId/assets/${assetIDs[checkAsset]}").body.riskValues.DSRA
        if (checkImpactAfter != null) {
            assert dsraAfter != null
        }
        dsraAfter?.with {
            assert potentialImpactsEffective.C == checkImpactAfter
            assert potentialImpactsCalculated.C == checkImpactAfter
        }

        where:
        firstAsset | firstImpact | secondAsset | secondImpact | checkAsset | checkImpactAfter | checkImpactBefore | removeElement | reasons
        2          | 1           | 7           | 2            | 4          | 1                | 2                 | 7             | "lower value remains"
        2          | 1           | 7           | 2            | 4          | 2                | 2                 | 2             | "higher value remains"
        0          | 2           | 7           | 3            | 4          | 2                | 3                 | 7             | "value gets passed down from asset0"
        0          | 2           | 5           | 3            | 3          | 2                | 2                 | 6             | "non-inheriting links are ignored"
        2          | 1           | 7           | 2            | 4          | 1                | 2                 | 7             | "Lower value remains"
        2          | 1           | 7           | 2            | 4          | 2                | 2                 | 2             | "Higher value remains"
        0          | 2           | 7           | 3            | 4          | 2                | 3                 | 7             | "Value gets passed down from asset 0"
        0          | 2           | 5           | 3            | 3          | 2                | 2                 | 6             | "Non-inheriting links are ignored"
        1          | 3           | 4           | 2            | 5          | null             | 3                 | 1             | "Higher value remains"
        3          | 2           | 6           | 1            | 7          | null             | null              | 3             | "Unaffected because higher up in tree"
        1          | 2           | 3           | 1            | 2          | 2                | 2                 | 3             | "Unaffected by removal lower in tree"
        5          | 3           | 7           | 1            | 6          | null             | null              | 5             | "Only connected via non-inheriting link"
        4          | 1           | 6           | 2            | 7          | null             | null              | 6             | "Not part of inheritance tree"
        2          | 3           | 3           | 1            | 4          | null             | 1                 | 3             | "calculated value is removed when previous element was removed"
        2          | 3           | 3           | 1            | 4          | 1                | 1                 | 2             | "Value gets passed down from asset3"
    }

    @Unroll
    // Test impact inheritance in this graph:
    // ```mermaid
    //graph TD
    //    A(Asset 0) -->|asset_asset_app| B(Asset 1)
    //    B -->|asset_asset_dat| C(Asset 2)
    //    B -->|asset_asset_dat| F(Asset 5)
    //    C -->|asset_asset_dat| D(Asset 3)
    //    F -->|asset_asset_noninheriting| G(Asset 6)
    //    G -->|asset_asset_noninheriting| D
    //    D -->|asset_asset_dat| E(Asset 4)
    //    B -->|asset_asset_dat| H(Asset 7)
    //    H -->|asset_asset_dat| D
    //```
    def "When removing outgoing links from #removeLinksFrom then impact #checkImpactBefore / #checkImpactAfter (before/after) is correctly determined for element no. #checkAsset with starting impact #firstImpact / #secondImpact for element numbers #firstAsset / #secondAsset because #reasons"() {
        given: "a graph of eight assets"
        assetIDs = [] as List
        (0..7).each { i ->
            assetIDs << post("/domains/$dsgvoDomainId/assets", [
                name   : "asset-${i}",
                subType: "AST_Datatype",
                status : "NEW",
                owner  : [targetUri: "$baseUrl/units/$unitId"],
            ]).body.resourceId
        }

        // create links:
        putLinks("${assetIDs[0]}", ["${assetIDs[1]}"])
        putLinks("${assetIDs[1]}", [
            "${assetIDs[2]}",
            "${assetIDs[7]}",
            "${assetIDs[5]}"
        ])
        putLinks("${assetIDs[2]}", ["${assetIDs[3]}"])
        putLinks("${assetIDs[5]}", ["${assetIDs[6]}"], "asset_asset_noninheriting")
        putLinks("${assetIDs[6]}", ["${assetIDs[3]}"], "asset_asset_noninheriting")
        putLinks("${assetIDs[3]}", ["${assetIDs[4]}"])
        putLinks("${assetIDs[7]}", ["${assetIDs[3]}"])

        when: "impact values are set"
        putImpactValue(assetIDs[firstAsset], firstImpact)
        putImpactValue(assetIDs[secondAsset], secondImpact)

        then: "inherited impact is correctly calculated"
        def dsra = get("/domains/$dsgvoDomainId/assets/${assetIDs[checkAsset]}").body?.riskValues?.DSRA
        if (checkImpactBefore != null) {
            assert dsra != null
        }
        // if checkImpactBefore is null, dsra may have null values or not exist at all
        // if dsra exists, make sure it has the expected values
        dsra?.with {
            assert potentialImpactsEffective.C == checkImpactBefore
            assert potentialImpactsCalculated.C == checkImpactBefore
        }

        when: "links are removed"
        removeLinks("${assetIDs[removeLinksFrom]}")

        then: "the impact was correctly recalculated"
        def dsraAfter = get("/domains/$dsgvoDomainId/assets/${assetIDs[checkAsset]}").body.riskValues.DSRA
        if (checkImpactAfter != null) {
            assert dsraAfter != null
        }
        dsraAfter?.with {
            assert potentialImpactsEffective.C == checkImpactAfter
            assert potentialImpactsCalculated.C == checkImpactAfter
        }

        where:
        firstAsset | firstImpact | secondAsset | secondImpact | checkAsset | checkImpactAfter | checkImpactBefore | removeLinksFrom | reasons
        2          | 1           | 7           | 2            | 4          | 1                | 2                 | 7             | "lower value remains"
        2          | 1           | 7           | 2            | 4          | 2                | 2                 | 2             | "higher value remains"
        0          | 2           | 7           | 3            | 4          | 2                | 3                 | 7             | "value gets passed down from asset0"
        0          | 2           | 5           | 3            | 3          | 2                | 2                 | 6             | "non-inheriting links are ignored"
        2          | 1           | 7           | 2            | 4          | 1                | 2                 | 7             | "Lower value remains"
        2          | 1           | 7           | 2            | 4          | 2                | 2                 | 2             | "Higher value remains"
        0          | 2           | 7           | 3            | 4          | 2                | 3                 | 7             | "Value gets passed down from asset 0"
        0          | 2           | 5           | 3            | 3          | 2                | 2                 | 6             | "Non-inheriting links are ignored"
        1          | 3           | 4           | 2            | 5          | null             | 3                 | 1             | "Higher value remains"
        3          | 2           | 6           | 1            | 7          | null             | null              | 3             | "Unaffected because higher up in tree"
        1          | 2           | 3           | 1            | 2          | 2                | 2                 | 3             | "Unaffected by removal lower in tree"
        5          | 3           | 7           | 1            | 6          | null             | null              | 5             | "Only connected via non-inheriting link"
        4          | 1           | 6           | 2            | 7          | null             | null              | 6             | "Not part of inheritance tree"
        2          | 3           | 3           | 1            | 4          | null             | 1                 | 3             | "calculated value is removed when previous element was removed"
        2          | 3           | 3           | 1            | 4          | 1                | 1                 | 2             | "Value gets passed down from asset3"
    }

    protected putImpactValue(String assetID, Integer impactValue) {
        get("/domains/$dsgvoDomainId/assets/$assetID").with {
            body.riskValues.DSRA = body.riskValues.DSRA ?: [:]
            body.riskValues.DSRA.potentialImpacts = body.riskValues.DSRA.potentialImpacts ?: [:]
            body.riskValues.DSRA.potentialImpacts.C = impactValue
            put(body._self, body, getETag())
        }
    }

    protected putLinks(String source, List<GString> targets, String type = "asset_asset_dat") {
        def url = "/domains/$dsgvoDomainId/assets/$source"
        get(url).with {
            body.links = [
                (type): targets.collect { target ->
                    [target: [targetUri: "$baseUrl/assets/$target"]]
                }
            ]
            put(body._self, body, getETag())
        }
    }

    protected removeLinks(String source) {
        def url = "/domains/$dsgvoDomainId/assets/$source"
        get(url).with {
            body.links = [:]
            put(body._self, body, getETag())
        }
    }
}
