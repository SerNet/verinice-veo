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

import java.time.Instant
import java.util.regex.Matcher
import java.util.regex.Pattern

import org.veo.core.entity.Asset
import org.veo.core.entity.Client
import org.veo.core.entity.Control
import org.veo.core.entity.Document
import org.veo.core.entity.Domain
import org.veo.core.entity.EntityLayerSupertype
import org.veo.core.entity.Key
import org.veo.core.entity.Nameable
import org.veo.core.entity.Person
import org.veo.core.entity.Process
import org.veo.core.entity.Unit
import org.veo.core.entity.groups.AssetGroup
import org.veo.core.entity.groups.ControlGroup
import org.veo.core.entity.groups.DocumentGroup
import org.veo.core.entity.groups.PersonGroup
import org.veo.core.entity.groups.ProcessGroup
import org.veo.persistence.entity.jpa.AssetData
import org.veo.persistence.entity.jpa.ClientData
import org.veo.persistence.entity.jpa.ControlData
import org.veo.persistence.entity.jpa.DocumentData
import org.veo.persistence.entity.jpa.DomainData
import org.veo.persistence.entity.jpa.PersonData
import org.veo.persistence.entity.jpa.ProcessData
import org.veo.persistence.entity.jpa.UnitData
import org.veo.persistence.entity.jpa.groups.AssetGroupData
import org.veo.persistence.entity.jpa.groups.ControlGroupData
import org.veo.persistence.entity.jpa.groups.DocumentGroupData
import org.veo.persistence.entity.jpa.groups.PersonGroupData
import org.veo.persistence.entity.jpa.groups.ProcessGroupData

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import spock.lang.Specification

/**
 * Base class for veo specifications
 */
abstract class VeoSpec extends Specification {
    static AssetData newAsset(Unit owner, @DelegatesTo(value = Asset.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Asset") Closure init = null) {
        return new AssetData().tap {
            id = Key.newUuid()
            it.owner = owner
            execute(it, init)
            initEntityLayerSupertype(it)
        }
    }

    static AssetGroupData newAssetGroup(Unit owner, @DelegatesTo(value = AssetGroup.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.AssetGroup") Closure init = null) {
        return new AssetGroupData().tap {
            id = Key.newUuid()
            it.owner = owner
            execute(it, init)
            initEntityLayerSupertype(it)
        }
    }

    static ClientData newClient(@DelegatesTo(value = Client.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Client") Closure init = null) {
        return new ClientData().tap {
            id = Key.newUuid()
            execute(it, init)
            if (it.name == null) {
                it.name = it.modelType + it.id
            }
        }
    }

    static ControlData newControl(Unit owner, @DelegatesTo(value = Control.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Control") Closure init = null) {
        return new ControlData().tap {
            id = Key.newUuid()
            it.owner = owner
            execute(it, init)
            initEntityLayerSupertype(it)
        }
    }

    static ControlGroupData newControlGroup(Unit owner, @DelegatesTo(value = ControlGroup.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.ControlGroup") Closure init = null) {
        return new ControlGroupData().tap {
            id = Key.newUuid()
            it.owner = owner
            execute(it, init)
            initEntityLayerSupertype(it)
        }
    }

    static DocumentData newDocument(Unit owner, @DelegatesTo(value = Document.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Document") Closure init = null) {
        return new DocumentData().tap {
            id = Key.newUuid()
            it.owner = owner
            execute(it, init)
            initEntityLayerSupertype(it)
        }
    }

    static DocumentGroupData newDocumentGroup(Unit owner, @DelegatesTo(value = DocumentGroup.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.DocumentGroup") Closure init = null) {
        return new DocumentGroupData().tap {
            id = Key.newUuid()
            it.owner = owner
            execute(it, init)
            initEntityLayerSupertype(it)
        }
    }

    static DomainData newDomain(@DelegatesTo(value = Domain.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Domain") Closure init = null) {
        return new DomainData().tap {
            id = Key.newUuid()
            execute(it, init)
            name(it)
        }
    }

    static PersonData newPerson(Unit owner, @DelegatesTo(value = Person.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Person") Closure init = null) {
        return new PersonData().tap {
            id = Key.newUuid()
            it.owner = owner
            execute(it, init)
            initEntityLayerSupertype(it)
        }
    }

    static PersonGroupData newPersonGroup(Unit owner, @DelegatesTo(value = PersonGroup.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.PersonGroup") Closure init = null) {
        return new PersonGroupData().tap {
            id = Key.newUuid()
            it.owner = owner
            execute(it, init)
            initEntityLayerSupertype(it)
        }
    }

    static ProcessData newProcess(Unit owner, @DelegatesTo(value = Process.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Process") Closure init = null) {
        return new ProcessData().tap {
            id = Key.newUuid()
            it.owner = owner
            execute(it, init)
            initEntityLayerSupertype(it)
        }
    }

    static ProcessGroupData newProcessGroup(Unit owner, @DelegatesTo(value = ProcessGroup.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.ProcessGroup") Closure init = null) {
        return new ProcessGroupData().tap {
            id = Key.newUuid()
            it.owner = owner
            execute(it, init)
            initEntityLayerSupertype(it)
        }
    }

    static UnitData newUnit(Client client, @DelegatesTo(value = Unit.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Unit") Closure init = null) {
        return new UnitData().tap {
            id = Key.newUuid()
            it.client = client
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

    private static def name(Nameable target) {
        if (target.name == null) {
            target.name = target.modelType + " " + target.id
        }
    }

    private static initEntityLayerSupertype(EntityLayerSupertype target) {
        if(target.customAspects == null) {
            target.customAspects = []
        }
        if(target.domains == null) {
            target.domains = []
        }
        if(target.links == null) {
            target.links = []
        }
        name(target)
    }

    static String getTextBetweenQuotes(String text) {
        Pattern p = Pattern.compile("\"([^\"]*)\"")
        Matcher m = p.matcher(text)
        if (m.find()) {
            return m.group(1)
        } else {
            return text
        }
    }
}
