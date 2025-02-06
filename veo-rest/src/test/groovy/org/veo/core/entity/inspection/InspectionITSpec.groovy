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

import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
import org.veo.core.entity.Unit
import org.veo.core.entity.condition.AndExpression
import org.veo.core.entity.condition.ConstantExpression
import org.veo.core.entity.condition.ContainsExpression
import org.veo.core.entity.condition.CustomAspectAttributeValueExpression
import org.veo.core.entity.condition.EqualsExpression
import org.veo.core.entity.condition.PartCountExpression
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
            description.translations.(Locale.ENGLISH) = "Every computer running linux needs a tux sticker"
            elementType = ElementType.ASSET
            elementSubType = "computer"
            condition = new AndExpression([
                new EqualsExpression(
                new CustomAspectAttributeValueExpression("software", "os"),
                new ConstantExpression("linux")
                ),
                new EqualsExpression(
                new PartCountExpression("tuxSticker"),
                new ConstantExpression(0)
                )
            ])
            suggestAddingPart("tuxSticker")
        }

        when: "a mac has no tux sticker"
        def mac = newAsset(unit) {
            associateWithDomain(domain, "computer", "NEW")
            customAspects.add(newCustomAspect("software", null) {
                attributes["os"] = "macOs"
            })
        }
        def finding = stickerInspection.run(mac, domain)

        then: "it's alright"
        finding.empty

        when: "a linux pc has no sticker"
        def linuxPc = newAsset(unit) {
            associateWithDomain(domain, "computer", "NEW")
            customAspects.add(newCustomAspect("software", null) {
                attributes["os"] = "linux"
            })
        }
        finding = stickerInspection.run(linuxPc, domain)

        then: "there is a warning"
        with(finding.get()) {
            severity == Severity.WARNING
            description.translations.en == "Every computer running linux needs a tux sticker"
            with(suggestions[0]) {
                it instanceof AddPartSuggestion
                partSubType == "tuxSticker"
            }
        }

        when: "the linux pc is equipped with a tux sticker"
        linuxPc.addPart(newAsset(unit) {
            associateWithDomain(domain, "tuxSticker", "NEW")
        })
        finding = stickerInspection.run(linuxPc, domain)

        then: "the warning is gone"
        finding.empty
    }

    def "find a constant string in a CA attribute list"() {
        given:
        def inspection = newInspection() {
            severity = Severity.HINT
            description.translations.(Locale.ENGLISH) = "A dog might find the needle in the haystack"
            elementType = ElementType.ASSET
            elementSubType = "farm"
            condition =
                    new ContainsExpression(
                    new CustomAspectAttributeValueExpression("assets", "haystack"),
                    new ConstantExpression("needle")
                    )
            suggestAddingPart("dog")
        }

        when:
        def farm = newAsset(unit) {
            associateWithDomain(domain, "farm", "ESTABLISHED")
            customAspects.add(newCustomAspect("assets", domain) {
                attributes["haystack"] = [
                    'straw',
                    'straw',
                    'straw',
                    'needle',
                    'straw'
                ]
            })
        }
        def finding = inspection.run(farm, domain)

        then:
        with(finding.get()) {
            severity == Severity.HINT
            description.translations.en == "A dog might find the needle in the haystack"
            with(suggestions[0]) {
                it instanceof AddPartSuggestion
                partSubType == "dog"
            }
        }

        when:
        farm.applyCustomAspect(newCustomAspect("assets", domain) {
            attributes["haystack"] = [
                'straw',
                'straw',
                'straw',
                'straw'
            ]
        })
        finding = inspection.run(farm, domain)

        then:
        finding.empty
    }
}