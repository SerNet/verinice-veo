/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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

/**
 * Integration test for the translation controller.
 * Uses mocked spring MVC environment.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
class TranslationControllerMockMvcSpec extends VeoMvcSpec {

    @WithUserDetails("user@domain.example")
    def "get the translation for multiple languages"() {
        given:
        createTestClient().tap {
            createTestDomain(it, DSGVO_DOMAINTEMPLATE_UUID)
        }
        when: "a request for t9ns is made"

        def translations = parseJson(get('/translations?languages=de,en'))

        then: "a correct response is returned"
        translations.lang.de.person_address_city == "Stadt"
        translations.lang.en.person_address_city == "City"

        and: "the response contains translations for the standard properties"
        translations.lang.de.name == 'Name'
        translations.lang.de.abbreviation == 'Abkürzung'
        translations.lang.en.description == 'Description'

        and: "the response contains translations for the element types"
        translations.lang.de.document == 'Dokument'
        translations.lang.de.scopes == 'Scopes'
        translations.lang.en.control == 'control'
        translations.lang.en.persons == 'persons'
    }

    @WithUserDetails("user@domain.example")
    def "get the translation for a single language"() {
        given:
        createTestClient().tap {
            createTestDomain(it, DSGVO_DOMAINTEMPLATE_UUID)
        }
        when: "a request for t9ns is made"
        def translations = parseJson(get('/translations?languages=de'))

        then: "a correct response is returned"
        translations.lang.de != null
        translations.lang.en == null
    }

    @WithUserDetails("user@domain.example")
    def "get the translation for an unsupported language"() {
        given:
        createTestClient().tap {
            createTestDomain(it, DSGVO_DOMAINTEMPLATE_UUID)
        }
        when: "a request for t9ns is made"
        def translations = parseJson(get('/translations?languages=tlh'))

        then: "a fallback response is returned"
        translations.lang.keySet() ==~ ['tlh']
        with (translations.lang.tlh) {
            asset == 'asset'
            description == 'Description'
        }
    }
}
