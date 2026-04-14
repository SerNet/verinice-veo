/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2026  Urs Zeidler
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
package org.veo.core.usecase.domaintemplate

import org.veo.core.entity.DomainTemplate
import org.veo.core.entity.ElementType
import org.veo.core.entity.NameAbbreviationAndDescription
import org.veo.core.entity.Translated
import org.veo.core.entity.TranslationException
import org.veo.core.entity.definitions.CustomAspectDefinition
import org.veo.core.entity.definitions.ElementTypeDefinition
import org.veo.core.entity.definitions.SubTypeDefinition
import org.veo.core.entity.definitions.attribute.BooleanAttributeDefinition
import org.veo.core.entity.exception.UnprocessableDataException
import org.veo.core.usecase.UseCaseSpec

class ValidateDomainTemplateSpec extends UseCaseSpec {

    static final Translated EMPTY_TRANSLATION = new Translated(Map.of())
    static final Translated INCOMPLETE_TRANSLATION = new Translated([
        (Locale.of("EN")): new NameAbbreviationAndDescription()
    ])
    static final Translated COMPLETE_TRANSLATION = new Translated([
        (Locale.of("EN")): new NameAbbreviationAndDescription("a","name","desc")
    ]
    )
    static final Map MINIMAL_TRANSLATION_MAP = [(Locale.of("EN")):[
            "asset_sub_plural": "assets",
            "asset_sub_singular": "asset" ,
            "asset_sub_status_one": "one"
        ]]
    static final Map INCOMPLETE_TRANSLATION_MAP = [(Locale.of("EN")):["some Value": "some"]]
    static final Map SUPERFLUOUS_MAP = [(Locale.of("EN")):["some Value": "some",
            "asset_sub_plural": "assets",
            "asset_sub_singular": "asset" ,
            "asset_sub_status_one": "one"
        ]]

    def "Report incomplete translation #translation"() {
        given: "an unvalid template"
        def id = UUID.randomUUID()
        DomainTemplate domaintemplate = Mock()
        domaintemplate.getId() >> id
        domaintemplate.getTranslations() >> translation

        when: "validate"
        DomainTemplateValidator.validateDomainTemplate(domaintemplate)

        then:
        UnprocessableDataException e = thrown()
        e.message == errorMessage

        where:
        translation | errorMessage
        null |"No translations"
        EMPTY_TRANSLATION |"No translations"
        INCOMPLETE_TRANSLATION|"Translated template name missing for 'en'."
    }

    def "Template with translated names passes validation"() {
        given: "a valid template"
        def id = UUID.randomUUID()
        DomainTemplate domaintemplate = Mock()
        domaintemplate.getId() >> id
        domaintemplate.getTemplateVersion()>> com.github.zafarkhaja.semver.Version.forIntegers(1)
        domaintemplate.getElementTypeDefinitions() >> []
        domaintemplate.getTranslations() >> COMPLETE_TRANSLATION

        when: "validate"
        DomainTemplateValidator.validateDomainTemplate(domaintemplate)

        then:
        noExceptionThrown()
    }

    def "Report incomplete element type definition translations"() {
        given: "a valid template"
        def id = UUID.randomUUID()
        DomainTemplate domaintemplate = Mock()
        domaintemplate.getId() >> id
        domaintemplate.getTranslations() >> COMPLETE_TRANSLATION

        def ca = new CustomAspectDefinition()
        ca.setAttributeDefinitions(attributDefinition)
        def st = new SubTypeDefinition()
        st.setSortKey(0)
        st.setStatuses(["one"])
        ElementTypeDefinition testType = Mock()
        testType.getElementType() >> ElementType.ASSET
        testType.getSubTypes() >> ["sub": st ]
        testType.getCustomAspects() >> ["testAspect" : ca]
        testType.getLinks() >> [:]
        testType.getTranslations() >> translations

        domaintemplate.getElementTypeDefinitions() >> Set.of(testType)

        when: "validate"
        DomainTemplateValidator.validateDomainTemplate(domaintemplate)

        then:
        TranslationException e = thrown()
        e.message == errorMessage

        where:
        translations| attributDefinition | errorMessage
        Map.of() | [:] | "Issues were found in the translations: Language 'en': MISSING: Translations empty for: asset"
        INCOMPLETE_TRANSLATION_MAP| [:] | "Issues were found in the translations: Language 'en': MISSING: asset_sub_plural, asset_sub_singular, asset_sub_status_one ; SUPERFLUOUS: some Value"
        SUPERFLUOUS_MAP| [:] | "Issues were found in the translations: Language 'en': SUPERFLUOUS: some Value"
        MINIMAL_TRANSLATION_MAP| ["test-attribute": new BooleanAttributeDefinition()] | "Issues were found in the translations: Language 'en': MISSING: test-attribute"
    }
}
