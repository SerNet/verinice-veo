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
package org.veo.adapter.presenter.api.response.transformer;

import static org.veo.core.entity.risk.DomainRiskReferenceProvider.referencesForDomain;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.veo.adapter.IdRefResolver;
import org.veo.adapter.presenter.api.dto.AbstractAssetDto;
import org.veo.adapter.presenter.api.dto.AbstractControlDto;
import org.veo.adapter.presenter.api.dto.AbstractDocumentDto;
import org.veo.adapter.presenter.api.dto.AbstractIncidentDto;
import org.veo.adapter.presenter.api.dto.AbstractPersonDto;
import org.veo.adapter.presenter.api.dto.AbstractProcessDto;
import org.veo.adapter.presenter.api.dto.AbstractScenarioDto;
import org.veo.adapter.presenter.api.dto.AbstractScopeDto;
import org.veo.adapter.presenter.api.dto.ControlDomainAssociationDto;
import org.veo.adapter.presenter.api.dto.ControlRiskValuesDto;
import org.veo.adapter.presenter.api.dto.DomainAssociationDto;
import org.veo.adapter.presenter.api.dto.ProcessDomainAssociationDto;
import org.veo.adapter.presenter.api.dto.ProcessRiskValuesDto;
import org.veo.adapter.presenter.api.dto.ScenarioDomainAssociationDto;
import org.veo.adapter.presenter.api.dto.ScenarioRiskValuesDto;
import org.veo.adapter.presenter.api.dto.ScopeDomainAssociationDto;
import org.veo.adapter.service.domaintemplate.SyntheticIdRef;
import org.veo.core.entity.Asset;
import org.veo.core.entity.Control;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.aspects.Aspect;
import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.risk.ControlRiskValues;
import org.veo.core.entity.risk.DomainRiskReferenceProvider;
import org.veo.core.entity.risk.ImpactRef;
import org.veo.core.entity.risk.ImplementationStatusRef;
import org.veo.core.entity.risk.PotentialProbabilityImpl;
import org.veo.core.entity.risk.ProbabilityRef;
import org.veo.core.entity.risk.ProcessImpactValues;
import org.veo.core.entity.risk.RiskDefinitionRef;

/**
 * Maps {@link Domain} associations of {@link Element}s between entities and DTOs. See {@link
 * DomainAssociationDto}.
 */
public class DomainAssociationTransformer {

  public void mapDomainsToEntity(
      AbstractAssetDto source, Asset target, IdRefResolver idRefResolver) {
    mapToEntity(source.getDomains(), target, idRefResolver);
  }

  public void mapDomainsToEntity(
      AbstractControlDto source, Control target, IdRefResolver idRefResolver) {
    mapToEntity(
        source.getDomains(),
        target,
        idRefResolver,
        (domain, associationDto) ->
            target.setRiskValues(
                domain,
                associationDto.getRiskValues().entrySet().stream()
                    .collect(groupRiskDefinitionsByDomain(domain))));
  }

  private Collector<
          Map.Entry<String, ControlRiskValuesDto>, ?, Map<RiskDefinitionRef, ControlRiskValues>>
      groupRiskDefinitionsByDomain(DomainBase domain) {
    var referenceProvider = referencesForDomain(domain);
    return Collectors.toMap(
        kv -> referenceProvider.getRiskDefinitionRef(kv.getKey()),
        kv -> mapControlRiskValuesDto2Entity(kv.getKey(), kv.getValue(), referenceProvider));
  }

  private ControlRiskValues mapControlRiskValuesDto2Entity(
      String riskDefinitionId,
      ControlRiskValuesDto riskValuesDto,
      DomainRiskReferenceProvider referenceProvider) {
    var riskValues = new ControlRiskValues();
    riskValues.setImplementationStatus(
        Optional.ofNullable(riskValuesDto.getImplementationStatus())
            .map(status -> referenceProvider.getImplementationStatus(riskDefinitionId, status))
            .orElse(null));
    return riskValues;
  }

  public void mapDomainsToEntity(
      AbstractDocumentDto source, Document target, IdRefResolver idRefResolver) {
    mapToEntity(source.getDomains(), target, idRefResolver);
  }

  public void mapDomainsToEntity(
      AbstractIncidentDto source, Incident target, IdRefResolver idRefResolver) {
    mapToEntity(source.getDomains(), target, idRefResolver);
  }

  public void mapDomainsToEntity(
      AbstractPersonDto source, Person target, IdRefResolver idRefResolver) {
    mapToEntity(source.getDomains(), target, idRefResolver);
  }

  public void mapDomainsToEntity(
      AbstractProcessDto source, Process target, IdRefResolver idRefResolver) {
    mapToEntity(
        source.getDomains(),
        target,
        idRefResolver,
        (domain, associationDto) -> {
          var referenceProvider = referencesForDomain(domain);
          target.setImpactValues(
              domain,
              associationDto.getRiskValues().entrySet().stream()
                  .collect(
                      Collectors.toMap(
                          e -> referenceProvider.getRiskDefinitionRef(e.getKey()),
                          e ->
                              mapProcessImpactValues(
                                  e.getKey(), e.getValue(), referenceProvider))));
        });
  }

  private ProcessImpactValues mapProcessImpactValues(
      String riskDefinitionId,
      ProcessRiskValuesDto value,
      DomainRiskReferenceProvider referenceProvider) {
    var riskValues = new ProcessImpactValues();

    Map<CategoryRef, ImpactRef> potentialImpacts =
        value.getPotentialImpacts().entrySet().stream()
            .collect(
                Collectors.toMap(
                    e -> toCategoryRef(riskDefinitionId, referenceProvider, e), Entry::getValue));
    riskValues.setPotentialImpacts(potentialImpacts);
    return riskValues;
  }

  private CategoryRef toCategoryRef(
      String riskDefinitionId,
      DomainRiskReferenceProvider referenceProvider,
      Entry<String, ImpactRef> e) {
    return referenceProvider.getCategoryRef(riskDefinitionId, e.getKey());
  }

  public void mapDomainsToEntity(
      AbstractScenarioDto source, Scenario target, IdRefResolver idRefResolver) {
    mapToEntity(
        source.getDomains(),
        target,
        idRefResolver,
        (domain, associationDto) ->
            target.setPotentialProbability(
                domain,
                associationDto.getRiskValues().entrySet().stream()
                    .collect(groupScenarioRiskValuesByDomain(domain))));
  }

  private Collector<
          Map.Entry<String, ScenarioRiskValuesDto>,
          ?,
          Map<RiskDefinitionRef, PotentialProbabilityImpl>>
      groupScenarioRiskValuesByDomain(DomainBase domain) {
    var referenceProvider = referencesForDomain(domain);
    return Collectors.toMap(
        kv -> toRiskDefinitionRef(kv.getKey(), domain),
        kv -> mapScenarioRiskValuesDto2Entity(kv.getKey(), kv.getValue(), referenceProvider));
  }

  private PotentialProbabilityImpl mapScenarioRiskValuesDto2Entity(
      String riskDefinitionId,
      ScenarioRiskValuesDto riskValuesDto,
      DomainRiskReferenceProvider referenceProvider) {
    var riskValues = new PotentialProbabilityImpl();

    riskValues.setPotentialProbability(
        Optional.ofNullable(riskValuesDto.getPotentialProbability())
            .map(pp -> referenceProvider.getProbabilityRef(riskDefinitionId, pp))
            .orElse(null));
    riskValues.setPotentialProbabilityExplanation(
        riskValuesDto.getPotentialProbabilityExplanation());
    return riskValues;
  }

  public void mapDomainsToEntity(
      AbstractScopeDto source, Scope target, IdRefResolver idRefResolver) {
    mapToEntity(
        source.getDomains(),
        target,
        idRefResolver,
        (domain, associationDto) ->
            target.setRiskDefinition(
                domain, toRiskDefinitionRef(associationDto.getRiskDefinition(), domain)));
  }

  public void mapDomainsToDto(Asset source, AbstractAssetDto target) {
    target.setDomains(extractDomainAssociations(source, DomainAssociationDto::new));
  }

  public void mapDomainsToDto(Control source, AbstractControlDto target) {
    Map<String, ControlDomainAssociationDto> extractDomainAssociations =
        extractDomainAssociations(
            source,
            domain -> {
              var associationDto = new ControlDomainAssociationDto();
              source
                  .getRiskValues(domain)
                  .ifPresent(
                      riskValues ->
                          associationDto.setRiskValues(
                              riskValues.entrySet().stream()
                                  .collect(
                                      Collectors.toMap(
                                          kv -> kv.getKey().getIdRef(),
                                          this::mapControlRiskValuesToDto))));
              return associationDto;
            });
    target.setDomains(extractDomainAssociations);
  }

  private ControlRiskValuesDto mapControlRiskValuesToDto(
      Map.Entry<RiskDefinitionRef, ControlRiskValues> entry) {
    var riskValuesDto = new ControlRiskValuesDto();
    riskValuesDto.setImplementationStatus(
        Optional.ofNullable(entry.getValue().getImplementationStatus())
            .map(ImplementationStatusRef::getOrdinalValue)
            .orElse(null));
    return riskValuesDto;
  }

  private ProcessRiskValuesDto mapProcessRiskValuesToDto(
      Map.Entry<RiskDefinitionRef, ProcessImpactValues> entry) {
    var riskValuesDto = new ProcessRiskValuesDto();
    Map<CategoryRef, ImpactRef> potentialImpacts = entry.getValue().getPotentialImpacts();
    Map<String, ImpactRef> riskValues =
        potentialImpacts.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().getIdRef(), Entry::getValue));
    riskValuesDto.setPotentialImpacts(riskValues);
    return riskValuesDto;
  }

  public void mapDomainsToDto(Document source, AbstractDocumentDto target) {
    target.setDomains(extractDomainAssociations(source, DomainAssociationDto::new));
  }

  public void mapDomainsToDto(Incident source, AbstractIncidentDto target) {
    target.setDomains(extractDomainAssociations(source, DomainAssociationDto::new));
  }

  public void mapDomainsToDto(Person source, AbstractPersonDto target) {
    target.setDomains(extractDomainAssociations(source, DomainAssociationDto::new));
  }

  public void mapDomainsToDto(Process source, AbstractProcessDto target) {
    target.setDomains(
        extractDomainAssociations(
            source,
            domain -> {
              var assocationDto = new ProcessDomainAssociationDto();
              source
                  .getImpactValues(domain)
                  .ifPresent(
                      riskValues -> {
                        Collector<
                                Entry<RiskDefinitionRef, ProcessImpactValues>,
                                ?,
                                Map<String, ProcessRiskValuesDto>>
                            collector =
                                Collectors.toMap(
                                    kv -> kv.getKey().getIdRef(), this::mapProcessRiskValuesToDto);
                        Map<String, ProcessRiskValuesDto> values =
                            riskValues.entrySet().stream().collect(collector);
                        assocationDto.setRiskValues(values);
                      });
              return assocationDto;
            }));
  }

  public void mapDomainsToDto(Scenario source, AbstractScenarioDto target) {
    target.setDomains(
        extractDomainAssociations(
            source,
            domain -> {
              var assocationDto = new ScenarioDomainAssociationDto();
              source
                  .getPotentialProbability(domain)
                  .ifPresent(
                      riskValues ->
                          assocationDto.setRiskValues(
                              riskValues.entrySet().stream()
                                  .collect(
                                      Collectors.toMap(
                                          kv -> kv.getKey().getIdRef(),
                                          this::mapScenarioRiskValuesToDto))));
              return assocationDto;
            }));
  }

  private ScenarioRiskValuesDto mapScenarioRiskValuesToDto(
      Map.Entry<RiskDefinitionRef, PotentialProbabilityImpl> entry) {
    var riskValuesDto = new ScenarioRiskValuesDto();
    riskValuesDto.setPotentialProbability(
        Optional.ofNullable(entry.getValue().getPotentialProbability())
            .map(ProbabilityRef::getIdRef)
            .orElse(null));
    riskValuesDto.setPotentialProbabilityExplanation(
        entry.getValue().getPotentialProbabilityExplanation());
    return riskValuesDto;
  }

  public void mapDomainsToDto(Scope source, AbstractScopeDto target) {
    target.setDomains(
        extractDomainAssociations(
            source,
            domain -> {
              var assocationDto = new ScopeDomainAssociationDto();
              source
                  .getRiskDefinition(domain)
                  .map(RiskDefinitionRef::getIdRef)
                  .ifPresent(assocationDto::setRiskDefinition);
              return assocationDto;
            }));
  }

  private <T extends DomainAssociationDto> Map<String, T> extractDomainAssociations(
      Element source, Supplier<T> supplier) {
    return extractDomainAssociations(source, domain -> supplier.get());
  }

  private <T extends DomainAssociationDto> Map<String, T> extractDomainAssociations(
      Element source, Function<DomainBase, T> supplier) {
    // Catalog item elements in a domain template may have aspects for
    // domains that
    // the element is not assigned to. Seems invalid, but that's the
    // situation we
    // have to deal with here.
    var domains = new HashSet<DomainBase>();
    domains.addAll(source.getDomains());
    domains.addAll(
        source.getSubTypeAspects().stream().map(Aspect::getDomain).collect(Collectors.toSet()));

    return domains.stream()
        .collect(
            Collectors.toMap(
                domain -> domain.getId().uuidValue(),
                domain -> {
                  var association = supplier.apply(domain);
                  association.setSubType(source.getSubType(domain).orElse(null));
                  association.setStatus(source.getStatus(domain).orElse(null));
                  association.setDecisionResults(source.getDecisionResults(domain));
                  return association;
                }));
  }

  private <TAssociationDto extends DomainAssociationDto> void mapToEntity(
      Map<String, TAssociationDto> domains, Element target, IdRefResolver idRefResolver) {
    mapToEntity(domains, target, idRefResolver, (domain, associationDto) -> {});
  }

  private <TAssociationDto extends DomainAssociationDto> void mapToEntity(
      Map<String, TAssociationDto> domains,
      Element target,
      IdRefResolver idRefResolver,
      BiConsumer<DomainBase, TAssociationDto> customMapper) {
    domains
        .entrySet()
        .forEach(
            entry -> {
              var associationDto = entry.getValue();
              DomainBase domainTemplate =
                  idRefResolver.resolve(SyntheticIdRef.from(entry.getKey(), Domain.class));
              target.associateWithDomain(
                  domainTemplate, associationDto.getSubType(), associationDto.getStatus());
              customMapper.accept(domainTemplate, associationDto);
            });
  }

  private RiskDefinitionRef toRiskDefinitionRef(String riskDefId, DomainBase domain) {
    if (riskDefId == null) {
      return null;
    }
    return referencesForDomain(domain).getRiskDefinitionRef(riskDefId);
  }
}
