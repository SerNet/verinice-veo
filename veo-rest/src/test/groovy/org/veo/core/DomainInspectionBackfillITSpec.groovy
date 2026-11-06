/*
 * verinice.veo
 * Copyright (C) 2026  Jochen Kemnade
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
package org.veo.core

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.entity.Asset
import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
import org.veo.core.entity.TranslatedText
import org.veo.core.entity.condition.ConstantExpression
import org.veo.core.entity.inspection.Finding
import org.veo.core.entity.inspection.Inspection
import org.veo.core.entity.inspection.Severity
import org.veo.core.repository.DomainRepository
import org.veo.core.repository.InspectionFindingRepository
import org.veo.rest.DomainInspectionBackfill

class DomainInspectionBackfillITSpec extends VeoSpringSpec {

    @Autowired
    DomainRepository domainRepository

    @Autowired
    InspectionFindingRepository inspectionFindingRepository

    UUID clientId
    UUID unitId

    def setup() {
        clientId = createTestClient().id
        unitId = executeInTransaction {
            unitDataRepository.save(newUnit(clientRepository.getById(clientId))).id
        }
    }

    def "backfills findings and marks the domain as complete"() {
        given: "a domain that needs backfilling, with an asset created directly in the DB (no findings yet)"
        def domain = createDomain("needs backfill", false)
        def asset = createAsset(domain)

        expect:
        findingsOf(asset, domain).empty

        when:
        backfill()

        then:
        findingsOf(asset, domain).size() == 1
        backfillComplete(domain)
    }

    def "skips domains that are already complete"() {
        given:
        def domain = createDomain("already complete", true)
        def asset = createAsset(domain)

        when:
        backfill()

        then:
        findingsOf(asset, domain).empty
    }

    def "a failing domain is rolled back and does not affect the others"() {
        given:
        def brokenDomain = createDomain("broken", false)
        def healthyDomain = createDomain("healthy", false)

        and: "an asset in both domains (findings are stored per element and domain)"
        def asset = createAsset(brokenDomain, healthyDomain)

        and: "a repository that fails for one of the domains after it has written the findings"
        def failingRepository = Stub(InspectionFindingRepository) {
            updateInspections(_, _) >> { Domain d, boolean b ->
                inspectionFindingRepository.updateInspections(d, b)
                if (d.id == brokenDomain.id) {
                    throw new IllegalStateException("simulated failure after writing findings")
                }
            }
        }

        when:
        backfill(failingRepository)

        then: "the error is logged, not thrown, so the application would still start"
        noExceptionThrown()

        and: "the findings written for the broken domain are rolled back, and it will be retried"
        findingsOf(asset, brokenDomain).empty
        !backfillComplete(brokenDomain)

        and: "the healthy domain is done"
        findingsOf(asset, healthyDomain).size() == 1
        backfillComplete(healthyDomain)
    }

    def "running the backfill again does not duplicate findings"() {
        given:
        def domain = createDomain("twice", false)
        def asset = createAsset(domain)
        backfill()

        and: "the domain is flagged for backfilling once more"
        executeInTransaction {
            domainRepository.getById(domain.id, domain.owner.id).inspectionBackfillComplete = false
        }

        when:
        backfill()

        then:
        findingsOf(asset, domain).size() == 1
    }

    private void backfill(InspectionFindingRepository repository = inspectionFindingRepository) {
        new DomainInspectionBackfill(domainRepository, repository, txTemplate).backfillInspectionFindings()
    }

    /** A domain with one inspection that applies to every asset of subtype AST_Server. */
    private Domain createDomain(String domainName, boolean backfillComplete) {
        executeInTransaction {
            def client = clientRepository.getById(clientId)
            newDomain(client) { d ->
                name = domainName
                applyElementTypeDefinition(newElementTypeDefinition(ElementType.ASSET, d) {
                    subTypes = [AST_Server: newSubTypeDefinition()]
                })
                applyInspection("alwaysWarn", new Inspection(
                                Severity.WARNING,
                                new TranslatedText([(Locale.ENGLISH): "Always warns"]),
                                ElementType.ASSET,
                                "AST_Server",
                                new ConstantExpression(true)))
                inspectionBackfillComplete = backfillComplete
            }
            clientRepository.save(client).domains.find { it.name == domainName }
        }
    }

    private Asset createAsset(Domain... domains) {
        executeInTransaction {
            def unit = unitDataRepository.findById(unitId).orElseThrow()
            assetDataRepository.save(newAsset(unit) {
                domains.each { d ->
                    associateWithDomain(d, "AST_Server", "NEW")
                }
            })
        }
    }

    private Collection<Finding> findingsOf(Asset asset, Domain domain) {
        inspectionFindingRepository.findInspections(asset, domain)
    }

    private boolean backfillComplete(Domain domain) {
        domainRepository.getById(domain.id, domain.owner.id).inspectionBackfillComplete
    }
}