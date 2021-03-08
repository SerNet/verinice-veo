/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
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
package org.veo.adapter.presenter.api.dto.full;

import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.AbstractRiskDto;
import org.veo.core.entity.Asset;
import org.veo.core.entity.AssetRisk;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Person;
import org.veo.core.entity.Scenario;

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
public class AssetRiskDto extends AbstractRiskDto {

    @Schema(description = "A valid reference to this resource.")
    private String _self = "";

    @Valid
    private ModelObjectReference<Asset> asset;

    @Builder
    public AssetRiskDto(@Valid @Singular Set<ModelObjectReference<Domain>> domains,
            @Valid @NotNull(message = "A scenario must be present.") ModelObjectReference<Scenario> scenario,
            @Valid ModelObjectReference<Control> mitigatedBy,
            @Valid ModelObjectReference<Person> riskOwner,
            @Pattern(regexp = Patterns.DATETIME) String createdAt, String createdBy,
            @Pattern(regexp = Patterns.DATETIME) String updatedAt, String updatedBy,
            @Valid ModelObjectReference<Asset> asset, String self, long version) {
        super(domains, scenario, mitigatedBy, riskOwner, createdAt, createdBy, updatedAt, updatedBy,
                version);
        this.asset = asset;
        this._self = self;
    }

    public static AssetRiskDto from(@Valid AssetRisk risk, ReferenceAssembler referenceAssembler) {
        return AssetRiskDto.builder()
                           .asset(ModelObjectReference.from(risk.getAsset(), referenceAssembler))
                           .scenario(ModelObjectReference.from(risk.getScenario(),
                                                               referenceAssembler))
                           .riskOwner(ModelObjectReference.from(risk.getRiskOwner(),
                                                                referenceAssembler))
                           .mitigatedBy(ModelObjectReference.from(risk.getMitigation(),
                                                                  referenceAssembler))
                           .createdAt(risk.getCreatedAt()
                                          .toString())
                           .createdBy(risk.getCreatedBy())
                           .updatedAt(risk.getUpdatedAt()
                                          .toString())
                           .updatedBy(risk.getUpdatedBy())
                           .version(risk.getVersion())
                           .domains(risk.getDomains()
                                        .stream()
                                        .map(o -> ModelObjectReference.from(o, referenceAssembler))
                                        .collect(Collectors.toSet()))
                           .self(referenceAssembler.targetReferenceOf(AssetRisk.class,
                                                                      risk.getAsset()
                                                                          .getId()
                                                                          .uuidValue(),
                                                                      risk.getScenario()
                                                                          .getId()
                                                                          .uuidValue()))
                           .build();
    }
}
