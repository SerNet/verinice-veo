/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman.
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
package org.veo.persistence.entity.jpa

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.Asset
import org.veo.core.entity.Person
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository

class ScopeJpaSpec extends AbstractJpaSpec {

    @Autowired
    UnitDataRepository unitRepository

    @Autowired
    ClientDataRepository clientRepository

    @Autowired
    TransactionTemplate txTemplate

    ClientData client
    UnitData unit
    AssetData asset
    AssetData assetComposite
    PersonData person
    PersonData personComposite

    def setup() {
        client = clientRepository.save(newClient())
        unit = unitRepository.save(newUnit(client))
        asset = newAsset(unit)
        assetComposite = newAsset(unit) {
            parts = [asset]
        }
        person = newPerson(unit)
        personComposite = newPerson(unit) {
            parts = [person]
        }
    }

    def "save and load a scope"() {
        when:"saving a scope with subcomposites and elements"
        assetComposite = assetDataRepository.save(assetComposite)
        personComposite = personDataRepository.save(personComposite)

        def scope = newScope(unit) {
            members = [
                assetComposite,
                personComposite
            ]
        }
        def scopeId = txTemplate.execute {
            scopeDataRepository.save(scope).id
        }

        then: "the scope can be retrieved"
        def retrievedScope = scopeDataRepository.findById(scopeId).get()

        and: "it contains the composites"
        retrievedScope.members.size() == 2
        retrievedScope.members.contains assetComposite
        retrievedScope.members.contains personComposite

        and: "the composites contain the elements"
        def retrievedAssetComposite = retrievedScope.members.findAll {it instanceof Asset}.first()
        def retrievedPersonComposite = retrievedScope.members.findAll {it instanceof Person}.first()
        retrievedAssetComposite.parts == [asset] as Set
        retrievedPersonComposite.parts == [person] as Set
    }

    def "delete a scope containing composites"() {
        given: "A nested structure of scopes, composites and elements"
        def scope = txTemplate.execute {
            assetDataRepository.save(assetComposite)
            personDataRepository.save(personComposite)
            scopeDataRepository.save(newScope(unit) {
                members = [
                    assetComposite,
                    personComposite,
                    asset,
                    person
                ]
            })
        }

        expect: "the members were saved"
        def savedScope = scopeDataRepository.findById(scope.id)
        savedScope.present
        savedScope.get().members.size() == 4

        when: "the scope is removed"
        txTemplate.execute{
            scope.remove()
            scopeDataRepository.delete(scope)
        }

        then: "the scope is removed but the composites remain"
        scopeDataRepository.findById(scope.id).empty
        assetDataRepository.findById(asset.id).present
        assetDataRepository.findById(assetComposite.id).present
        personDataRepository.findById(person.id).present
        personDataRepository.findById(personComposite.id).present
    }

    def "delete composites that are memebers of a scope"() {
        given: "A nested structure of scopes, composites and elements"
        def scope = txTemplate.execute {
            assetComposite = assetDataRepository.save(assetComposite)
            asset = assetDataRepository.save(asset)
            personComposite = personDataRepository.save(personComposite)
            person = personDataRepository.save(person)
            scopeDataRepository.save(newScope(unit) {
                members = [
                    assetComposite,
                    personComposite,
                    asset,
                    person
                ]
            })
        }

        expect: "the scope has all members"
        def savedScope = scopeDataRepository.findById(scope.id)
        savedScope.get().members.size() == 4

        when: "the composites are removed"
        txTemplate.execute{
            assetDataRepository.delete(assetComposite)
            personDataRepository.delete(personComposite)
        }

        then: "the scope still contains all members"
        // this behaviour is expected - scope members have to be removed by business logic
        // when elements are removed. Currently this is implemented in the repositories
        // (caution: not JPA-repositories)
        def persistedScope = scopeDataRepository.findById(scope.id)
        persistedScope.present
        persistedScope.get().members.size() == 4
        persistedScope.get().members.contains(asset)
        persistedScope.get().members.contains(assetComposite)
        persistedScope.get().members.contains(person)
        persistedScope.get().members.contains(personComposite)
    }
}
