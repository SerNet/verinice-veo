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
package org.veo.core.entity

import org.veo.core.entity.definitions.attribute.BooleanAttributeDefinition
import org.veo.core.entity.definitions.attribute.EnumAttributeDefinition
import org.veo.core.entity.definitions.attribute.IntegerAttributeDefinition
import org.veo.core.entity.definitions.attribute.ListAttributeDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.test.VeoSpec

class ElementTypeDefinitionSpec extends VeoSpec {

    def client = newClient()
    def domain = newDomain(client)

    def "formats custom aspect attributes"() {
        given:
        def etd = newElementTypeDefinition(ElementType.DOCUMENT,domain)
        domain.applyElementTypeDefinition(etd)

        and:
        etd.customAspects.myAsp = newCustomAspectDefinition {
            attributeDefinitions = [
                docTitle: new TextAttributeDefinition(),
                docType: new EnumAttributeDefinition(["paper_back", "hard_cover"]),
                docNumberOfPages: new IntegerAttributeDefinition(),
                docUsesSimpleLanguage: new BooleanAttributeDefinition(),
                docGenres: new ListAttributeDefinition(new EnumAttributeDefinition([
                    "non_fiction",
                    "comedy",
                    "tragedy",
                    "trash"
                ])),
            ]
        }
        etd.translations[Locale.ENGLISH] = [
            paper_back: "Paperback",
            hard_cover: "Hardcover",
            non_fiction:"Non-fiction",
            comedy:"Comedy",
            tragedy:"Tragedy",
            trash: "Trash"
        ]

        expect:
        etd.localizeCustomAspectAttributeValue("myAsp", "docTitle", null, Locale.ENGLISH) == "-"
        etd.localizeCustomAspectAttributeValue("myAsp", "docTitle", "My new book", Locale.ENGLISH) == "My new book"
        etd.localizeCustomAspectAttributeValue("myAsp", "docType", "paper_back", Locale.ENGLISH) == "Paperback"
        etd.localizeCustomAspectAttributeValue("myAsp", "docNumberOfPages", 42, Locale.ENGLISH) == "42"
        etd.localizeCustomAspectAttributeValue("myAsp", "docUsesSimpleLanguage", true, Locale.ENGLISH) == "yes"
        etd.localizeCustomAspectAttributeValue("myAsp", "docGenres", [
            "comedy",
            "trash"
        ], Locale.ENGLISH) == "Comedy, Trash"
    }
}
