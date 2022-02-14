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
import org.veo.adapter.service.domaintemplate.SyntheticIdRef;
import org.veo.core.entity.Asset;
import org.veo.core.entity.Control;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.risk.ControlRiskValues;
import org.veo.core.entity.risk.DomainRiskReferenceProvider;
import org.veo.core.entity.risk.RiskDefinitionRef;

/**
 * Maps {@link Domain} associations of {@link Element}s between entities and
 * DTOs. See {@link DomainAssociationDto}.
 */
public class DomainAssociationTransformer {

    public void mapDomainsToEntity(AbstractAssetDto source, Asset target,
            IdRefResolver idRefResolver) {
        mapToEntity(source.getDomains(), target, idRefResolver);
    }

    public void mapDomainsToEntity(AbstractControlDto source, Control target,
            IdRefResolver idRefResolver) {
        mapToEntity(source.getDomains(), target, idRefResolver, (domain, associationDto) -> {
            target.setRiskValues(domain, associationDto.getRiskValues()
                                                       .entrySet()
                                                       .stream()
                                                       .collect(groupRiskDefinitionsByDomain(domain)));
        });
    }

    private Collector<Map.Entry<String, ControlRiskValuesDto>, ?, Map<RiskDefinitionRef, ControlRiskValues>> groupRiskDefinitionsByDomain(
            DomainTemplate domain) {
        var referenceProvider = referencesForDomain(domain);
        return Collectors.toMap(kv -> referenceProvider.getRiskDefinitionRef(kv.getKey())
                                                       .orElseThrow(() -> new NotFoundException(
                                                               "Risk definition '%s' was not found for domain '%s'",
                                                               kv.getKey(), domain.getId())),
                                kv -> mapControlRiskValuesDto2Entity(kv.getKey(), kv.getValue(),
                                                                     referenceProvider));
    }

    private ControlRiskValues mapControlRiskValuesDto2Entity(String riskDefinitionId,
            ControlRiskValuesDto riskValuesDto, DomainRiskReferenceProvider referenceProvider) {
        var riskValues = new ControlRiskValues();
        var implementationStatus = referenceProvider.getImplementationStatus(riskDefinitionId,
                                                                             riskValuesDto.getImplementationStatus())
                                                    .orElseThrow(() -> new IllegalArgumentException(
                                                            String.format("Risk definition %s contains no implementation status with ordinal value %d",
                                                                          riskDefinitionId,
                                                                          riskValuesDto.getImplementationStatus())));
        riskValues.setImplementationStatus(implementationStatus);
        return riskValues;
    }

    public void mapDomainsToEntity(AbstractDocumentDto source, Document target,
            IdRefResolver idRefResolver) {
        mapToEntity(source.getDomains(), target, idRefResolver);
    }

    public void mapDomainsToEntity(AbstractIncidentDto source, Incident target,
            IdRefResolver idRefResolver) {
        mapToEntity(source.getDomains(), target, idRefResolver);
    }

    public void mapDomainsToEntity(AbstractPersonDto source, Person target,
            IdRefResolver idRefResolver) {
        mapToEntity(source.getDomains(), target, idRefResolver);
    }

    public void mapDomainsToEntity(AbstractProcessDto source, Process target,
            IdRefResolver idRefResolver) {
        mapToEntity(source.getDomains(), target, idRefResolver);
    }

    public void mapDomainsToEntity(AbstractScenarioDto source, Scenario target,
            IdRefResolver idRefResolver) {
        mapToEntity(source.getDomains(), target, idRefResolver);
    }

    public void mapDomainsToEntity(AbstractScopeDto source, Scope target,
            IdRefResolver idRefResolver) {
        mapToEntity(source.getDomains(), target, idRefResolver);
    }

    public void mapDomainsToDto(Asset source, AbstractAssetDto target) {
        target.setDomains(extractDomainAssociations(source, DomainAssociationDto::new));
    }

    public void mapDomainsToDto(Control source, AbstractControlDto target) {
        target.setDomains(extractDomainAssociations(source, (domain) -> {
            var assocationDto = new ControlDomainAssociationDto();
            source.getRiskValues(domain)
                  .ifPresent(riskValues -> {
                      assocationDto.setRiskValues(riskValues.entrySet()
                                                            .stream()
                                                            .collect(Collectors.toMap(kv -> kv.getKey()
                                                                                              .getIdRef(),
                                                                                      this::mapControlRiskValuesToDto)));
                  });
            return assocationDto;
        }));
    }

    private ControlRiskValuesDto mapControlRiskValuesToDto(
            Map.Entry<RiskDefinitionRef, ControlRiskValues> entry) {
        var riskValuesDto = new ControlRiskValuesDto();
        riskValuesDto.setImplementationStatus(entry.getValue()
                                                   .getImplementationStatus()
                                                   .getOrdinalValue());
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
        target.setDomains(extractDomainAssociations(source, DomainAssociationDto::new));
    }

    public void mapDomainsToDto(Scenario source, AbstractScenarioDto target) {
        target.setDomains(extractDomainAssociations(source, DomainAssociationDto::new));
    }

    public void mapDomainsToDto(Scope source, AbstractScopeDto target) {
        target.setDomains(extractDomainAssociations(source, DomainAssociationDto::new));
    }

    private <T extends DomainAssociationDto> Map<String, T> extractDomainAssociations(
            Element source, Supplier<T> supplier) {
        return extractDomainAssociations(source, domain -> supplier.get());
    }

    private <T extends DomainAssociationDto> Map<String, T> extractDomainAssociations(
            Element source, Function<DomainTemplate, T> supplier) {
        // Catalog item elements in a domain template may have aspects for domains that
        // the element is not assigned to. Seems invalid, but that's the situation we
        // have to deal with here.
        var domains = new HashSet<DomainTemplate>();
        domains.addAll(source.getDomains());
        domains.addAll(source.getSubTypeAspects()
                             .stream()
                             .map(a -> a.getDomain())
                             .collect(Collectors.toSet()));

        return domains.stream()
                      .collect(Collectors.toMap(domain -> domain.getId()
                                                                .uuidValue(),
                                                domain -> {
                                                    var association = supplier.apply(domain);
                                                    association.setSubType(source.getSubType(domain)
                                                                                 .orElse(null));
                                                    association.setStatus(source.getStatus(domain)
                                                                                .orElse(null));
                                                    return association;
                                                }));
    }

    private <TAssociationDto extends DomainAssociationDto> void mapToEntity(
            Map<String, TAssociationDto> domains, Element target, IdRefResolver idRefResolver) {
        mapToEntity(domains, target, idRefResolver, (domain, associationDto) -> {
        });
    }

    private <TAssociationDto extends DomainAssociationDto> void mapToEntity(
            Map<String, TAssociationDto> domains, Element target, IdRefResolver idRefResolver,
            BiConsumer<DomainTemplate, TAssociationDto> customMapper) {
        domains.entrySet()
               .forEach(entry -> {
                   var associationDto = entry.getValue();
                   DomainTemplate domainTemplate = idRefResolver.resolve(SyntheticIdRef.from(entry.getKey(),
                                                                                             Domain.class));
                   if (domainTemplate instanceof Domain) {
                       var domain = (Domain) domainTemplate;
                       target.addToDomains(domain);
                       domain.validateSubType(target.getModelInterface(),
                                              associationDto.getSubType());
                   }
                   target.setSubType(domainTemplate, associationDto.getSubType(),
                                     associationDto.getStatus());
                   customMapper.accept(domainTemplate, associationDto);
               });
    }
}
