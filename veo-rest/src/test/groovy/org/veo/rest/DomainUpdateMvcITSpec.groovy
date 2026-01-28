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
import org.veo.core.entity.NameAbbreviationAndDescription
import org.veo.core.entity.Translated

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
                templateVersion = "0.5.0"
            },
            newDomainTemplate {
                name = "lärmschutz"
                templateVersion = "0.5.1"
            },
            newDomainTemplate {
                name = "lärmschutz"
                templateVersion = "0.5.2"
            },
            newDomainTemplate {
                name = "lärmschutz"
                templateVersion = "1.0.0"
            },
            newDomainTemplate {
                name = "esdProtection"
                templateVersion = "1.8.3"
            },
            newDomainTemplate {
                name = "esdProtection"
                templateVersion = "2.0.0"
            },
        ])

        and:
        domainDataRepository.saveAll([
            newDomain(client) {
                name = "lärmschutz"
                templateVersion = "0.5.1"
                translations = new Translated<>([
                    (EN): new NameAbbreviationAndDescription("Hearing protection", "hp", "WHAT DID YOU SAY? I CAN'T HEAR YOU!")
                ])
            },
            newDomain(client) {
                name = "esdProtection"
                templateVersion = "1.8.3"
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
                templateVersion = "0.5.0"
            },
            newDomainTemplate {
                name = "lärmschutz"
                templateVersion = "0.5.1"
            },
        ])

        and:
        domainDataRepository.saveAll([
            newDomain(client) {
                name = "lärmschutz"
                templateVersion = "0.5.1"
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
                templateVersion = "0.1.0"
            },
            newDomainTemplate {
                name = "schmutzSchutz"
                templateVersion = "1.0.0"
            },
            newDomainTemplate {
                name = "schmutzSchutz"
                templateVersion = "1.0.1"
            },
            newDomainTemplate {
                name = "schmutzSchutz"
                templateVersion = "1.1.0"
            },
            newDomainTemplate {
                name = "schmutzSchutz"
                templateVersion = "2.0.0"
            },
        ])

        and:
        domainDataRepository.saveAll([
            newDomain(client) {
                name = "schmutzSchutz"
                templateVersion = "0.1.0"
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
}
