/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Finn Westendorf.
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

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.common.CompoundIdRef;
import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.AbstractRiskDto;
import org.veo.adapter.presenter.api.dto.RiskDomainAssociationDto;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Person;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.ScopeRisk;
import org.veo.core.entity.ref.ITypedId;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Singular;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Data
@NoArgsConstructor
public class ScopeRiskDto extends AbstractRiskDto {

  @Valid private IdRef<Scope> scope;

  @Builder
  public ScopeRiskDto(
      @Valid @Singular Set<IdRef<Domain>> domains,
      @Valid @NotNull(message = "A scenario must be present.") IdRef<Scenario> scenario,
      @Valid IdRef<Control> mitigatedBy,
      @Valid IdRef<Person> riskOwner,
      @Pattern(regexp = Patterns.DATETIME) String createdAt,
      String createdBy,
      @Pattern(regexp = Patterns.DATETIME) String updatedAt,
      String updatedBy,
      @Valid IdRef<Scope> scope,
      CompoundIdRef<ScopeRisk, Scope, Scenario> selfRef,
      long version,
      String designator,
      @Valid Map<String, RiskDomainAssociationDto> domainsWithRiskValues) {
    super(designator, domains, scenario, mitigatedBy, riskOwner, domainsWithRiskValues);
    this.scope = scope;
    setSelfRef(selfRef);
    setCreatedAt(createdAt);
    setCreatedBy(createdBy);
    setUpdatedAt(updatedAt);
    setUpdatedBy(updatedBy);
    setVersion(version);
    setDomainsWithRiskValues(domainsWithRiskValues);
  }

  public static ScopeRiskDto from(@Valid ScopeRisk risk, ReferenceAssembler referenceAssembler) {
    return ScopeRiskDto.builder()
        .designator(risk.getDesignator())
        .scope(IdRef.from(risk.getEntity(), referenceAssembler))
        .scenario(IdRef.from(risk.getScenario(), referenceAssembler))
        .riskOwner(IdRef.from(risk.getRiskOwner(), referenceAssembler))
        .mitigatedBy(IdRef.from(risk.getMitigation(), referenceAssembler))
        .createdAt(risk.getCreatedAt().toString())
        .createdBy(risk.getCreatedBy())
        .updatedAt(risk.getUpdatedAt().toString())
        .updatedBy(risk.getUpdatedBy())
        .version(risk.getVersion())
        .domains(
            risk.getDomains().stream()
                .map(o -> IdRef.from(o, referenceAssembler))
                .collect(Collectors.toSet()))
        .selfRef(CompoundIdRef.from(risk, referenceAssembler))
        .domainsWithRiskValues(toDomainRiskDefinitions(risk, referenceAssembler))
        .build();
  }

  @Override
  @JsonIgnore
  public ITypedId<Scope> getOwnerRef() {
    return scope;
  }

  @Override
  public Class<ScopeRisk> getModelInterface() {
    return ScopeRisk.class;
  }
}
