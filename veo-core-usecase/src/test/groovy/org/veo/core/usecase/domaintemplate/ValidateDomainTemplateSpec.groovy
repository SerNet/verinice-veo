/*
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
 */
package org.veo.core.usecase.domaintemplate

import com.github.zafarkhaja.semver.Version

import org.veo.core.entity.DomainTemplate
import org.veo.core.entity.ElementType
import org.veo.core.entity.NameAbbreviationAndDescription
import org.veo.core.entity.Translated
import org.veo.core.entity.TranslatedText
import org.veo.core.entity.TranslationException
import org.veo.core.entity.definitions.CustomAspectDefinition
import org.veo.core.entity.definitions.ElementTypeDefinition
import org.veo.core.entity.definitions.SubTypeDefinition
import org.veo.core.entity.definitions.attribute.BooleanAttributeDefinition
import org.veo.core.entity.definitions.attribute.IntegerAttributeDefinition
import org.veo.core.entity.domainmigration.CustomAspectMigrationTransformDefinition
import org.veo.core.entity.domainmigration.DomainMigrationDefinition
import org.veo.core.entity.domainmigration.DomainMigrationStep
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
    static final Map MINIMAL_TRANSLATION_MAP = [(Locale.of("EN")): [
            "asset_sub_plural": "assets",
            "asset_sub_singular": "asset",
            "asset_sub_status_one": "one"
        ]]
    static final Map INCOMPLETE_TRANSLATION_MAP = [(Locale.of("EN")): ["some Value": "some"]]
    static final Map SUPERFLUOUS_MAP = [(Locale.of("EN")): ["some Value": "some",
            "asset_sub_plural": "assets",
            "asset_sub_singular": "asset",
            "asset_sub_status_one": "one"
        ]]

    def "Report incomplete translation #translation"() {
        given: "an unvalid template"
        def id = UUID.randomUUID()
        DomainTemplate domaintemplate = Mock()
        domaintemplate.getId() >> id
        domaintemplate.getTranslations() >> translation

        when: "validate"
        DomainTemplateValidator.validateDomainTemplate(domaintemplate, null, null)

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
        domaintemplate.getCatalogItems() >> []
        domaintemplate.getProfiles() >> []
        domaintemplate.getTranslations() >> COMPLETE_TRANSLATION
        domaintemplate.domainMigrationDefinition >> new DomainMigrationDefinition([])

        when: "validate"
        DomainTemplateValidator.validateDomainTemplate(domaintemplate, null, null)

        then:
        noExceptionThrown()
    }

    def "Report incomplete element type definition translations"() {
        given: "a valid template"
        def id = UUID.randomUUID()
        DomainTemplate domaintemplate = Mock()
        domaintemplate.getId() >> id
        domaintemplate.getCatalogItems() >> []
        domaintemplate.getProfiles() >> []
        domaintemplate.getTranslations() >> COMPLETE_TRANSLATION

        def ca = new CustomAspectDefinition()
        ca.setAttributeDefinitions(attributDefinition)
        def st = new SubTypeDefinition()
        st.setSortKey(0)
        st.setStatuses(["one"])
        ElementTypeDefinition testType = Mock()
        testType.getElementType() >> ElementType.ASSET
        testType.getSubTypes() >> ["sub": st]
        testType.getCustomAspects() >> ["testAspect" : ca]
        testType.getLinks() >> [:]
        testType.getTranslations() >> translations

        domaintemplate.getElementTypeDefinitions() >> Set.of(testType)

        when: "validate"
        DomainTemplateValidator.validateDomainTemplate(domaintemplate, null, null)

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

    def "Report incomplete migrations"() {
        given: "a valid template"
        DomainTemplate domaintemplate = Mock() {
            getId() >> UUID.randomUUID()
            getTranslations() >> COMPLETE_TRANSLATION
            getCatalogItems() >> []
            getProfiles() >> []
            getTemplateVersion() >> new Version(1, 0, 0)
            getElementTypeDefinitions() >> Set.of(Mock(ElementTypeDefinition) {
                getElementType() >> ElementType.ASSET
                getSubTypes() >> [
                    "sub": new SubTypeDefinition().tap {
                        setSortKey(0)
                        setStatuses(["one"])
                    }]
                getCustomAspects() >> [
                    "testAspect": new CustomAspectDefinition().tap {
                        setAttributeDefinitions(["test-attribute": new BooleanAttributeDefinition()])
                    }]
                getLinks() >> [:]
                getTranslations() >> [(Locale.of("EN")): [
                        "asset_sub_plural"    : "assets",
                        "asset_sub_singular"  : "asset",
                        "asset_sub_status_one": "one",
                        "test-attribute"      : "a test"
                    ]]
            })
            getDomainMigrationDefinition() >> new DomainMigrationDefinition(steps)
        }

        when: "validate"
        DomainTemplateValidator.validateDomainTemplate(domaintemplate, null, null)

        then:
        UnprocessableDataException e = thrown()
        e.message == errorMessage

        where:
        steps | errorMessage
        [
            new DomainMigrationStep("1",new TranslatedText([:]),[],null,false)
        ] | "No description provided for step '1'."
        [
            new DomainMigrationStep("1",new TranslatedText(["en": [:]]),[],null,false),
            new DomainMigrationStep("1",new TranslatedText(["en": [:]]),[],null,false)
        ] | "Id '1' not unique."
        [
            new DomainMigrationStep("1",new TranslatedText(["en": [:]]),[],
            [
                new CustomAspectMigrationTransformDefinition(null,null)
            ],true)
        ] |"Interactive step 1 does not support new definitions."
    }
}
