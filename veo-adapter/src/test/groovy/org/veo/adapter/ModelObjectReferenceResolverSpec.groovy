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
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContextFactory
import org.veo.core.entity.Asset
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Person
import org.veo.core.entity.Unit
import org.veo.core.usecase.repository.Repository
import org.veo.core.usecase.repository.RepositoryProvider

import spock.lang.Specification

class ModelObjectReferenceResolverSpec extends Specification {

    RepositoryProvider repositoryProvider = Mock()
    DtoToEntityContextFactory dtoToEntityContextFactory = Mock()
    ModelObjectReferenceResolver referenceResolver = new ModelObjectReferenceResolver(repositoryProvider, dtoToEntityContextFactory)

    Repository<Asset, Key<UUID>> assetRepo = Mock()
    Repository<Person, Key<UUID>> personRepo = Mock()
    DtoToEntityContext newContext = Mock()

    def setup() {
        repositoryProvider.getRepositoryFor(Asset) >> assetRepo
        repositoryProvider.getRepositoryFor(Person) >> personRepo
        dtoToEntityContextFactory.create() >> newContext
    }

    def "loads models into context"() {
        given: "an asset, a person and a client with a domain"
        def client = Mock(Client) {
            it.domains >> [
                Mock(Domain)
            ]
        }
        def unit = Mock(Unit) {
            it.client >> client
        }
        def asset = Mock(Asset) {
            it.id >> Key.newUuid()
            it.owner >> unit
        }
        def person = Mock(Person) {
            it.id >> Key.newUuid()
            it.owner >> unit
        }

        when: "resolving references to the model objects"
        def result = referenceResolver.loadIntoContext(client, [
            Mock(ModelObjectReference) {
                it.id >> asset.id.uuidValue()
                it.type >> Asset
            },
            Mock(ModelObjectReference) {
                it.id >> person.id.uuidValue()
                it.type >> Person
            }
        ])
        then: "the asset, the person and the domain are added to the new context"
        result == newContext
        1 * assetRepo.findById(asset.id) >> Optional.of(asset)
        1 * personRepo.findById(person.id) >> Optional.of(person)
        1 * newContext.addEntity(asset)
        1 * newContext.addEntity(person)
        1 * newContext.addEntity(client.domains[0])
        0 * newContext.addEntity(_)
    }

    def "ignores domain references"() {
        given: "a client with two domains"
        def client = Mock(Client) {
            it.domains >> [
                Mock(Domain),
                Mock(Domain)
            ]
        }

        when: "resolving a reference to another domain"
        def result = referenceResolver.loadIntoContext(client, [
            Mock(ModelObjectReference) {
                it.id >> Key.newUuid().uuidValue()
                it.type >> Domain
            }
        ])
        then: "the domain object reference is ignored and only the client's domains are added"
        result == newContext
        0 * repositoryProvider.getRepositoryFor(Domain)
        1 * newContext.addEntity(client.domains[0])
        1 * newContext.addEntity(client.domains[1])
        0 * newContext.addEntity(_)
    }
}
