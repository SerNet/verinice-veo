/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.adapter.presenter.api.dto.full;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.common.RiskRef;
import org.veo.adapter.presenter.api.dto.AbstractRiskDto;
import org.veo.adapter.presenter.api.dto.RiskDomainAssociationDto;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.risk.RiskDefinitionRef;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Singular;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Data
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class ProcessRiskDto extends AbstractRiskDto {

  @Valid private IdRef<Process> process;

  @Valid @JsonIgnore
  private Map<String, RiskDomainAssociationDto> domainsWithRiskValues = Collections.emptyMap();

  @Override
  @JsonGetter(value = "domains")
  @Schema(
      description =
          "Key is a domain-ID, values are the reference to the "
              + "domain and its available risk definitions")
  public Map<String, RiskDomainAssociationDto> getDomains() {
    return domainsWithRiskValues;
  }

  @Override
  @JsonSetter(value = "domains")
  public void setDomains(Map<String, RiskDomainAssociationDto> domainMap) {
    super.setDomains(domainMap);
    this.domainsWithRiskValues = domainMap;
  }

  @Builder
  public ProcessRiskDto(
      @Valid @Singular Set<IdRef<Domain>> domains,
      @Valid @NotNull(message = "A scenario must be present.") IdRef<Scenario> scenario,
      @Valid IdRef<Control> mitigatedBy,
      @Valid IdRef<Person> riskOwner,
      @Pattern(regexp = Patterns.DATETIME) String createdAt,
      String createdBy,
      @Pattern(regexp = Patterns.DATETIME) String updatedAt,
      String updatedBy,
      @Valid IdRef<Process> process,
      RiskRef selfRef,
      long version,
      String designator,
      @Valid Map<String, RiskDomainAssociationDto> domainsWithRiskValues) {
    super(designator, domains, scenario, mitigatedBy, riskOwner);
    this.process = process;
    setSelfRef(selfRef);
    setCreatedAt(createdAt);
    setCreatedBy(createdBy);
    setUpdatedAt(updatedAt);
    setUpdatedBy(updatedBy);
    setVersion(version);
    setDomainsWithRiskValues(domainsWithRiskValues);
  }

  public static ProcessRiskDto from(
      @Valid ProcessRisk risk, ReferenceAssembler referenceAssembler) {
    return ProcessRiskDto.builder()
        .designator(risk.getDesignator())
        .process(IdRef.from(risk.getEntity(), referenceAssembler))
        .scenario(IdRef.from(risk.getScenario(), referenceAssembler))
        .riskOwner(IdRef.from(risk.getRiskOwner(), referenceAssembler))
        .mitigatedBy(IdRef.from(risk.getMitigation(), referenceAssembler))
        .createdAt(risk.getCreatedAt().toString())
        .createdBy(risk.getCreatedBy())
        .updatedAt(risk.getUpdatedAt().toString())
        .updatedBy(risk.getUpdatedBy())
        .version(risk.getVersion())
        .domains(toDomainReferences(risk, referenceAssembler))
        .selfRef(new RiskRef(referenceAssembler, risk))
        .domainsWithRiskValues(toDomainRiskDefinitions(risk, referenceAssembler))
        .build();
  }

  private static Set<IdRef<Domain>> toDomainReferences(
      ProcessRisk risk, ReferenceAssembler referenceAssembler) {
    return risk.getDomains().stream()
        .map(o -> IdRef.from(o, referenceAssembler))
        .collect(Collectors.toSet());
  }

  private static Map<String, RiskDomainAssociationDto> toDomainRiskDefinitions(
      ProcessRisk risk, ReferenceAssembler referenceAssembler) {
    HashMap<String, RiskDomainAssociationDto> result = new HashMap<>();
    risk.getDomains()
        .forEach(
            d ->
                result.put(
                    d.getIdAsString(),
                    new RiskDomainAssociationDto(
                        IdRef.from(d, referenceAssembler),
                        valuesGroupedByRiskDefinition(risk, d))));
    return result;
  }

  private static Map<String, RiskValuesDto> valuesGroupedByRiskDefinition(
      ProcessRisk risk, Domain domain) {
    return risk.getRiskDefinitions(domain).stream()
        .collect(toMap(RiskDefinitionRef::getIdRef, rd -> RiskValuesDto.from(risk, rd, domain)));
  }
}
