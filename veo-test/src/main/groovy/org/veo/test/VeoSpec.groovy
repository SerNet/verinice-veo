/*******************************************************************************
 * Copyright (c) 2019 Urs Zeidler.
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
package org.veo.test

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType

import org.veo.core.entity.Asset
import org.veo.core.entity.Client
import org.veo.core.entity.Control
import org.veo.core.entity.Document
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.NameAble
import org.veo.core.entity.Person
import org.veo.core.entity.Process
import org.veo.core.entity.Unit
import org.veo.core.entity.impl.AssetImpl
import org.veo.core.entity.impl.ClientImpl
import org.veo.core.entity.impl.ControlImpl
import org.veo.core.entity.impl.DocumentImpl
import org.veo.core.entity.impl.DomainImpl
import org.veo.core.entity.impl.PersonImpl
import org.veo.core.entity.impl.ProcessImpl
import org.veo.core.entity.impl.UnitImpl
import spock.lang.Specification

/**
 * Base class for veo specifications
 */
abstract class VeoSpec extends Specification {

    Asset newAsset(Unit owner, @DelegatesTo(value = Asset.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Asset") Closure init = null) {
        return new AssetImpl(Key.newUuid(), null, owner).tap {
            execute(it, init)
            name(it)
        }
    }

    Client newClient(@DelegatesTo(value = Client.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Client") Closure init = null) {
        return new ClientImpl(Key.newUuid(), null).tap {
            execute(it, init)
            if (it.name == null) {
                it.name = it.modelType + it.id
            }
        }
    }

    Control newControl(Unit owner, @DelegatesTo(value = Control.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Control") Closure init = null) {
        return new ControlImpl(Key.newUuid(), null, owner).tap {
            execute(it, init)
            name(it)
        }
    }

    Document newDocument(Unit owner, @DelegatesTo(value = Document.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Document") Closure init = null) {
        return new DocumentImpl(Key.newUuid(), null, owner).tap {
            execute(it, init)
            name(it)
        }
    }

    Domain newDomain(@DelegatesTo(value = Domain.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Domain") Closure init = null) {
        return new DomainImpl(Key.newUuid(), null).tap {
            execute(it, init)
            name(it)
        }
    }

    Person newPerson(Unit owner, @DelegatesTo(value = Person.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Person") Closure init = null) {
        return new PersonImpl(Key.newUuid(), null, owner).tap {
            execute(it, init)
            name(it)
        }
    }

    Process newProcess(Unit owner, @DelegatesTo(value = Process.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Process") Closure init = null) {
        return new ProcessImpl(Key.newUuid(), null, owner).tap {
            execute(it, init)
            name(it)
        }
    }

    Unit newUnit(Client client, @DelegatesTo(value = Unit.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Unit") Closure init = null) {
        return new UnitImpl(Key.newUuid(), null, client).tap {
            execute(it, init)
            name(it)
        }
    }

    private static def execute(Object target, Closure init) {
        if (init != null) {
            init.delegate = target
            init.resolveStrategy = Closure.DELEGATE_FIRST
            init.call(target)
        }
    }

    private static def name(NameAble target) {
        if (target.name == null) {
            target.name = target.modelType + " " + target.id
        }
    }
}
