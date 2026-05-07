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
package org.veo.rest

import org.springframework.security.test.context.support.WithUserDetails

import com.github.zafarkhaja.semver.Version

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.ElementType
import org.veo.core.entity.NameAbbreviationAndDescription
import org.veo.core.entity.Translated
import org.veo.core.entity.TranslatedText
import org.veo.core.entity.condition.CustomAspectAttributeValueExpression
import org.veo.core.entity.definitions.attribute.BooleanAttributeDefinition
import org.veo.core.entity.definitions.attribute.ExternalDocumentAttributeDefinition
import org.veo.core.entity.definitions.attribute.IntegerAttributeDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.core.entity.domainmigration.CustomAspectAttribute
import org.veo.core.entity.domainmigration.CustomAspectMigrationTransformDefinition
import org.veo.core.entity.domainmigration.DomainMigrationDefinition
import org.veo.core.entity.domainmigration.DomainMigrationStep

@WithUserDetails("user@domain.example")
class DomainUpdateMvcITSpec extends VeoMvcSpec {

    Client client

    def setup() {
        executeInTransaction {
            client = createTestClient()
        }
    }

    def "possible domain updates are presented"() {
        given:
        domainTemplateDataRepository.saveAll([
            newDomainTemplate {
                name = "lärmschutz"
                templateVersion = Version.parse("0.5.0")
            },
            newDomainTemplate {
                name = "lärmschutz"
                templateVersion = Version.parse("0.5.1")
            },
            newDomainTemplate {
                name = "lärmschutz"
                templateVersion = Version.parse("0.5.2")
            },
            newDomainTemplate {
                name = "lärmschutz"
                templateVersion = Version.parse("1.0.0")
            },
            newDomainTemplate {
                name = "esdProtection"
                templateVersion = Version.parse("1.8.3")
            },
            newDomainTemplate {
                name = "esdProtection"
                templateVersion = Version.parse("2.0.0")
            },
        ])

        and:
        domainDataRepository.saveAll([
            newDomain(client) {
                name = "lärmschutz"
                templateVersion = Version.parse("0.5.1")
                translations = new Translated<>([
                    (EN): new NameAbbreviationAndDescription("Hearing protection", "hp", "WHAT DID YOU SAY? I CAN'T HEAR YOU!")
                ])
            },
            newDomain(client) {
                name = "esdProtection"
                templateVersion = Version.parse("1.8.3")
                translations = new Translated<>([
                    (EN): new NameAbbreviationAndDescription("ESD Protection", "esdp", "High voltage")
                ])
            },
        ])

        when:
        def updates = parseJson(get("/domains/updates"))

        then:
        updates.size() == 2
        with(updates.find { it.domain.name == "lärmschutz" }) {
            it.domain.translations.en.abbreviation == "hp"
            it.domain.templateVersion == "0.5.1"
            it.allUpdates*.templateVersion == ["0.5.2", "1.0.0"]
            it.possibleUpdates*.templateVersion == ["0.5.2", "1.0.0"]
            it.latestUpdate.templateVersion == "1.0.0"
            it.latestPossibleUpdate.templateVersion == "1.0.0"
        }
        with(updates.find { it.domain.name == "esdProtection" }) {
            it.domain.translations.en.abbreviation == "esdp"
            it.domain.templateVersion == "1.8.3"
            it.allUpdates*.templateVersion == ["2.0.0"]
            it.possibleUpdates*.templateVersion == ["2.0.0"]
            it.latestUpdate.templateVersion == "2.0.0"
            it.latestPossibleUpdate.templateVersion == "2.0.0"
        }
    }

    def "up-to-date domains are omitted"() {
        given:
        domainTemplateDataRepository.saveAll([
            newDomainTemplate {
                name = "lärmschutz"
                templateVersion = Version.parse("0.5.0")
            },
            newDomainTemplate {
                name = "lärmschutz"
                templateVersion = Version.parse("0.5.1")
            },
        ])

        and:
        domainDataRepository.saveAll([
            newDomain(client) {
                name = "lärmschutz"
                templateVersion = Version.parse("0.5.1")
                translations = new Translated<>([:])
            }
        ])

        when:
        def updates = parseJson(get("/domains/updates"))

        then:
        updates.size() == 0
    }

    def "major updates cannot be skipped"() {
        given:
        domainTemplateDataRepository.saveAll([
            newDomainTemplate {
                name = "schmutzSchutz"
                templateVersion = Version.parse("0.1.0")
            },
            newDomainTemplate {
                name = "schmutzSchutz"
                templateVersion = Version.parse("1.0.0")
            },
            newDomainTemplate {
                name = "schmutzSchutz"
                templateVersion = Version.parse("1.0.1")
            },
            newDomainTemplate {
                name = "schmutzSchutz"
                templateVersion = Version.parse("1.1.0")
            },
            newDomainTemplate {
                name = "schmutzSchutz"
                templateVersion = Version.parse("2.0.0")
            },
        ])

        and:
        domainDataRepository.saveAll([
            newDomain(client) {
                name = "schmutzSchutz"
                templateVersion = Version.parse("0.1.0")
                translations = new Translated<>([:])
            }
        ])

        when:
        def updates = parseJson(get("/domains/updates"))

        then: "it is not possible to skip 1.*.* and update directly to 2.0.0"
        updates.size() == 1
        with(updates.find { it.domain.name == "schmutzSchutz" }) {
            it.domain.templateVersion == "0.1.0"
            it.allUpdates*.templateVersion == [
                "1.0.0",
                "1.0.1",
                "1.1.0",
                "2.0.0"
            ]
            it.possibleUpdates*.templateVersion == ["1.0.0", "1.0.1", "1.1.0"]
            it.latestUpdate.templateVersion == "2.0.0"
            it.latestPossibleUpdate.templateVersion == "1.1.0"
        }
    }

    def "warnings are generated for update conflicts caused by added attributes"() {
        given: "two DTs with a similar CA, but one has an additional attr"
        def templateA = domainTemplateDataRepository.save(newDomainTemplate {
            name = "AnimalProtection"
            templateVersion = Version.parse("1.0.0")
            translations.translations[Locale.ENGLISH] = new NameAbbreviationAndDescription("Animal protection", null, null)
            translations.translations[Locale.GERMAN] = new NameAbbreviationAndDescription("Tierschutz", null, null)
            applyElementTypeDefinition(newElementTypeDefinition(ElementType.ASSET, it) {
                subTypes.Pet = newSubTypeDefinition {}
                customAspects.movement = newCustomAspectDefinition {
                    attributeDefinitions.numberOfLegs = new IntegerAttributeDefinition()
                    attributeDefinitions.hasWings = new BooleanAttributeDefinition()
                    translations.put(Locale.ENGLISH, [
                        numberOfLegs        : "Number of legs",
                        hasWings            : "Has wings?",
                        asset_Pet_plural    : "Pets",
                        asset_Pet_singular  : "Pet",
                        asset_Pet_status_NEW: "New"])
                    translations.put(Locale.GERMAN, [
                        numberOfLegs        : "Anzahl der Beine",
                        hasWings            : "Hat Flügel?",
                        asset_Pet_plural    : "Haustiere",
                        asset_Pet_singular  : "Haustier",
                        asset_Pet_status_NEW: "Neu"])
                }
            })
        })
        def templateB = domainTemplateDataRepository.save(newDomainTemplate {
            name = "EnvironmentProtection"
            templateVersion = Version.parse("1.0.0")
            applyElementTypeDefinition(newElementTypeDefinition(ElementType.ASSET, it) {
                subTypes.Animal = newSubTypeDefinition {}
                customAspects.movement = newCustomAspectDefinition {
                    attributeDefinitions.numberOfLegs = new IntegerAttributeDefinition()
                }
            })
        })

        and: "an asset with conflicting values"
        def domainA = createTestDomain(client, templateA.id)
        def domainB = createTestDomain(client, templateB.id)
        def unit = unitDataRepository.save(newUnit(client) {
            addToDomains([domainA, domainB] as Set)
        })
        def assetId = assetDataRepository.save(newAsset(unit) {
            associateWithDomain(domainA, "Pet", "NEW")
            associateWithDomain(domainB, "Animal", "NEW")
            applyCustomAspectAttribute(domainA, "movement", "numberOfLegs", 2)
            applyCustomAspectAttribute(domainB, "movement", "numberOfLegs", 4)
        }).id

        and: "a minor update for a DT, adding an attribute and bringing the custom aspect in line with the other template"
        domainTemplateDataRepository.save(newDomainTemplate {
            name = "EnvironmentProtection"
            templateVersion = Version.parse("1.1.0")
            translations.translations[Locale.ENGLISH] = new NameAbbreviationAndDescription("Environment protection", null, null)
            translations.translations[Locale.GERMAN] = new NameAbbreviationAndDescription("Umweltschutz", null, null)
            applyElementTypeDefinition(newElementTypeDefinition(ElementType.ASSET, it) {
                subTypes.Animal = newSubTypeDefinition {}
                customAspects.movement = newCustomAspectDefinition {
                    attributeDefinitions.numberOfLegs = new IntegerAttributeDefinition()
                    attributeDefinitions.hasWings = new BooleanAttributeDefinition()
                }
            })
        })

        when: "evaluating the asset"
        def findings = parseJson(get("/domains/${domainB.id}/assets/$assetId")).with {
            parseJson(post("/domains/${domainB.id}/assets/evaluation", it, 200)).inspectionFindings
        }

        then:
        findings.size() == 1
        findings[0].description.en == "The object cannot be migrated to the new domain version 1.1.0: In Environment protection 1.1.0, some attributes are shared with other domains (Animal protection), but the object has deviating values in those domains:\n\n* Number of legs: 2\n\nPlease edit this object here or in the other domains to align the deviating values."
        findings[0].description.de == "Das Objekt ist nicht migrierbar auf die neue Domänen-Version 1.1.0: In Umweltschutz 1.1.0 werden einige Attribute gemeinsam genutzt mit anderen Domänen (Tierschutz). Dieses Objekt hat jedoch dort abweichende Werte:\n\n* Anzahl der Beine: 2\n\nBitte bearbeiten Sie das Objekt hier oder in den anderen Domänen, um die abweichenden Werte aneinander anzugleichen."

        when: "reevaluating with aligned values"
        findings = parseJson(get("/domains/${domainB.id}/assets/$assetId")).with {
            it.customAspects.movement.numberOfLegs = 2
            parseJson(post("/domains/${domainB.id}/assets/evaluation", it, 200)).inspectionFindings
        }

        then:
        findings.empty
    }

    def "warnings are generated for update conflicts due to renamed CA keys"() {
        given: "two DTs with a similar CA, but with different keys"
        def templateA = domainTemplateDataRepository.save(newDomainTemplate {
            name = "MISO 27000"
            templateVersion = Version.parse("1.0.0")
            translations.translations[Locale.ENGLISH] = new NameAbbreviationAndDescription("The MISO 27000", null, null)
            translations.translations[Locale.GERMAN] = new NameAbbreviationAndDescription("Die MISO 27000", null, null)
            applyElementTypeDefinition(newElementTypeDefinition(ElementType.ASSET, it) {
                subTypes.Soup = newSubTypeDefinition {}
                customAspects.noodles = newCustomAspectDefinition {
                    it.attributeDefinitions.hasNoodles = new BooleanAttributeDefinition()
                    translations.put(Locale.ENGLISH, [hasNoodles       : "contains noodles",
                        asset_Soup_plural: "Soups", asset_Soup_singular: "Soup", asset_Soup_status_NEW: "New"])
                    translations.put(Locale.GERMAN, [hasNoodles       : "enthält Nudeln",
                        asset_Soup_plural: "Suppen", asset_Soup_singular: "Suppe", asset_Soup_status_NEW: "Neu"])
                }
            })
        })
        def templateB = domainTemplateDataRepository.save(newDomainTemplate {
            name = "MISO 27001"
            templateVersion = Version.parse("1.0.0")
            applyElementTypeDefinition(newElementTypeDefinition(ElementType.ASSET, it) {
                subTypes.Soup = newSubTypeDefinition {}
                customAspects.nodles = newCustomAspectDefinition {
                    attributeDefinitions.hasNodles = new BooleanAttributeDefinition()
                }
            })
        })

        and: "an asset with conflicting values"
        def domainA = createTestDomain(client, templateA.id)
        def domainB = createTestDomain(client, templateB.id)
        def unit = unitDataRepository.save(newUnit(client) {
            addToDomains([domainA, domainB] as Set)
        })
        def assetId = assetDataRepository.save(newAsset(unit) {
            associateWithDomain(domainA, "Soup", "NEW")
            associateWithDomain(domainB, "Soup", "NEW")
            applyCustomAspectAttribute(domainA, "noodles", "hasNoodles", false)
            applyCustomAspectAttribute(domainB, "nodles", "hasNodles", true)
        }).id

        and: "a major update for a DT, renaming the CA and bringing it in line with the other template"
        domainTemplateDataRepository.save(newDomainTemplate {
            name = "MISO 27001"
            templateVersion = Version.parse("2.0.0")
            translations.translations[Locale.ENGLISH] = new NameAbbreviationAndDescription("The MISO 27000", null, null)
            translations.translations[Locale.GERMAN] = new NameAbbreviationAndDescription("Die MISO 27000", null, null)
            applyElementTypeDefinition(newElementTypeDefinition(ElementType.ASSET, it) {
                subTypes.Soup = newSubTypeDefinition {}
                customAspects.noodles = newCustomAspectDefinition {
                    it.attributeDefinitions.hasNoodles = new BooleanAttributeDefinition()
                }
            })
            domainMigrationDefinition = new DomainMigrationDefinition([
                new DomainMigrationStep("fix-key", TranslatedText.empty(), [
                    new CustomAspectAttribute(ElementType.ASSET, "nodles", "hasNodles")
                ], [
                    new CustomAspectMigrationTransformDefinition(new CustomAspectAttributeValueExpression("nodles", "hasNodles"),
                    new CustomAspectAttribute(ElementType.ASSET, "noodles", "hasNoodles"))
                ], false)
            ])
        })

        when: "evaluating the asset"
        def findings = parseJson(get("/domains/${domainB.id}/assets/$assetId")).with {
            parseJson(post("/domains/${domainB.id}/assets/evaluation", it, 200)).inspectionFindings
        }

        then:
        findings.size() == 1
        findings[0].description.en == "The object cannot be migrated to the new domain version 2.0.0: In The MISO 27000 2.0.0, some attributes are shared with other domains (The MISO 27000), but the object has deviating values in those domains:\n\n* contains noodles: no\n\nPlease edit this object here or in the other domains to align the deviating values."
        findings[0].description.de == "Das Objekt ist nicht migrierbar auf die neue Domänen-Version 2.0.0: In Die MISO 27000 2.0.0 werden einige Attribute gemeinsam genutzt mit anderen Domänen (Die MISO 27000). Dieses Objekt hat jedoch dort abweichende Werte:\n\n* enthält Nudeln: nein\n\nBitte bearbeiten Sie das Objekt hier oder in den anderen Domänen, um die abweichenden Werte aneinander anzugleichen."

        when: "aligning the values and reevaluating"
        findings = parseJson(get("/domains/${domainB.id}/assets/$assetId")).with {
            it.customAspects.nodles.hasNodles = false
            parseJson(post("/domains/${domainB.id}/assets/evaluation", it, 200)).inspectionFindings
        }

        then:
        findings.empty
    }

    def "warnings are generated for update conflicts due to redefined attributes"() {
        given: "a domain template defining a URL as a free text"
        def template = domainTemplateDataRepository.save(newDomainTemplate {
            name = "Secure webhosting"
            templateVersion = Version.parse("1.0.0")
            applyElementTypeDefinition(newElementTypeDefinition(ElementType.ASSET, it) {
                subTypes.Website = newSubTypeDefinition {}
                customAspects.basics = newCustomAspectDefinition {
                    attributeDefinitions.title = new TextAttributeDefinition()
                    attributeDefinitions.location = new TextAttributeDefinition()
                }
            })
        })

        and: "an asset with an invalid URL attribute"
        def domain = createTestDomain(client, template.id)
        def unit = unitDataRepository.save(newUnit(client) {
            addToDomains([domain] as Set)
        })
        def assetId = assetDataRepository.save(newAsset(unit) {
            associateWithDomain(domain, "Website", "NEW")
            applyCustomAspectAttribute(domain, "basics", "location", "something-example")
        }).id

        and: "a major update for a DT, changing the attribute's type"
        domainTemplateDataRepository.save(newDomainTemplate {
            name = "Secure webhosting"
            templateVersion = Version.parse("2.0.0")
            applyElementTypeDefinition(newElementTypeDefinition(ElementType.ASSET, it) {
                subTypes.Website = newSubTypeDefinition {}
                customAspects.basics = newCustomAspectDefinition {
                    it.attributeDefinitions.title = new TextAttributeDefinition()
                    it.attributeDefinitions.location = new ExternalDocumentAttributeDefinition()
                }
                translations = [
                    (Locale.ENGLISH): [
                        asset_Website_singular: "Website",
                        asset_Website_plural: "Websites",
                        asset_Website_status_NEW: "New",
                        title: "Title",
                        location: "Address"
                    ],
                    (Locale.GERMAN) : [
                        asset_Website_singular: "Website",
                        asset_Website_plural: "Websites",
                        asset_Website_status_NEW: "Neu",
                        title: "Titel",
                        location: "Adresse"
                    ],
                ]
            })
            domainMigrationDefinition = new DomainMigrationDefinition([
                new DomainMigrationStep("fix-url-type", TranslatedText.empty(), [
                    new CustomAspectAttribute(ElementType.ASSET, "basics", "location")
                ], [
                    new CustomAspectMigrationTransformDefinition(new CustomAspectAttributeValueExpression("basics", "location"),
                    new CustomAspectAttribute(ElementType.ASSET, "basics", "location"))
                ], false)
            ])
        })

        when: "evaluating the asset"
        def findings = parseJson(get("/domains/${domain.id}/assets/$assetId")).with {
            parseJson(post("/domains/${domain.id}/assets/evaluation", it, 200)).inspectionFindings
        }

        then:
        findings.size() == 1
        // TODO #919 expect translated CA name
        findings[0].description.en == "The object cannot be migrated to the new domain version 2.0.0: Invalid value 'something-example' for attribute 'Address': URL does not contain a valid protocol - the following protocols are supported: http, https, ftp, ftps, smb"
        findings[0].description.de == "Das Objekt ist nicht migrierbar auf die neue Domänen-Version 2.0.0: Ungültiger Wert 'something-example' für Attribut 'Adresse': URL enthält kein gültiges Protokoll - die folgenden Protokolle werden unterstützt: http, https, ftp, ftps, smb"

        when: "aligning the values and reevaluating"
        findings = parseJson(get("/domains/${domain.id}/assets/$assetId")).with {
            it.customAspects.basics.location = "https://something.example"
            parseJson(post("/domains/${domain.id}/assets/evaluation", it, 200)).inspectionFindings
        }

        then:
        findings.empty
    }

    def "warnings are generated for update conflicts due to attributes that need to be manually removed"() {
        given: "a domain template defining a URL as a free text"
        def template = domainTemplateDataRepository.save(newDomainTemplate {
            name = "Job applications"
            translations.translations[Locale.ENGLISH] = new NameAbbreviationAndDescription("Job applications", null, null)
            translations.translations[Locale.GERMAN] = new NameAbbreviationAndDescription("Bewerbungen", null, null)
            templateVersion = Version.parse("1.0.0")
            applyElementTypeDefinition(newElementTypeDefinition(ElementType.PERSON, it) {
                subTypes.Applicant = newSubTypeDefinition {}
                customAspects.physique = newCustomAspectDefinition {
                    attributeDefinitions.weight = new IntegerAttributeDefinition()
                }
                translations = [
                    (Locale.ENGLISH): [
                        person_Applicant_singular: "Applicant",
                        person_Applicant_plural: "Applicants",
                        person_Applicant_status_NEW: "New",
                        weight: 'Weight'

                    ],
                    (Locale.GERMAN) : [
                        person_Applicant_singular: "Bewerber:in",
                        person_Applicant_plural: "Bewerber:innen",
                        person_Applicant_status_NEW: "New",
                        weight: 'Gewicht'
                    ]
                ]
            })
        })

        and: "a person with an obsolete attribute"
        def domain = createTestDomain(client, template.id)
        def unit = unitDataRepository.save(newUnit(client) {
            addToDomains([domain] as Set)
        })
        def personId = personDataRepository.save(newPerson(unit) {
            associateWithDomain(domain, "Applicant", "NEW")
            applyCustomAspectAttribute(domain, "physique", "weight", 120)
        }).id

        and: "a major update for a DT, setting the attribute to be manually removed"
        domainTemplateDataRepository.save(newDomainTemplate {
            name = "Job applications"
            templateVersion = Version.parse("2.0.0")
            applyElementTypeDefinition(newElementTypeDefinition(ElementType.PERSON, it) {
                subTypes.Applicant = newSubTypeDefinition {}
                translations = [
                    (Locale.ENGLISH): [
                        person_Applicant_singular: "Applicant",
                        person_Applicant_plural: "Applicants",
                        person_Applicant_status_NEW: "New"

                    ],
                    (Locale.GERMAN) : [
                        person_Applicant_singular: "Bewerber:in",
                        person_Applicant_plural: "Bewerber:innen",
                        person_Applicant_status_NEW: "New"
                    ]
                ]
            })
            domainMigrationDefinition = new DomainMigrationDefinition([
                new DomainMigrationStep("remove-inappropriate-data", TranslatedText.empty(), [
                    new CustomAspectAttribute(ElementType.PERSON, "physique", "weight")
                ], null, true)
            ])
        })

        when: "evaluating the person"
        def findings = parseJson(get("/domains/${domain.id}/persons/$personId")).with {
            parseJson(post("/domains/${domain.id}/persons/evaluation", it, 200)).inspectionFindings
        }

        then:
        findings.size() == 1
        // TODO #919 expect translated CA name
        findings[0].description.en == 'The object cannot be migrated to the new domain version 2.0.0: In Job applications 1.0.0, the attribute \'weight\' does no longer exist. The value \'120\' needs to be manually removed.'
        findings[0].description.de == 'Das Objekt ist nicht migrierbar auf die neue Domänen-Version 2.0.0: In Bewerbungen 1.0.0, gibt es das Attribut \'weight\' nicht mehr. Der Wert \'120\' muss manuell entfernt werden.'

        when: "removing the values and reevaluating"
        findings = parseJson(get("/domains/${domain.id}/persons/$personId")).with {
            it.customAspects.remove('physique')
            parseJson(post("/domains/${domain.id}/assets/evaluation", it, 200)).inspectionFindings
        }

        then:
        findings.empty
    }

    def "no false update warnings are generated for links"() {
        given: "a domain template with a link"
        def template = domainTemplateDataRepository.save(newDomainTemplate {
            name = "Secure webhosting"
            templateVersion = Version.parse("1.0.0")
            applyElementTypeDefinition(newElementTypeDefinition(ElementType.ASSET, it) {
                subTypes.Website = newSubTypeDefinition {}
                links.hyperlinkedWebsite = newLinkDefinition(ElementType.ASSET, "Website")
            })
        })

        and: "an asset with a link"
        def domain = createTestDomain(client, template.id)
        def unit = unitDataRepository.save(newUnit(client) {
            addToDomains([domain] as Set)
        })
        def linkedAsset = assetDataRepository.save(newAsset(unit) {
            associateWithDomain(domain, "Website", "NEW")
        })
        def mainAssetId = assetDataRepository.save(newAsset(unit) {
            associateWithDomain(domain, "Website", "NEW")
            applyLink(newCustomLink(linkedAsset, "hyperlinkedWebsite", domain))
        }).id

        and: "a major update for a DT (actual changes don't matter)"
        domainTemplateDataRepository.save(newDomainTemplate {
            name = "Secure webhosting"
            templateVersion = Version.parse("2.0.0")
            applyElementTypeDefinition(newElementTypeDefinition(ElementType.ASSET, it) {
                subTypes.Website = newSubTypeDefinition {}
                links.hyperlinkedWebsite = newLinkDefinition(ElementType.ASSET, "Website")
                translations = [
                    (Locale.ENGLISH): [
                        asset_Website_singular: "Website",
                        asset_Website_plural: "Websites",
                        asset_Website_status_NEW: "New",
                        hyperlinkedWebsite: "Hyperlinked website",
                    ],
                    (Locale.GERMAN) : [
                        asset_Website_singular: "Website",
                        asset_Website_plural: "Websites",
                        asset_Website_status_NEW: "Neu",
                        hyperlinkedWebsite: "Verlinkte Webseite",
                    ],
                ]
            })
        })

        when: "evaluating the asset"
        def findings = parseJson(get("/domains/${domain.id}/assets/$mainAssetId")).with {
            parseJson(post("/domains/${domain.id}/assets/evaluation", it, 200)).inspectionFindings
        }

        then:
        findings.empty
    }
}
