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

import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.common.SymIdRef;
import org.veo.adapter.presenter.api.dto.AssetDomainAssociationDto;
import org.veo.adapter.presenter.api.dto.CustomAspectMapDto;
import org.veo.adapter.presenter.api.dto.DomainAssociationDto;
import org.veo.adapter.presenter.api.dto.ImpactValuesDto;
import org.veo.adapter.presenter.api.dto.LinkMapDto;
import org.veo.adapter.presenter.api.dto.ProcessDomainAssociationDto;
import org.veo.adapter.presenter.api.dto.ScenarioDomainAssociationDto;
import org.veo.adapter.presenter.api.dto.ScenarioRiskValuesDto;
import org.veo.adapter.presenter.api.dto.ScopeDomainAssociationDto;
import org.veo.adapter.presenter.api.dto.full.FullAssetDto;
import org.veo.adapter.presenter.api.dto.full.FullControlDto;
import org.veo.adapter.presenter.api.dto.full.FullDocumentDto;
import org.veo.adapter.presenter.api.dto.full.FullIncidentDto;
import org.veo.adapter.presenter.api.dto.full.FullPersonDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessDto;
import org.veo.adapter.presenter.api.dto.full.FullScenarioDto;
import org.veo.adapter.presenter.api.dto.full.FullScopeDto;
import org.veo.core.entity.Asset;
import org.veo.core.entity.Control;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.risk.ImpactValues;
import org.veo.core.entity.risk.PotentialProbability;
import org.veo.core.entity.risk.ProbabilityRef;
import org.veo.core.entity.risk.RiskDefinitionRef;

import lombok.RequiredArgsConstructor;

/**
 * Maps {@link Domain} associations of {@link Element}s between entities and DTOs. See {@link
 * DomainAssociationDto}.
 */
@RequiredArgsConstructor
public class DomainAssociationTransformer {
  private final ReferenceAssembler referenceAssembler;

  public void mapDomainsToDto(Asset source, FullAssetDto target, boolean newStructure) {
    target.setDomains(
        extractDomainAssociations(
            source,
            domain -> {
              var associationDto = new AssetDomainAssociationDto();
              associationDto.setRiskValues(mapRiskValues(source, domain));
              return associationDto;
            },
            newStructure));
  }

  public void mapDomainsToDto(Control source, FullControlDto target, boolean newStructure) {
    target.setDomains(extractDomainAssociations(source, DomainAssociationDto::new, newStructure));
  }

  private ImpactValuesDto mapImpactValuesToDto(Map.Entry<RiskDefinitionRef, ImpactValues> entry) {
    var dto = new ImpactValuesDto();
    var impactValue = entry.getValue();

    dto.setPotentialImpacts(categoryRefToString(impactValue.potentialImpacts()));
    dto.setPotentialImpactsCalculated(
        categoryRefToString(impactValue.potentialImpactsCalculated()));
    dto.setPotentialImpactsEffective(
        categoryRefToString(impactValue.getPotentialImpactsEffective()));
    dto.setPotentialImpactReasons(categoryRefToString(impactValue.potentialImpactReasons()));
    dto.setPotentialImpactExplanations(
        categoryRefToString(impactValue.potentialImpactExplanations()));
    dto.setPotentialImpactEffectiveReasons(
        categoryRefToString(impactValue.getPotentialImpactEffectiveReasons()));

    return dto;
  }

  private <T> Map<String, T> categoryRefToString(Map<CategoryRef, T> inputMap) {
    return inputMap.entrySet().stream().collect(toMap(e -> e.getKey().getIdRef(), Entry::getValue));
  }

  public void mapDomainsToDto(Document source, FullDocumentDto target, boolean newStructure) {
    target.setDomains(extractDomainAssociations(source, DomainAssociationDto::new, newStructure));
  }

  public void mapDomainsToDto(Incident source, FullIncidentDto target, boolean newStructure) {
    target.setDomains(extractDomainAssociations(source, DomainAssociationDto::new, newStructure));
  }

  public void mapDomainsToDto(Person source, FullPersonDto target, boolean newStructure) {
    target.setDomains(extractDomainAssociations(source, DomainAssociationDto::new, newStructure));
  }

  public void mapDomainsToDto(Process source, FullProcessDto target, boolean newStructure) {
    target.setDomains(
        extractDomainAssociations(
            source,
            domain -> {
              var associationDto = new ProcessDomainAssociationDto();
              associationDto.setRiskValues(mapRiskValues(source, domain));
              return associationDto;
            },
            newStructure));
  }

  public Map<String, ImpactValuesDto> mapRiskValues(Process source, Domain domain) {
    return source.getImpactValues(domain).entrySet().stream()
        .collect(toMap(kv -> kv.getKey().getIdRef(), this::mapImpactValuesToDto));
  }

  public Map<String, ImpactValuesDto> mapRiskValues(Scope source, Domain domain) {
    return source.getImpactValues(domain).entrySet().stream()
        .collect(toMap(kv -> kv.getKey().getIdRef(), this::mapImpactValuesToDto));
  }

  public Map<String, ImpactValuesDto> mapRiskValues(Asset source, Domain domain) {
    return source.getImpactValues(domain).entrySet().stream()
        .collect(toMap(kv -> kv.getKey().getIdRef(), this::mapImpactValuesToDto));
  }

  public void mapDomainsToDto(Scenario source, FullScenarioDto target, boolean newStructure) {
    target.setDomains(
        extractDomainAssociations(
            source,
            domain -> {
              var associationDto = new ScenarioDomainAssociationDto();
              associationDto.setRiskValues(mapRiskValues(source, domain));
              return associationDto;
            },
            newStructure));
  }

  public Map<String, ScenarioRiskValuesDto> mapRiskValues(Scenario source, Domain domain) {
    return source.getPotentialProbability(domain).entrySet().stream()
        .collect(toMap(kv -> kv.getKey().getIdRef(), this::mapScenarioRiskValuesToDto));
  }

  private ScenarioRiskValuesDto mapScenarioRiskValuesToDto(
      Map.Entry<RiskDefinitionRef, PotentialProbability> entry) {
    var riskValuesDto = new ScenarioRiskValuesDto();
    riskValuesDto.setPotentialProbability(
        Optional.ofNullable(entry.getValue().potentialProbability())
            .map(ProbabilityRef::getIdRef)
            .orElse(null));
    riskValuesDto.setPotentialProbabilityExplanation(
        entry.getValue().potentialProbabilityExplanation());
    return riskValuesDto;
  }

  public void mapDomainsToDto(Scope source, FullScopeDto target, boolean newStructure) {
    target.setDomains(
        extractDomainAssociations(
            source,
            domain -> {
              var associationDto = new ScopeDomainAssociationDto();
              associationDto.setRiskDefinition(mapRiskDefinition(source, domain));
              associationDto.setRiskValues(mapRiskValues(source, domain));
              return associationDto;
            },
            newStructure));
  }

  public String mapRiskDefinition(Scope source, Domain domain) {
    return source.getRiskDefinition(domain).map(RiskDefinitionRef::getIdRef).orElse(null);
  }

  private <T extends DomainAssociationDto> Map<UUID, T> extractDomainAssociations(
      Element source, Supplier<T> supplier, boolean newStructure) {
    return extractDomainAssociations(source, domain -> supplier.get(), newStructure);
  }

  private <T extends DomainAssociationDto> Map<UUID, T> extractDomainAssociations(
      Element source, Function<Domain, T> supplier, boolean newStructure) {
    return source.getDomains().stream()
        .collect(
            toMap(
                Identifiable::getId,
                domain -> {
                  var association = supplier.apply(domain);
                  association.setSubType(source.getSubType(domain));
                  association.setStatus(source.getStatus(domain));
                  association.setDecisionResults(source.getDecisionResults(domain));
                  if (newStructure) {
                    association.setCustomAspects(CustomAspectMapDto.from(source, domain));
                    association.setLinks(LinkMapDto.from(source, domain, referenceAssembler));
                  }
                  association.setAppliedCatalogItem(
                      source
                          .findAppliedCatalogItem(domain)
                          .map(ci -> SymIdRef.from(ci, referenceAssembler))
                          .orElse(null));
                  return association;
                }));
  }
}
