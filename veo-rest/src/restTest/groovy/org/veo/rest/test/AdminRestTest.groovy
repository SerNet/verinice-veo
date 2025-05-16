/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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

import static org.veo.rest.test.UserType.DEFAULT

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.temporal.TemporalAccessor
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters
import java.time.temporal.TemporalAmount
import java.time.temporal.TemporalUnit

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j

@Slf4j
class AdminRestTest extends VeoRestTest{
    def "get unit dump"() {
        given:
        def unitId = post("/units", [
            name: "my little unit"
        ]).body.resourceId

        when:
        def dump = get("/admin/unit-dump/$unitId", 200, UserType.ADMIN).body

        then:
        dump.unit.name == "my little unit"
    }

    def "get a unit dump from created elements"() {
        given:
        def unitId = postNewUnit("my catalog unit").resourceId
        def catalogItemIds = getCatalogItems(dsgvoDomainId)*.id
        log.info("==> catalogItemIds: {}", catalogItemIds)

        def incarnationDescription = get("/units/${unitId}/domains/$dsgvoDomainId/incarnation-descriptions?itemIds=${catalogItemIds.join(',')}&mode=MANUAL").body
        log.info("==> incarnationDescription: {}", JsonOutput.toJson(incarnationDescription))
        post("/units/${unitId}/incarnations", incarnationDescription)

        when:
        def dump = get("/admin/unit-dump/$unitId", 200, UserType.ADMIN).body
        log.info("===> {}",JsonOutput.toJson(dump))

        then:
        dump.unit.name == "my catalog unit"

        dump.domains.size() >= 2
        dump.elements.size() == catalogItemIds.size()
        with (dump.elements.find { it.abbreviation == "VVT" }) {
            description == "VVT-Prozess"
            links.size() == 1
            links.process_tom.size() == 8
            domains[owner.dsgvoDomainId].subType == "PRO_DataProcessing"
            domains[owner.dsgvoDomainId].status == "NEW"
        }
        with (dump.elements.find { it.abbreviation == "TOM-I" }) {
            customAspects.size() == 1
            customAspects.control_dataProtection.domains.size() == 1
            customAspects.control_dataProtection.attributes.size() == 1
            customAspects.control_dataProtection.attributes.control_dataProtection_objectives == [
                "control_dataProtection_objectives_integrity"
            ]
            domains[owner.dsgvoDomainId].subType == "CTL_TOM"
            domains[owner.dsgvoDomainId].status == "NEW"
        }
    }

    def "crud system messages"() {
        when: "we add a message"
        def pubDate = Instant.now().plusSeconds(10).truncatedTo(ChronoUnit.SECONDS)
        def effDate = Instant.now().plusSeconds(20).truncatedTo(ChronoUnit.SECONDS)
        def pastDate = Instant.now().minusSeconds(20).truncatedTo(ChronoUnit.SECONDS)

        def ret = post("/admin/messages",[message:[DE: "test message"],
            publication: "$pubDate",
            effective: "$effDate",
            level: "INFO"
        ],201, UserType.ADMIN)
        def messageId = ret.body.resourceId

        then:
        with(ret.body) {
            resourceId == "1"
            message == "SystemMessage created successfully."
        }

        and:"the message exists for users"
        with(get("/messages", 200, UserType.DEFAULT).body) {
            size() == 1
            with(it.first()) {
                id == 1
                level == "INFO"
                message == ["de": "test message"]
                effective== "$effDate"
                publication == "$pubDate"
            }
        }

        and:"it can be loaded individually"
        with(get("/messages/"+messageId, 200, UserType.DEFAULT).body) {
            id == 1
            level == "INFO"
            message == ["de": "test message"]
            effective== "$effDate"
            publication == "$pubDate"
        }

        and: "invalid id returns not found"
        get("/messages/0", 404, UserType.DEFAULT)

        and: "users cannot post a message"
        post("/admin/messages",[message:[DE: "test message"],
            publication: "$pubDate",
            effective: "$effDate",
            level: "INFO",
            type: "SYSTEM"
        ],403, UserType.DEFAULT)

        and: "users cannot modify a message"
        put("/admin/messages/$messageId",[message:[DE: "test message1"],
            publication: "$pubDate",
            effective: "$effDate",
            level: "INFO"
        ],null,403, UserType.DEFAULT)

        and:"users cannot delete a message"
        delete("/admin/messages/$messageId",403, UserType.DEFAULT)

        when: "the admin changes the message"
        ret = put("/admin/messages/$messageId",[message:[DE: "test message1"],
            publication: "$pubDate",
            effective: "$effDate",
            level: "INFO"
        ],null,200, UserType.ADMIN)

        then:
        with(ret.body) {
            message == "SystemMessage updated."
        }

        and:"the message is changed"
        with(get("/messages", 200, UserType.DEFAULT).body) {
            size() == 1
            with(it.first()) {
                id == 1
                level == "INFO"
                message == ["de": "test message1"]
            }
        }

        expect: "that the dates are validated"
        put("/admin/messages/$messageId",[message:[DE: "test message1"],
            publication: "$effDate",
            effective: "$pubDate",
            level: "INFO"
        ],null,422, UserType.ADMIN).body.message == "Effective time is before publication time."

        put("/admin/messages/$messageId",[message:[DE: "test message1"],
            publication: "$pubDate",
            effective: "$pastDate",
            level: "INFO"
        ],null,422, UserType.ADMIN).body.message == "Effective time is before publication time."

        put("/admin/messages/$messageId",[message:[DE: "test message1"],
            publication: "$pastDate",
            effective: "$effDate",
            level: "INFO"
        ],null,422, UserType.ADMIN).body.message == "Publication time is before creation time."

        and: "effective can be null"
        put("/admin/messages/$messageId",[message:[DE: "test message1"],
            publication: "$effDate",
            level: "INFO"
        ],null,200, UserType.ADMIN).body.message == "SystemMessage updated."

        when: "the admin deletes the message"
        delete("/admin/messages/$messageId",204, UserType.ADMIN)

        then:"the message is deleted"
        with(get("/messages", 200, UserType.DEFAULT).body) {
            size() == 0
        }

        and:"deleting again returns not found"
        delete("/admin/messages/$messageId",404, UserType.ADMIN)

        and: "updating returns not found"
        put("/admin/messages/$messageId",[message:[DE: "test message1"],
            publication: "$pubDate",
            effective: "$effDate",
            level: "INFO"
        ],null,404, UserType.ADMIN)
    }
}
