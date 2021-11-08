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
package org.veo.adapter

import org.veo.adapter.presenter.api.common.ReferenceAssembler
import org.veo.adapter.presenter.api.dto.CustomLinkDto
import org.veo.adapter.presenter.api.dto.ElementTypeDefinitionDto
import org.veo.adapter.presenter.api.dto.full.FullAssetDto
import org.veo.adapter.service.domaintemplate.DomainTemplateAssembler
import org.veo.adapter.service.domaintemplate.SyntheticIdRef
import org.veo.adapter.service.domaintemplate.dto.TransformCatalogDto
import org.veo.core.entity.Asset
import org.veo.core.entity.TailoringReferenceType

import spock.lang.Specification

class DomainTemplateAssemblerSpec extends Specification{
    def refAssembler = Mock(ReferenceAssembler)
    def domainTemplateAssembler = new DomainTemplateAssembler(refAssembler, UUID.randomUUID().toString(), "my little template", "mla", "it's gonna be great", "ME", "2.0", "stable")

    def "creates template DTO with trivial data"() {
        when:
        def typeDefinitions = [
            asset: new ElementTypeDefinitionDto(),
            document: new ElementTypeDefinitionDto(),
        ];
        domainTemplateAssembler.setElementTypeDefinitions(typeDefinitions)
        def dto = domainTemplateAssembler.createDomainTemplateDto()

        then:
        dto.name == "my little template"
        dto.elementTypeDefinitions == typeDefinitions
    }

    def "adds element type definitions"() {
        given:
        def definitions = [
            asset: new ElementTypeDefinitionDto(),
            document: new ElementTypeDefinitionDto()
        ]

        when:
        domainTemplateAssembler.setElementTypeDefinitions(definitions)
        def dto = domainTemplateAssembler.createDomainTemplateDto()

        then:
        dto.elementTypeDefinitions == definitions
    }

    def "creates template DTO with a catalog item"() {
        given:
        def element = new FullAssetDto().tap{
            id = UUID.randomUUID()
            abbreviation = "AB"
            name = "asseto balsamico"
            domains = [:]
        }
        domainTemplateAssembler.addCatalog("food catalog", "FC_", [
            (element.id): element
        ])
        when:
        def dto = domainTemplateAssembler.createDomainTemplateDto()

        then:
        dto.catalogs.size() > 0
        with(dto.catalogs.first()) {
            name == "food catalog"
            with((it as TransformCatalogDto)) {
                catalogItems.size() == 1
                with(catalogItems.first()) {
                    it.namespace == "FC_AB"
                    it.element.name == "asseto balsamico"
                }
            }
        }
    }

    def "creates tailoring references for linked items"() {
        given:
        def linkTarget1 = new FullAssetDto().tap{
            id = UUID.randomUUID()
            domains = [:]
            name = "target a"
        }
        def linkTarget2 = new FullAssetDto().tap{
            id = UUID.randomUUID()
            domains = [:]
            name = "target b"
        }
        def linkTarget3 = new FullAssetDto().tap{
            id = UUID.randomUUID()
            domains = [:]
            name = "target c"
        }
        def linkSource = new FullAssetDto().tap{
            id = UUID.randomUUID()
            name = "sauce"
            domains = [:]
            links = [
                linkTypeA: [
                    new CustomLinkDto().tap{
                        target = SyntheticIdRef.from(linkTarget1.id, Asset)
                    },
                    new CustomLinkDto().tap {
                        target = SyntheticIdRef.from(linkTarget2.id, Asset)
                    }
                ],
                linkTypeB: [
                    new CustomLinkDto().tap {
                        target = SyntheticIdRef.from(linkTarget3.id, Asset)
                    }
                ]
            ]
        }
        domainTemplateAssembler.addCatalog("linked catalog", "LK_", [
            (linkTarget1.id): linkTarget1,
            (linkTarget2.id): linkTarget2,
            (linkTarget3.id): linkTarget3,
            (linkSource.id): linkSource,
        ])
        when:
        def dto = domainTemplateAssembler.createDomainTemplateDto()

        then:
        with((dto.catalogs.first() as TransformCatalogDto).catalogItems.sort { it.element.name }) { catalogItems ->
            size() == 4

            catalogItems[0].element.id == linkSource.id
            catalogItems[0].element.name == "sauce"
            catalogItems[0].tailoringReferences.size() == 3
            with(catalogItems[0].tailoringReferences) {
                it.size() == 3
                with (it.find { it.catalogItem.id == catalogItems[1].id }) {
                    referenceType == TailoringReferenceType.LINK
                    linkType == "linkTypeA"
                }
                with (it.find { it.catalogItem.id == catalogItems[2].id }) {
                    referenceType == TailoringReferenceType.LINK
                    linkType == "linkTypeA"
                }
                with (it.find { it.catalogItem.id == catalogItems[3].id }) {
                    referenceType == TailoringReferenceType.LINK
                    linkType == "linkTypeB"
                }
            }

            catalogItems[1].element.id == linkTarget1.id
            catalogItems[1].element.name == "target a"
            with(catalogItems[1].tailoringReferences) {
                it.size() == 1
                it.first().referenceType == TailoringReferenceType.LINK_EXTERNAL
                it.first().catalogItem.id == catalogItems[1].id
                it.first().linkType == "linkTypeA"
            }

            catalogItems[2].element.id == linkTarget2.id
            catalogItems[2].element.name == "target b"
            with(catalogItems[2].tailoringReferences) {
                it.size() == 1
                it.first().referenceType == TailoringReferenceType.LINK_EXTERNAL
                it.first().catalogItem.id == catalogItems[2].id
                it.first().linkType == "linkTypeA"
            }

            catalogItems[3].element.id == linkTarget3.id
            catalogItems[3].element.name == "target c"
            with(catalogItems[3].tailoringReferences) {
                it.size() == 1
                it.first().referenceType == TailoringReferenceType.LINK_EXTERNAL
                it.first().catalogItem.id == catalogItems[3].id
                it.first().linkType == "linkTypeB"
            }
        }
    }

    def "creates tailoring references for parts"() {
        given:
        def part1 = new FullAssetDto().tap {
            id = UUID.randomUUID()
            domains = [:]
            name = "part a"
        }

        def part2 = new FullAssetDto().tap {
            id = UUID.randomUUID()
            domains = [:]
            name = "part b"
        }
        def root = new FullAssetDto().tap{
            id = UUID.randomUUID()
            name = "composite root"
            domains = [:]
            parts = [
                SyntheticIdRef.from(part1.id, Asset),
                SyntheticIdRef.from(part2.id, Asset),
            ]
        }
        domainTemplateAssembler.addCatalog("composite catalog", "CC_", [
            (part1.id): part1,
            (part2.id): part2,
            (root.id): root
        ])
        when:
        def dto = domainTemplateAssembler.createDomainTemplateDto()

        then:
        dto.catalogs.size() > 0
        with((dto.catalogs.first() as TransformCatalogDto).catalogItems.sort{it.element.name}) {catalogItems ->
            catalogItems.size() == 3

            catalogItems[0].element.id == root.id
            with(catalogItems[0].tailoringReferences) {
                size() == 2
                it.find{it.catalogItem.id == catalogItems[1].id}.referenceType == TailoringReferenceType.COPY
            }
        }
    }
}
