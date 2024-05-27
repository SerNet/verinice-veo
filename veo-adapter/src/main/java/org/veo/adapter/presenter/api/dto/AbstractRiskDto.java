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
package org.veo.adapter.presenter.api.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static java.util.stream.Collectors.toMap;
import static org.veo.adapter.presenter.api.dto.MapFunctions.renameKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.full.RiskValuesDto;
import org.veo.adapter.presenter.api.io.mapper.CategorizedRiskValueMapper;
import org.veo.adapter.presenter.api.openapi.IdRefDomains;
import org.veo.adapter.presenter.api.openapi.IdRefEntity;
import org.veo.adapter.presenter.api.openapi.IdRefOwner;
import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Person;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.risk.RiskValues;
import org.veo.core.entity.state.RiskState;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Valid
@SuppressWarnings("PMD.AbstractClassWithoutAnyMethod")
@Accessors(makeFinal = true)
public abstract class AbstractRiskDto extends AbstractVersionedSelfReferencingDto
    implements RiskState, ModelDto {

  @Schema(
      description = "Compact human-readable identifier that is unique within the client.",
      example = "A-155",
      requiredMode = REQUIRED,
      accessMode = Schema.AccessMode.READ_ONLY)
  @ToString.Include
  private String designator;

  @Valid
  @ArraySchema(schema = @Schema(implementation = IdRefDomains.class))
  @JsonIgnore
  @Singular
  private Set<IdRef<Domain>> domains = Collections.emptySet();

  @JsonGetter(value = "domains")
  @Schema(
      description =
          "Key is a domain-ID, values are the reference to the "
              + "domain and its available risk definitions")
  public Map<String, RiskDomainAssociationDto> getDomains() {
    return domainsWithRiskValues;
  }

  @JsonSetter(value = "domains")
  public void setDomains(Map<String, RiskDomainAssociationDto> domainMap) {
    this.domains =
        domainMap.values().stream()
            .map(RiskDomainAssociationDto::getReference)
            .collect(Collectors.toSet());
    this.domainsWithRiskValues = domainMap;
  }

  @JsonIgnore
  public Set<IdRef<Domain>> getDomainReferences() {
    return domains;
  }

  @Valid
  @NotNull(message = "A scenario must be present.")
  @Schema(requiredMode = REQUIRED, implementation = IdRefOwner.class)
  private IdRef<Scenario> scenario;

  @Valid
  @Schema(
      implementation = IdRefEntity.class,
      description = "This risk is mitigated by this control or control-composite.")
  private IdRef<Control> mitigation;

  @Valid
  @Schema(
      implementation = IdRefEntity.class,
      description = "The accountable point-of-contact for this risk.")
  private IdRef<Person> riskOwner;

  @Valid @JsonIgnore
  protected Map<String, RiskDomainAssociationDto> domainsWithRiskValues = Collections.emptyMap();

  public void transferToDomain(String sourceDomainId, String targetDomainId) {
    var domainAssociations = getDomains();
    renameKey(domainAssociations, sourceDomainId, targetDomainId);
    setDomains(domainAssociations);
  }

  protected static Set<IdRef<Domain>> toDomainReferences(
      AbstractRisk<?, ?> risk, ReferenceAssembler referenceAssembler) {
    return risk.getDomains().stream()
        .map(o -> IdRef.from(o, referenceAssembler))
        .collect(Collectors.toSet());
  }

  protected static Map<String, RiskDomainAssociationDto> toDomainRiskDefinitions(
      AbstractRisk<?, ?> risk, ReferenceAssembler referenceAssembler) {
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
      AbstractRisk<?, ?> risk, Domain domain) {
    return risk.getRiskDefinitions(domain).stream()
        .collect(toMap(RiskDefinitionRef::getIdRef, rd -> RiskValuesDto.from(risk, rd, domain)));
  }

  @Override
  @JsonIgnore
  public Set<ITypedId<Domain>> getDomainRefs() {
    return new HashSet<>(domains);
  }

  @Override
  @JsonIgnore
  public ITypedId<Scenario> getScenarioRef() {
    return scenario;
  }

  @Nullable
  @Override
  @JsonIgnore
  public ITypedId<Control> getMitigationRef() {
    return mitigation;
  }

  @Nullable
  @Override
  @JsonIgnore
  public ITypedId<Person> getRiskOwnerRef() {
    return riskOwner;
  }

  @Override
  @JsonIgnore
  public Set<RiskValues> getRiskValues() {
    return CategorizedRiskValueMapper.map(getDomainsWithRiskValues());
  }
}
