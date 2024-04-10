/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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

import org.veo.adapter.presenter.api.common.IdRef
import org.veo.adapter.presenter.api.common.ReferenceAssembler
import org.veo.core.entity.Asset
import org.veo.core.entity.Client
import org.veo.core.entity.Key
import org.veo.core.entity.Person
import org.veo.core.entity.transform.IdentifiableFactory
import org.veo.core.repository.Repository
import org.veo.core.repository.RepositoryProvider
import org.veo.core.usecase.service.DbIdRefResolver
import org.veo.core.usecase.service.IdRefResolver

import spock.lang.Specification

class DbIdRefResolverSpec extends Specification {

    RepositoryProvider repositoryProvider = Mock()
    IdentifiableFactory identifiableFactory = Mock()
    Client client = Mock()
    IdRefResolver referenceResolver = new DbIdRefResolver(repositoryProvider, identifiableFactory, client)

    Repository<Asset> assetRepo = Mock()
    Repository<Person> personRepo = Mock()

    def setup() {
        repositoryProvider.getRepositoryBaseFor(Asset) >> assetRepo
        repositoryProvider.getRepositoryBaseFor(Person) >> personRepo
    }

    def "loads reference target from repo"() {
        given: "an asset"
        def asset = Mock(Asset) {
            it.id >> Key.newUuid()
            it.idAsString >> it.id.uuidValue()
            it.getModelInterface() >> Asset
        }

        when: "resolving references to the asset"
        def result = referenceResolver.resolve(IdRef.from(asset, Mock(ReferenceAssembler)))

        then: "the asset is returned"
        1 * assetRepo.findAllByRefs(Set.of(IdRef.from(asset, Mock(ReferenceAssembler))), client) >> [asset]
        result == asset

        and: "the client was validated"
        1 * asset.checkSameClient(client)
    }

    def "caches results"() {
        given: "two assets and two persons"
        def asset1 = Mock(Asset) {
            it.id >> Key.newUuid()
            it.idAsString >> it.id.uuidValue()
            it.getModelInterface() >> Asset
        }
        def asset2 = Mock(Asset) {
            it.id >> Key.newUuid()
            it.idAsString >> it.id.uuidValue()
            it.getModelInterface() >> Asset
        }
        def asset3 = Mock(Asset) {
            it.id >> Key.newUuid()
            it.idAsString >> it.id.uuidValue()
            it.getModelInterface() >> Asset
        }
        def person1 = Mock(Person) {
            it.id >> Key.newUuid()
            it.idAsString >> it.id.uuidValue()
            it.getModelInterface() >> Person
        }
        def person2 = Mock(Person) {
            it.id >> Key.newUuid()
            it.idAsString >> it.id.uuidValue()
            it.getModelInterface() >> Person
        }

        when: "resolving the references initially"
        referenceResolver.resolve(IdRef.from(asset1, Mock(ReferenceAssembler)))
        referenceResolver.resolve(IdRef.from(asset2, Mock(ReferenceAssembler)))
        referenceResolver.resolve(IdRef.from(person1, Mock(ReferenceAssembler)))
        referenceResolver.resolve(IdRef.from(person2, Mock(ReferenceAssembler)))

        then: "the entities are fetched from the repo"
        1 * assetRepo.findAllByRefs(Set.of(IdRef.from(asset1, Mock(ReferenceAssembler))), client) >> [asset1]
        1 * assetRepo.findAllByRefs(Set.of(IdRef.from(asset2, Mock(ReferenceAssembler))), client) >> [asset2]
        1 * personRepo.findAllByRefs(Set.of(IdRef.from(person1, Mock(ReferenceAssembler))), client) >> [person1]
        1 * personRepo.findAllByRefs(Set.of(IdRef.from(person2, Mock(ReferenceAssembler))), client) >> [person2]

        when: "resolving the references again"
        def retrievedAsset1 = referenceResolver.resolve(IdRef.from(asset1, Mock(ReferenceAssembler)))
        def retrievedAsset2 = referenceResolver.resolve(IdRef.from(asset2, Mock(ReferenceAssembler)))
        def retrievedPerson1 = referenceResolver.resolve(IdRef.from(person1, Mock(ReferenceAssembler)))
        def retrievedPerson2 = referenceResolver.resolve(IdRef.from(person2, Mock(ReferenceAssembler)))

        then: "they are not fetched again"
        0 * assetRepo.findAllByRefs(_, _)
        0 * personRepo.findAllByRefs(_, _)

        and: "they are returned from a cache"
        retrievedAsset1 == asset1
        retrievedAsset2 == asset2
        retrievedPerson1 == person1
        retrievedPerson2 == person2

        when: "resolving a cached and an uncached reference"
        def retrievedAssets = referenceResolver.resolve([
            IdRef.from(asset1, Mock(ReferenceAssembler)),
            IdRef.from(asset3, Mock(ReferenceAssembler))
        ] as Set)

        then: "only the uncached asset is fetched"
        1 * assetRepo.findAllByRefs(Set.of(IdRef.from(asset3, Mock(ReferenceAssembler))), client) >> [asset3]
        0 * assetRepo.findAllByRefs(_, _)
        retrievedAssets == [asset1, asset3] as Set
    }
}
