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
import org.veo.core.entity.ElementType
import org.veo.rest.common.ClientNotActiveException

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
        translations.lang.de.subType == 'Subtyp'
        translations.lang.de.riskDefinition == 'Risikodefinition'
        translations.lang.en.description == 'Description'
        translations.lang.en.potentialProbability == 'Potential probability'
        translations.lang.en.potentialProbabilityExplanation == 'Explanation'

        and: "the response contains translations for the element types"
        translations.lang.de.document == 'Dokument'
        translations.lang.de.scopes == 'Scopes'
        translations.lang.en.control == 'control'
        translations.lang.en.persons == 'persons'
        translations.lang.en.person_plural == 'persons'
    }

    @WithUserDetails("user@domain.example")
    def "get the translation for a single language"() {
        given:
        createTestClient().tap {
            createTestDomain(it, DSGVO_DOMAINTEMPLATE_UUID)
        }

        when: "a request for t9ns is made"
        def translations = parseJson(get('/translations?languages=de'))

        then: "only the requested t9n is returned"
        translations.lang.de != null
        translations.lang.en == null
    }

    @WithUserDetails("user@domain.example")
    def "correctly handles conflicting translations for domains"() {
        given:
        def client = createTestClient()
        def domain1 =
                createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)

        def domain2 =
                createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID).with {
                    elementTypeDefinitions.find { it.elementType== ElementType.PERSON }.translations.get(Locale.ENGLISH).'person_contactInformation_office' = 'Office phone'
                    domainDataRepository.save(it)
                }

        when:
        def translationsDomain1 = parseJson(get("/translations?domain=${domain1.idAsString}&languages=de,en"))

        then:
        translationsDomain1.lang.en.person_contactInformation_office == 'Phone'

        when:
        def translationsDomain2 = parseJson(get("/translations?domain=${domain2.idAsString}&languages=de,en"))

        then:
        translationsDomain2.lang.en.person_contactInformation_office == 'Office phone'

        when:
        def translationsBothDomains = parseJson(get("/translations?languages=de,en"))

        then:
        translationsBothDomains.lang.en.person_contactInformation_office in ['Phone', 'Office phone']

        and:
        noExceptionThrown()
    }

    @WithUserDetails("user@domain.example")
    def "get the translation for a non-existing domain"() {
        given:
        createTestClient()

        expect:
        get("/translations?domain=123&languages=de,en", 404)
    }

    @WithUserDetails("user@domain.example")
    def "get the translation for an unsupported language"() {
        given:
        createTestClient().tap {
            createTestDomain(it, DSGVO_DOMAINTEMPLATE_UUID)
        }

        when: "an unsupported language is requested"
        def translations = parseJson(get('/translations?languages=tlh'))

        then: "a fallback response is returned"
        translations.lang.keySet() ==~ ['tlh']
        with(translations.lang.tlh) {
            asset == 'asset'
            description == 'Description'
        }
    }

    @WithUserDetails("user@domain.example")
    def "get the translation for an invalid language"() {
        given:
        createTestClient().tap {
            createTestDomain(it, DSGVO_DOMAINTEMPLATE_UUID)
        }

        when: "a request for a non-existing ISO country code is made"
        def translations = parseJson(get('/translations?languages=vulcan'))

        then: "no translations are returned"
        translations.lang.de == null
        translations.lang.en == null

        then: "...except for a deprecated default translation for risk definitions"
        // TODO VEO-1739 remove deprecated "default translation"
        // translations.lang.vulcan == null
        translations.lang.vulcan.riskDefinition != null
    }

    @WithUserDetails("user@domain.example")
    def "get a translation with a region"() {
        given:
        createTestClient().tap {
            createTestDomain(it, DSGVO_DOMAINTEMPLATE_UUID)
        }

        when: "a request with a region is made"
        def translations = parseJson(get('/translations?languages=de-CH'))

        then: "the matching language is returned"
        translations.lang.de != null
        translations.lang.en == null
    }

    @WithUserDetails("user@domain.example")
    def "get translations without an active client"() {
        when: "a request for translations"
        get('/translations?languages=de', 403)

        then: "no translations are returned"
        thrown(ClientNotActiveException)
    }
}
