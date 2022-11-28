/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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
package org.veo.test

import java.sql.ClientInfoStatus
import java.time.Instant
import java.time.temporal.ChronoUnit

import org.veo.core.entity.Asset
import org.veo.core.entity.Catalog
import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Client
import org.veo.core.entity.Control
import org.veo.core.entity.CustomAspect
import org.veo.core.entity.CustomLink
import org.veo.core.entity.Designated
import org.veo.core.entity.Document
import org.veo.core.entity.Domain
import org.veo.core.entity.DomainBase
import org.veo.core.entity.DomainTemplate
import org.veo.core.entity.Element
import org.veo.core.entity.ElementOwner
import org.veo.core.entity.EntityType
import org.veo.core.entity.Identifiable
import org.veo.core.entity.Incident
import org.veo.core.entity.ItemUpdateType
import org.veo.core.entity.Key
import org.veo.core.entity.LinkTailoringReference
import org.veo.core.entity.Person
import org.veo.core.entity.Process
import org.veo.core.entity.Scenario
import org.veo.core.entity.Scope
import org.veo.core.entity.TailoringReference
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.Unit
import org.veo.core.entity.UpdateReference
import org.veo.core.entity.Versioned
import org.veo.core.entity.definitions.ElementTypeDefinition
import org.veo.core.entity.definitions.SubTypeDefinition
import org.veo.core.entity.event.ClientChangedEvent
import org.veo.core.entity.event.ClientEvent
import org.veo.core.entity.inspection.Inspection
import org.veo.core.entity.inspection.Severity
import org.veo.core.entity.risk.ProbabilityImpl
import org.veo.core.entity.risk.RiskDefinitionRef
import org.veo.core.entity.risk.RiskValues
import org.veo.core.entity.riskdefinition.CategoryDefinition
import org.veo.core.entity.riskdefinition.CategoryLevel
import org.veo.core.entity.riskdefinition.ImplementationStateDefinition
import org.veo.core.entity.riskdefinition.ProbabilityDefinition
import org.veo.core.entity.riskdefinition.ProbabilityLevel
import org.veo.core.entity.riskdefinition.RiskDefinition
import org.veo.core.entity.riskdefinition.RiskMethod
import org.veo.core.entity.riskdefinition.RiskValue
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.entity.jpa.AssetData
import org.veo.persistence.entity.jpa.CatalogData
import org.veo.persistence.entity.jpa.CatalogItemData
import org.veo.persistence.entity.jpa.ClientData
import org.veo.persistence.entity.jpa.ControlData
import org.veo.persistence.entity.jpa.CustomAspectData
import org.veo.persistence.entity.jpa.CustomLinkData
import org.veo.persistence.entity.jpa.DocumentData
import org.veo.persistence.entity.jpa.DomainData
import org.veo.persistence.entity.jpa.DomainTemplateData
import org.veo.persistence.entity.jpa.IncidentData
import org.veo.persistence.entity.jpa.LinkTailoringReferenceData
import org.veo.persistence.entity.jpa.PersonData
import org.veo.persistence.entity.jpa.ProcessData
import org.veo.persistence.entity.jpa.ScenarioData
import org.veo.persistence.entity.jpa.ScopeData
import org.veo.persistence.entity.jpa.TailoringReferenceData
import org.veo.persistence.entity.jpa.UnitData
import org.veo.persistence.entity.jpa.UpdateReferenceData
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import spock.lang.Specification

/**
 * Base class for veo specifications
 */
abstract class VeoSpec extends Specification {
    private static EntityFactory factory = new EntityDataFactory()

    private static int designatorCounter = 1

    static AssetData newAsset(ElementOwner owner, @DelegatesTo(value = Asset.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Asset") Closure init = null) {
        return factory.createAsset(null, owner).tap {
            VeoSpec.execute(it, init)
            VeoSpec.initElement(it)
        }
    }

    static ClientData newClient(@DelegatesTo(value = Client.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Client") Closure init = null) {
        return factory.createClient(Key.newUuid(), null).tap {
            it.updateState(ClientEvent.ClientChangeType.ACTIVATION)
            VeoSpec.execute(it, init)
            if (it.name == null) {
                it.name = it.modelType + it.id
            }
            version(it)
        }
    }

    static ControlData newControl(ElementOwner owner, @DelegatesTo(value = Control.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Control") Closure init = null) {
        return factory.createControl(null, owner).tap {
            VeoSpec.execute(it, init)
            VeoSpec.initElement(it)
        }
    }

    static DocumentData newDocument(ElementOwner owner, @DelegatesTo(value = Document.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Document") Closure init = null) {
        return factory.createDocument(null, owner).tap {
            VeoSpec.execute(it, init)
            VeoSpec.initElement(it)
        }
    }

    static IncidentData newIncident(ElementOwner owner, @DelegatesTo(value = Incident.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Incident") Closure init = null) {
        return factory.createIncident(null, owner).tap {
            VeoSpec.execute(it, init)
            VeoSpec.initElement(it)
        }
    }

    static ScenarioData newScenario(ElementOwner owner, @DelegatesTo(value = Scenario.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Scenario") Closure init = null) {
        return factory.createScenario(null, owner).tap {
            VeoSpec.execute(it, init)
            VeoSpec.initElement(it)
        }
    }

    static DomainData newDomain(Client owner, @DelegatesTo(value = Domain.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Domain") Closure init = null) {
        return factory.createDomain(null, "", "").tap {
            VeoSpec.execute(it, init)
            VeoSpec.name(it)
            VeoSpec.version(it)
            owner.addToDomains(it)
            EntityType
                    .ELEMENT_TYPES
                    .collect { it.singularTerm }
                    .findAll { type -> it.findElementTypeDefinition(type).empty}
                    .each { type -> it.elementTypeDefinitions.add(newElementTypeDefinition(type, it)) }
        }
    }

    static ElementTypeDefinition newElementTypeDefinition(String type, Domain it, @DelegatesTo(value = ElementTypeDefinition.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.definitions.ElementTypeDefinition") Closure init = null) {
        return factory.createElementTypeDefinition(type, it).tap{
            VeoSpec.execute(it, init)
        }
    }

    static SubTypeDefinition newSubTypeDefinition(@DelegatesTo(value = SubTypeDefinition.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.definitions.SubTypeDefinition") Closure init = null) {
        return new SubTypeDefinition().tap{
            statuses = ["NEW"]
            VeoSpec.execute(it, init)
        }
    }

    static DomainTemplateData newDomainTemplate(@DelegatesTo(value = DomainTemplate.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.DomainTemplate") Closure init = null) {
        return factory.createDomainTemplate(null, "me", "1.0.0", Key.newUuid()).tap {
            VeoSpec.execute(it, init)
            VeoSpec.name(it)
            VeoSpec.version(it)
        }
    }

    static CatalogData newCatalog(DomainBase domainTemplate, @DelegatesTo(value = Catalog.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Catalog") Closure init = null) {
        return factory.createCatalog(domainTemplate).tap {
            VeoSpec.execute(it, init)
            VeoSpec.name(it)
            VeoSpec.version(it)
        }
    }

    static CatalogItemData newCatalogItem(Catalog catalog,@DelegatesTo(value = Element.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.CatalogItem") Closure elementSupplier, @DelegatesTo(value = CatalogItem.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.CatalogItem") Closure init = null) {
        return factory.createCatalogItem(catalog, elementSupplier).tap {
            VeoSpec.execute(it, init)
            VeoSpec.version(it)
        }
    }

    static TailoringReferenceData newTailoringReference(CatalogItem catalogItem, TailoringReferenceType type, @DelegatesTo(value = TailoringReference.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.TailoringReference") Closure init = null) {
        return factory.createTailoringReference(catalogItem, type).tap {
            VeoSpec.execute(it, init)
        }
    }

    static UpdateReferenceData newUpdateReference(CatalogItem catalogItem, ItemUpdateType type, @DelegatesTo(value = UpdateReference.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.UpdateReference") Closure init = null) {
        return factory.createUpdateReference(catalogItem, type).tap {
            VeoSpec.execute(it, init)
        }
    }

    static LinkTailoringReferenceData newLinkTailoringReference(CatalogItem catalogItem,
            TailoringReferenceType referenceType,
            @DelegatesTo(value = LinkTailoringReference.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.LinkTailoringReference") Closure init = null) {
        return factory.createLinkTailoringReference(catalogItem, referenceType).tap {
            VeoSpec.execute(it, init)
        }
    }

    static PersonData newPerson(ElementOwner owner, @DelegatesTo(value = Person.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Person") Closure init = null) {
        return factory.createPerson(null, owner).tap {
            VeoSpec.execute(it, init)
            VeoSpec.initElement(it)
        }
    }

    static ProcessData newProcess(ElementOwner owner, @DelegatesTo(value = Process.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Process") Closure init = null) {
        return factory.createProcess(null, owner).tap {
            VeoSpec.execute(it, init)
            VeoSpec.initElement(it)
        }
    }

    static ScopeData newScope(ElementOwner owner, @DelegatesTo(value = Scope.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Scope") Closure init = null) {
        return factory.createScope(null, owner).tap {
            VeoSpec.execute(it, init)
            VeoSpec.initElement(it)
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

    static CustomAspectData newCustomAspect(String type, @DelegatesTo(value = CustomAspect.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.CustomAspect") Closure init = null) {
        return factory.createCustomAspect(type).tap{
            VeoSpec.execute(it, init)
        }
    }

    static CustomLinkData newCustomLink(Element linkTarget, String type, @DelegatesTo(value = CustomLink.class)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.CustomLink") Closure init = null) {
        return factory.createCustomLink(linkTarget, null, type).tap{
            VeoSpec.execute(it, init)
        }
    }

    static ElementTypeDefinition newElementTypeDefinition(DomainBase domain, String type, @DelegatesTo(value = CustomLink.class)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.ElementTypeDefinition") Closure init = null) {
        return factory.createElementTypeDefinition(type, domain).tap{
            VeoSpec.execute(it, init)
        }
    }

    static RiskDefinition createRiskDefinition(String id, @DelegatesTo(value = RiskDefinition.class)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.riskdefinition.RiskDefintiion") Closure init = null) {
        RiskDefinition rd = new RiskDefinition()
        rd.id = id
        rd.riskValues = [
            new RiskValue(0,"#A0CF11","symbolic_risk_1",["de" : ["name": "gering","abbreviation":"1","description":"Die bereits umgesetzten oder zumindest im Sicherheitskonzept vorgesehenen Sicherheitsmaßnahmen bieten einen ausreichenden Schutz. In der Praxis ist es üblich, geringe Risiken zu akzeptieren und die Gefährdung dennoch zu beobachten."]]),
            new RiskValue(1,"#FFFF13","symbolic_risk_2",["de" : ["name": "mittel","abbreviation":"2","description":"Die bereits umgesetzten oder zumindest im Sicherheitskonzept vorgesehenen Sicherheitsmaßnahmen reichen möglicherweise nicht aus."]]),
            new RiskValue(2,"#FF8E43","symbolic_risk_3",["de" : ["name": "hoch","abbreviation":"3","description":"Die bereits umgesetzten oder zumindest im Sicherheitskonzept vorgesehenen Sicherheitsmaßnahmen bieten keinen ausreichenden Schutz vor der jeweiligen Gefährdung."]]),
            new RiskValue(3,"#FF1212","symbolic_risk_4",["de" :["name": "sehr hoch","abbreviation":"4","description":"Die bereits umgesetzten oder zumindest im Sicherheitskonzept vorgesehenen Sicherheitsmaßnahmen bieten keinen ausreichenden Schutz vor der jeweiligen Gefährdung. In der Praxis werden sehr hohe Risiken selten akzeptiert."]])
        ] as List

        def riskMatrix = [
            [
                rd.riskValues[0],
                rd.riskValues[0],
                rd.riskValues[0],
                rd.riskValues[0],
            ] as List,
            [
                rd.riskValues[0],
                rd.riskValues[0],
                rd.riskValues[1],
                rd.riskValues[2],
            ] as List,
            [
                rd.riskValues[1],
                rd.riskValues[1],
                rd.riskValues[2],
                rd.riskValues[3],
            ] as List,
            [
                rd.riskValues[1],
                rd.riskValues[2],
                rd.riskValues[3],
                rd.riskValues[3],
            ] as List
        ] as List

        rd.categories = [
            new CategoryDefinition("C",riskMatrix,
            createDefaultCategoryLevels(),
            ["de":["name":"Vertraulichkeit","abbreviation":"c","description": ""]]
            ),
            new CategoryDefinition("I",riskMatrix,
            createDefaultCategoryLevels(),
            ["de":["name":"Integrität","abbreviation":"i","description": ""]]
            ),
            new CategoryDefinition("A",riskMatrix,
            createDefaultCategoryLevels(),
            ["de":["name":"Belastbarkeit","abbreviation":"r","description": ""]]
            ),
            new CategoryDefinition("R",riskMatrix,
            createDefaultCategoryLevels(),
            ["de":["name":"Vertraulichkeit","abbreviation":"c","description": ""]]
            )
        ] as List

        rd.probability = new ProbabilityDefinition(["de":["name":"Wahrscheinlichkit","abbreviation":"w","description":""]],[
            new ProbabilityLevel(0,"#004643",["de":["name":"selten","abbreviation":"1","description":"Ereignis könnte nach heutigem Kenntnisstand höchstens alle fünf Jahre eintreten."]]),
            new ProbabilityLevel(1,"#004643",["de":["name":"mittel","abbreviation":"2","description":"Ereignis tritt einmal alle fünf Jahre bis einmal im Jahr ein."]]),
            new ProbabilityLevel(2,"#004643",["de":["name":"häufig","abbreviation":"3","description":"Ereignis tritt einmal im Jahr bis einmal pro Monat ein."]]),
            new ProbabilityLevel(3,"#004643",["de":["name":"sehr häufig","abbreviation":"4","description":"Ereignis tritt mehrmals im Monat ein."]])
        ] as List)

        rd.implementationStateDefinition = new ImplementationStateDefinition(["de":["name":"Umsetzungsstatus","abbreviation":"s","description":""]],[
            new CategoryLevel(0,"#12AE0F",["de":["name":"ja","abbreviation":"J","description":"Die Maßnahme ist vollständig umgesetzt."]]),
            new CategoryLevel(1,"#AE0D11",["de":["name":"nein","abbreviation":"N","description":"Die Maßnahme ist nicht umgesetzt."]]),
            new CategoryLevel(2,"#EDE92F",["de":["name":"teilweise","abbreviation":"Tw","description":"Die Maßnahme ist nicht vollständig umgesetzt."]]),
            new CategoryLevel(3,"#49A2ED",["de":["name":"nicht anwendbar","abbreviation":"NA","description":"Die Maßnahme ist für den Betrachtungsgegenstand nicht anwendbar."]])
        ] as List)
        rd.riskMethod = new RiskMethod(["en":["impactMethod":"highwatermark","description":"description"]])

        execute(rd, init)
        return rd
    }

    static CategoryLevel newCategoryLevel(String name, @DelegatesTo(value = CategoryLevel.class)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.riskdefinition.CategoryLevel") Closure init = null) {
        CategoryLevel categoryLevel = new CategoryLevel("#000000")
        execute(categoryLevel, init)
        return categoryLevel
    }

    static RiskDefinition newRiskDefinition(String id, @DelegatesTo(value = RiskDefinition.class)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.riskdefinition.RiskDefinition") Closure init = null) {
        def rd = new RiskDefinition()
        rd.id = id
        execute(rd, init)
        return rd
    }

    static CategoryDefinition newCategoryDefinition(String id, @DelegatesTo(value = CategoryDefinition.class)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.riskdefinition.CategoryDefinition") Closure init = null) {
        def rd = new CategoryDefinition()
        rd.id = id
        execute(rd, init)
        return rd
    }

    static RiskValues newRiskValues(RiskDefinitionRef riskDefinitionRef, Domain domain, @DelegatesTo(value = RiskValues.class)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.risk.RiskValues") Closure init = null) {
        return new RiskValues(new ProbabilityImpl(), [], [], new Key<String>(riskDefinitionRef.idRef), domain.id).tap {
            VeoSpec.execute(it, init)
        }
    }

    static Inspection newInspection(@DelegatesTo(value = Inspection.class)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.inspection.Inspection") Closure init = null) {
        return new Inspection(Severity.HINT, [:]).tap {
            VeoSpec.execute(it, init)
        }
    }

    private static List<CategoryLevel> createDefaultCategoryLevels() {
        return [
            new CategoryLevel(0,"#004643",["de":["name":"vernachlässigbar","abbreviation":"1","description":"Die Schadensauswirkungen sind gering und können vernachlässigt werden."]]),
            new CategoryLevel(1,"#004643",["de":["name":"begrenzt","abbreviation":"2","description":"Die Schadensauswirkungen sind begrenzt und überschaubar."]]),
            new CategoryLevel(2,"#004643",["de":["name":"beträchtlich","abbreviation":"3","description":"Die Schadensauswirkungen können beträchtlich sein."]]),
            new CategoryLevel(3,"#004643",["de":["name":"existenzbedrohend","abbreviation":"4","description":"Die Schadensauswirkungen können ein existenziell bedrohliches, katastrophales Ausmaß erreichen."]])
        ] as List
    }

    private static def execute(Object target, Closure init) {
        if (init != null) {
            init.delegate = target
            init.resolveStrategy = Closure.DELEGATE_FIRST
            init.call(target)
        }
    }

    private static def name(Identifiable target) {
        if (target.name == null) {
            target.name = target.modelType + " " + target.id
        }
    }

    private static initElement(Element target) {
        if(target.designator == null) {
            assignDesignator(target)
        }
        if(target.customAspects == null) {
            target.customAspects = []
        }
        if(target.links == null) {
            target.links = []
        }
        name(target)
        version(target)
    }

    static def assignDesignator(Designated target) {
        target.designator = "${target.typeDesignator}-${designatorCounter++}"
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

    static Instant roundToMicros(Instant instant) {
        instant.plusNanos(500).truncatedTo(ChronoUnit.MICROS)
    }
}
