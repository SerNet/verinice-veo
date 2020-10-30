/*******************************************************************************
 * Copyright (c) 2020 Jonas Jordan.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.adapter

import org.veo.adapter.presenter.api.common.ModelObjectReference
import org.veo.adapter.presenter.api.common.ReferenceAssembler
import org.veo.core.entity.Asset
import org.veo.core.entity.Client
import org.veo.core.entity.Key
import org.veo.core.entity.Person
import org.veo.core.usecase.repository.Repository
import org.veo.core.usecase.repository.RepositoryProvider

import spock.lang.Specification

class ModelObjectReferenceResolverSpec extends Specification {

    RepositoryProvider repositoryProvider = Mock()
    Client client = Mock()
    ModelObjectReferenceResolver referenceResolver = new ModelObjectReferenceResolver(repositoryProvider, client)

    Repository<Asset, Key<UUID>> assetRepo = Mock()
    Repository<Person, Key<UUID>> personRepo = Mock()

    def setup() {
        repositoryProvider.getRepositoryFor(Asset) >> assetRepo
        repositoryProvider.getRepositoryFor(Person) >> personRepo
    }

    def "loads reference target from repo"() {
        given: "an asset"
        def asset = Mock(Asset) {
            it.id >> Key.newUuid()
            it.getModelInterface() >> Asset
        }

        when: "resolving references to the model objects"
        def result = referenceResolver.resolve(ModelObjectReference.from(asset, Mock(ReferenceAssembler)))
        then: "the asset is returned"
        1 * assetRepo.findById(asset.id) >> Optional.of(asset)
        result == asset
        and: "the client was validated"
        1 * asset.checkSameClient(client)
    }

    def "caches results"() {
        given: "two assets and two persons"
        def asset1 = Mock(Asset) {
            it.id >> Key.newUuid()
            it.getModelInterface() >> Asset
        }
        def asset2 = Mock(Asset) {
            it.id >> Key.newUuid()
            it.getModelInterface() >> Asset
        }
        def person1 = Mock(Person) {
            it.id >> Key.newUuid()
            it.getModelInterface() >> Person
        }
        def person2 = Mock(Person) {
            it.id >> Key.newUuid()
            it.getModelInterface() >> Person
        }

        when: "resolving the references initially"
        referenceResolver.resolve(ModelObjectReference.from(asset1, Mock(ReferenceAssembler)))
        referenceResolver.resolve(ModelObjectReference.from(asset2, Mock(ReferenceAssembler)))
        referenceResolver.resolve(ModelObjectReference.from(person1, Mock(ReferenceAssembler)))
        referenceResolver.resolve(ModelObjectReference.from(person2, Mock(ReferenceAssembler)))
        then: "the entities are fetched from the repo"
        1 * assetRepo.findById(asset1.id) >> Optional.of(asset1)
        1 * assetRepo.findById(asset2.id) >> Optional.of(asset2)
        1 * personRepo.findById(person1.id) >> Optional.of(person1)
        1 * personRepo.findById(person2.id) >> Optional.of(person2)

        when: "resolving the references again"
        def retrievedAsset1 = referenceResolver.resolve(ModelObjectReference.from(asset1, Mock(ReferenceAssembler)))
        def retrievedAsset2 = referenceResolver.resolve(ModelObjectReference.from(asset2, Mock(ReferenceAssembler)))
        def retrievedPerson1 = referenceResolver.resolve(ModelObjectReference.from(person1, Mock(ReferenceAssembler)))
        def retrievedPerson2 = referenceResolver.resolve(ModelObjectReference.from(person2, Mock(ReferenceAssembler)))
        then: "they are not fetched again"
        0 * assetRepo.findById(_)
        0 * assetRepo.findById(_)
        0 * personRepo.findById(_)
        0 * personRepo.findById(_)
        and: "they are returned from a cache"
        retrievedAsset1 == asset1
        retrievedAsset2 == asset2
        retrievedPerson1 == person1
        retrievedPerson2 == person2
    }
}
