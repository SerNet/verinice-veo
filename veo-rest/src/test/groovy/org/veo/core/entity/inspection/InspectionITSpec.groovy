/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.core.entity.inspection

import org.veo.core.entity.Asset
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.entity.condition.CustomAspectAttributeValueProvider
import org.veo.core.entity.condition.EqualsMatcher
import org.veo.core.entity.condition.PartCountProvider
import org.veo.test.VeoSpec

class InspectionITSpec extends VeoSpec {
    Domain domain
    Unit unit

    def setup() {
        def client = newClient {}
        domain = newDomain(client)
        unit = newUnit(client)
    }

    def "inspection yields findings"() {
        given:
        def stickerInspection = newInspection() {
            severity = Severity.WARNING
            description.en = "Every computer running linux needs a tux sticker"
            elementType = Asset.SINGULAR_TERM
            elementSubType = "computer"
            addCondition(new CustomAspectAttributeValueProvider("software", "os"), new EqualsMatcher("linux"))
            addCondition(new PartCountProvider("tuxSticker"), new EqualsMatcher(0))
            suggestAddingPart("tuxSticker")
        }

        when: "a mac has no tux sticker"
        def mac = newAsset(unit) {
            setSubType(domain, "computer", "NEW")
            customAspects.add(newCustomAspect("software") {
                attributes["os"] = "macOs"
            })
        }
        def finding = stickerInspection.run(mac, domain)

        then: "it's alright"
        finding.empty

        when: "a linux pc has no sticker"
        def linuxPc = newAsset(unit) {
            setSubType(domain, "computer", "NEW")
            customAspects.add(newCustomAspect("software") {
                attributes["os"] = "linux"
            })
        }
        finding = stickerInspection.run(linuxPc, domain)

        then: "there is a warning"
        with(finding.get()) {
            severity == Severity.WARNING
            description.en == "Every computer running linux needs a tux sticker"
            with(suggestions[0]) {
                it instanceof AddPartSuggestion
                partSubType == "tuxSticker"
            }
        }

        when: "the linux pc is equipped with a tux sticker"
        linuxPc.addPart(newAsset(unit) {
            setSubType(domain, "tuxSticker", "NEW")
        })
        finding = stickerInspection.run(linuxPc, domain)

        then: "the warning is gone"
        finding.empty
    }
}
