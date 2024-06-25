/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
package org.veo.adapter.presenter.api

import static java.util.UUID.randomUUID

import org.veo.adapter.presenter.api.common.CompoundIdRef
import org.veo.adapter.presenter.api.common.IdRef
import org.veo.adapter.presenter.api.common.ReferenceAssembler
import org.veo.adapter.presenter.api.common.SymIdRef
import org.veo.core.entity.Asset
import org.veo.core.entity.AssetRisk
import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Scenario
import org.veo.core.entity.Unit
import org.veo.core.entity.ref.TypedCompoundId
import org.veo.core.entity.ref.TypedId
import org.veo.core.entity.ref.TypedSymbolicId

import spock.lang.Specification

class RefSpec extends Specification {

    ReferenceAssembler referenceAssembler = Mock()

    def "ref equals & hashcode is implemented correctly"() {
        given:
        def unit1 = Spy(Unit) {
            idAsString >> randomUUID()
            displayName >> ""
        }
        def unit1Doppelganger = Spy(Unit) {
            idAsString >> unit1.idAsString
            displayName >> ""
        }
        def unit2 = Spy(Unit) {
            idAsString >> randomUUID()
            displayName >> ""
        }
        def assetWithSameId = Spy(Asset) {
            idAsString >> unit1.idAsString
            displayName >> ""
        }

        expect:
        IdRef.from(unit1, referenceAssembler) == IdRef.from(unit1, referenceAssembler)
        IdRef.from(unit1, referenceAssembler).hashCode() == IdRef.from(unit1, referenceAssembler).hashCode()

        IdRef.from(unit1Doppelganger, referenceAssembler) == IdRef.from(unit1, referenceAssembler)
        IdRef.from(unit1Doppelganger, referenceAssembler).hashCode() == IdRef.from(unit1, referenceAssembler).hashCode()

        IdRef.from(unit1, referenceAssembler) != IdRef.from(unit2, referenceAssembler)
        IdRef.from(unit1, referenceAssembler).hashCode() != IdRef.from(unit2, referenceAssembler).hashCode()

        IdRef.from(unit1, referenceAssembler) != IdRef.from(assetWithSameId, referenceAssembler)
        IdRef.from(unit1, referenceAssembler).hashCode() != IdRef.from(assetWithSameId, referenceAssembler).hashCode()

        and:
        TypedId.from(unit1) == TypedId.from(unit1)
        TypedId.from(unit1).hashCode() == TypedId.from(unit1).hashCode()

        TypedId.from(unit1Doppelganger) == TypedId.from(unit1)
        TypedId.from(unit1Doppelganger).hashCode() == TypedId.from(unit1).hashCode()

        TypedId.from(unit1) != TypedId.from(unit2)
        TypedId.from(unit1).hashCode() != TypedId.from(unit2).hashCode()

        TypedId.from(unit1) != TypedId.from(assetWithSameId)
        TypedId.from(unit1).hashCode() != TypedId.from(assetWithSameId).hashCode()

        and:
        IdRef.from(unit1, referenceAssembler) == TypedId.from(unit1)
        TypedId.from(unit1) == IdRef.from(unit1, referenceAssembler)
        TypedId.from(unit1).hashCode() == IdRef.from(unit1, referenceAssembler).hashCode()

        IdRef.from(unit1, referenceAssembler) == TypedId.from(unit1Doppelganger)
        TypedId.from(unit1Doppelganger) == IdRef.from(unit1, referenceAssembler)
        TypedId.from(unit1Doppelganger).hashCode() == IdRef.from(unit1, referenceAssembler).hashCode()

        IdRef.from(unit2, referenceAssembler) != TypedId.from(unit1)
        TypedId.from(unit1) != IdRef.from(unit2, referenceAssembler)
        TypedId.from(unit1).hashCode() != IdRef.from(unit2, referenceAssembler).hashCode()

        IdRef.from(assetWithSameId, referenceAssembler) != TypedId.from(unit1)
        TypedId.from(unit1) != IdRef.from(assetWithSameId, referenceAssembler)
        TypedId.from(unit1).hashCode() != IdRef.from(assetWithSameId, referenceAssembler).hashCode()
    }

    def "sym ref equals & hashcode is implemented correctly"() {
        given:
        def domain = Spy(Domain) {
            id >> Key.newUuid()
            displayName >> ""
        }
        def domain2 = Spy(Domain) {
            id >> Key.newUuid()
            displayName >> ""
        }
        def ci = Spy(CatalogItem) {
            symbolicId >> Key.newUuid()
            displayName >> ""
            domainBase >> domain
        }
        def ci2 = Spy(CatalogItem) {
            symbolicId >> Key.newUuid()
            displayName >> ""
            domainBase >> domain
        }
        def ciDoppelganger = Spy(CatalogItem) {
            symbolicId >> ci.symbolicId
            displayName >> ""
            domainBase >> domain
        }
        def ciWithSameIdInOtherDomain = Spy(CatalogItem) {
            symbolicId >> ci.symbolicId
            displayName >> ""
            domainBase >> domain2
        }

        expect:
        SymIdRef.from(ci, referenceAssembler) == SymIdRef.from(ci, referenceAssembler)
        SymIdRef.from(ci, referenceAssembler).hashCode() == SymIdRef.from(ci, referenceAssembler).hashCode()

        SymIdRef.from(ciDoppelganger, referenceAssembler) == SymIdRef.from(ci, referenceAssembler)
        SymIdRef.from(ciDoppelganger, referenceAssembler).hashCode() == SymIdRef.from(ci, referenceAssembler).hashCode()

        SymIdRef.from(ci2, referenceAssembler) != SymIdRef.from(ci, referenceAssembler)
        SymIdRef.from(ci2, referenceAssembler).hashCode() != SymIdRef.from(ci, referenceAssembler).hashCode()

        SymIdRef.from(ciWithSameIdInOtherDomain, referenceAssembler) != SymIdRef.from(ci, referenceAssembler)
        SymIdRef.from(ciWithSameIdInOtherDomain, referenceAssembler).hashCode() != SymIdRef.from(ci, referenceAssembler).hashCode()

        and:
        TypedSymbolicId.from(ci) == TypedSymbolicId.from(ci)
        TypedSymbolicId.from(ci).hashCode() == TypedSymbolicId.from(ci).hashCode()

        TypedSymbolicId.from(ciDoppelganger) == TypedSymbolicId.from(ci)
        TypedSymbolicId.from(ciDoppelganger).hashCode() == TypedSymbolicId.from(ci).hashCode()

        TypedSymbolicId.from(ci2) != TypedSymbolicId.from(ci)
        TypedSymbolicId.from(ci2).hashCode() != TypedSymbolicId.from(ci).hashCode()

        TypedSymbolicId.from(ciWithSameIdInOtherDomain) != TypedSymbolicId.from(ci)
        TypedSymbolicId.from(ciWithSameIdInOtherDomain).hashCode() != TypedSymbolicId.from(ci).hashCode()

        and:
        SymIdRef.from(ci, referenceAssembler) == TypedSymbolicId.from(ci)
        TypedSymbolicId.from(ci) == SymIdRef.from(ci, referenceAssembler)
        TypedSymbolicId.from(ci).hashCode() == SymIdRef.from(ci, referenceAssembler).hashCode()

        SymIdRef.from(ciDoppelganger, referenceAssembler) == TypedSymbolicId.from(ci)
        TypedSymbolicId.from(ciDoppelganger) == SymIdRef.from(ci, referenceAssembler)
        TypedSymbolicId.from(ciDoppelganger).hashCode() == SymIdRef.from(ci, referenceAssembler).hashCode()

        SymIdRef.from(ci2, referenceAssembler) != TypedSymbolicId.from(ci)
        TypedSymbolicId.from(ci2) != SymIdRef.from(ci, referenceAssembler)
        TypedSymbolicId.from(ci2).hashCode() != SymIdRef.from(ci, referenceAssembler).hashCode()

        SymIdRef.from(ciWithSameIdInOtherDomain, referenceAssembler) != TypedSymbolicId.from(ci)
        TypedSymbolicId.from(ciWithSameIdInOtherDomain) != SymIdRef.from(ci, referenceAssembler)
        TypedSymbolicId.from(ciWithSameIdInOtherDomain).hashCode() != TypedSymbolicId.from(ci).hashCode()
    }

    def "compound ref equals & hashcode is implemented correctly"() {
        given:
        def asset1 = Spy(Asset) {
            id >> Key.newUuid()
            displayName >> ""
        }
        def asset2 = Spy(Asset) {
            id >> Key.newUuid()
            displayName >> ""
        }
        def scenario1 = Spy(Scenario) {
            id >> Key.newUuid()
            displayName >> ""
        }
        def scenario2 = Spy(Scenario) {
            id >> Key.newUuid()
            displayName >> ""
        }
        def risk = Spy(AssetRisk) {
            entity >> asset1
            scenario >> scenario1
        }
        def riskWithOtherScn = Spy(AssetRisk) {
            entity >> asset1
            scenario >> scenario2
        }
        def riskDoppelganger = Spy(AssetRisk) {
            entity >> asset1
            scenario >> scenario1
        }
        def riskWithOtherAsset = Spy(AssetRisk) {
            entity >> asset2
            scenario >> scenario1
        }

        expect:
        CompoundIdRef.from(risk, referenceAssembler) == CompoundIdRef.from(risk, referenceAssembler)
        CompoundIdRef.from(risk, referenceAssembler).hashCode() == CompoundIdRef.from(risk, referenceAssembler).hashCode()

        CompoundIdRef.from(riskDoppelganger, referenceAssembler) == CompoundIdRef.from(risk, referenceAssembler)
        CompoundIdRef.from(riskDoppelganger, referenceAssembler).hashCode() == CompoundIdRef.from(risk, referenceAssembler).hashCode()

        CompoundIdRef.from(riskWithOtherScn, referenceAssembler) != CompoundIdRef.from(risk, referenceAssembler)
        CompoundIdRef.from(riskWithOtherScn, referenceAssembler).hashCode() != CompoundIdRef.from(risk, referenceAssembler).hashCode()

        CompoundIdRef.from(riskWithOtherAsset, referenceAssembler) != CompoundIdRef.from(risk, referenceAssembler)
        CompoundIdRef.from(riskWithOtherAsset, referenceAssembler).hashCode() != CompoundIdRef.from(risk, referenceAssembler).hashCode()

        and:
        TypedCompoundId.from(risk) == TypedCompoundId.from(risk)
        TypedCompoundId.from(risk).hashCode() == TypedCompoundId.from(risk).hashCode()

        TypedCompoundId.from(riskDoppelganger) == TypedCompoundId.from(risk)
        TypedCompoundId.from(riskDoppelganger).hashCode() == TypedCompoundId.from(risk).hashCode()

        TypedCompoundId.from(riskWithOtherScn) != TypedCompoundId.from(risk)
        TypedCompoundId.from(riskWithOtherScn).hashCode() != TypedCompoundId.from(risk).hashCode()

        TypedCompoundId.from(riskWithOtherAsset) != TypedCompoundId.from(risk)
        TypedCompoundId.from(riskWithOtherAsset).hashCode() != TypedCompoundId.from(risk).hashCode()

        and:
        CompoundIdRef.from(risk, referenceAssembler) == TypedCompoundId.from(risk)
        TypedCompoundId.from(risk) == CompoundIdRef.from(risk, referenceAssembler)
        TypedCompoundId.from(risk).hashCode() == CompoundIdRef.from(risk, referenceAssembler).hashCode()

        CompoundIdRef.from(riskDoppelganger, referenceAssembler) == TypedCompoundId.from(risk)
        TypedCompoundId.from(riskDoppelganger) == CompoundIdRef.from(risk, referenceAssembler)
        TypedCompoundId.from(riskDoppelganger).hashCode() == CompoundIdRef.from(risk, referenceAssembler).hashCode()

        CompoundIdRef.from(riskWithOtherScn, referenceAssembler) != TypedCompoundId.from(risk)
        TypedCompoundId.from(riskWithOtherScn) != CompoundIdRef.from(risk, referenceAssembler)
        TypedCompoundId.from(riskWithOtherScn).hashCode() != CompoundIdRef.from(risk, referenceAssembler).hashCode()

        CompoundIdRef.from(riskWithOtherAsset, referenceAssembler) != TypedCompoundId.from(risk)
        TypedCompoundId.from(riskWithOtherAsset) != CompoundIdRef.from(risk, referenceAssembler)
        TypedCompoundId.from(riskWithOtherAsset).hashCode() != TypedCompoundId.from(risk).hashCode()
    }
}