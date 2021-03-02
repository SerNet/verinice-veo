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
import org.veo.core.entity.CustomLink
import org.veo.core.entity.CustomProperties
import org.veo.core.entity.Document
import org.veo.core.entity.Domain
import org.veo.core.entity.EntityLayerSupertype
import org.veo.core.entity.Incident
import org.veo.core.entity.Key
import org.veo.core.entity.ModelObject
import org.veo.core.entity.Person
import org.veo.core.entity.Process
import org.veo.core.entity.Scenario
import org.veo.core.entity.Scope
import org.veo.core.entity.Unit
import org.veo.core.entity.Versioned
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.entity.jpa.AssetData
import org.veo.persistence.entity.jpa.ClientData
import org.veo.persistence.entity.jpa.ControlData
import org.veo.persistence.entity.jpa.CustomLinkData
import org.veo.persistence.entity.jpa.CustomPropertiesData
import org.veo.persistence.entity.jpa.DocumentData
import org.veo.persistence.entity.jpa.DomainData
import org.veo.persistence.entity.jpa.IncidentData
import org.veo.persistence.entity.jpa.PersonData
import org.veo.persistence.entity.jpa.ProcessData
import org.veo.persistence.entity.jpa.ScenarioData
import org.veo.persistence.entity.jpa.ScopeData
import org.veo.persistence.entity.jpa.UnitData
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import spock.lang.Specification

/**
 * Base class for veo specifications
 */
abstract class VeoSpec extends Specification {
    private static EntityFactory factory = new EntityDataFactory()

    static AssetData newAsset(Unit owner, @DelegatesTo(value = Asset.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Asset") Closure init = null) {
        return factory.createAsset(null, owner).tap {
            VeoSpec.execute(it, init)
            VeoSpec.initEntityLayerSupertype(it)
        }
    }


    static ClientData newClient(@DelegatesTo(value = Client.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Client") Closure init = null) {
        return factory.createClient(Key.newUuid(), null).tap {
            VeoSpec.execute(it, init)
            if (it.name == null) {
                it.name = it.modelType + it.id
            }
            version(it)
        }
    }

    static ControlData newControl(Unit owner, @DelegatesTo(value = Control.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Control") Closure init = null) {
        return factory.createControl(null, owner).tap {
            VeoSpec.execute(it, init)
            VeoSpec.initEntityLayerSupertype(it)
        }
    }


    static DocumentData newDocument(Unit owner, @DelegatesTo(value = Document.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Document") Closure init = null) {
        return factory.createDocument(null, owner).tap {
            VeoSpec.execute(it, init)
            VeoSpec.initEntityLayerSupertype(it)
        }
    }



    static IncidentData newIncident(Unit owner, @DelegatesTo(value = Incident.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Incident") Closure init = null) {
        return factory.createIncident(null, owner).tap {
            VeoSpec.execute(it, init)
            VeoSpec.initEntityLayerSupertype(it)
        }
    }


    static ScenarioData newScenario(Unit owner, @DelegatesTo(value = Scenario.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Scenario") Closure init = null) {
        return factory.createScenario(null, owner).tap {
            VeoSpec.execute(it, init)
            VeoSpec.initEntityLayerSupertype(it)
        }
    }


    static DomainData newDomain(@DelegatesTo(value = Domain.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Domain") Closure init = null) {
        return factory.createDomain(null).tap {
            VeoSpec.execute(it, init)
            name(it)
            version(it)
        }
    }

    static PersonData newPerson(Unit owner, @DelegatesTo(value = Person.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Person") Closure init = null) {
        return factory.createPerson(null, owner).tap {
            VeoSpec.execute(it, init)
            VeoSpec.initEntityLayerSupertype(it)
        }
    }



    static ProcessData newProcess(Unit owner, @DelegatesTo(value = Process.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Process") Closure init = null) {
        return factory.createProcess(null, owner).tap {
            VeoSpec.execute(it, init)
            VeoSpec.initEntityLayerSupertype(it)
        }
    }


    static ScopeData newScope(Unit owner, @DelegatesTo(value = Scope.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Scope") Closure init = null) {
        return factory.createScope(null, owner).tap {
            VeoSpec.execute(it, init)
            VeoSpec.initEntityLayerSupertype(it)
        }
    }

    static UnitData newUnit(Client client, @DelegatesTo(value = Unit.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Unit") Closure init = null) {
        return factory.createUnit(null, null).tap {
            it.client = client
            VeoSpec.execute(it, init)
            name(it)
            version(it)
        }
    }

    static CustomPropertiesData newCustomProperties(String type, @DelegatesTo(value = CustomProperties.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.CustomProperties") Closure init = null) {
        return factory.createCustomProperties().tap{
            it.type = type
            VeoSpec.execute(it, init)
        }
    }

    static CustomLinkData newCustomLink(EntityLayerSupertype linkSource, EntityLayerSupertype linkTarget, @DelegatesTo(value = CustomLink.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.CustomLink") Closure init = null) {
        return factory.createCustomLink(null, linkSource, linkTarget).tap{
            VeoSpec.execute(it, init)
            if(it.name == null) {
                it.name = "link from $linkSource.name to $linkTarget.name"
            }
        }
    }

    private static def execute(Object target, Closure init) {
        if (init != null) {
            init.delegate = target
            init.resolveStrategy = Closure.DELEGATE_FIRST
            init.call(target)
        }
    }

    private static def name(ModelObject target) {
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
        version(target)
    }



    private static def version(Versioned target) {
        if(target.createdBy == null) {
            target.createdBy = "VeoRestMvcSpec entity factory"
        }
        if(target.createdAt == null) {
            target.createdAt = Instant.now()
        }
        if(target.updatedBy == null) {
            target.updatedBy = target.createdBy
        }
        if(target.updatedAt == null) {
            target.updatedAt = target.createdAt
        }
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
