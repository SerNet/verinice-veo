/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2026  Jonas Jordan
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
package org.veo.core.entity.decision;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.TranslatedText;
import org.veo.core.entity.aspects.ElementDomainAssociation;
import org.veo.core.entity.decision.firsthitpolicy.FirstHitPolicyDecision;
import org.veo.core.entity.event.ElementEvent;

import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type",
    defaultImpl = FirstHitPolicyDecision.class)
@JsonSubTypes({
  @JsonSubTypes.Type(name = "expressive", value = ExpressiveDecision.class),
  @JsonSubTypes.Type(name = "firstHitPolicy", value = FirstHitPolicyDecision.class),
})
@Schema(
    discriminatorMapping = {
      @DiscriminatorMapping(value = "expressive", schema = ExpressiveDecision.class),
      @DiscriminatorMapping(value = "firstHitPolicy", schema = FirstHitPolicyDecision.class)
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public abstract class Decision {
  @NotNull private TranslatedText name;

  @NotNull private ElementType elementType;

  @NotNull
  @Size(max = ElementDomainAssociation.SUB_TYPE_MAX_LENGTH)
  private String elementSubType;

  public boolean isApplicableToElement(Element element, Domain domain) {
    return getElementType() == element.getType()
        && getElementSubType().equals(element.findSubType(domain).orElse(null));
  }

  public abstract DecisionResult evaluate(Element element, Domain domain);

  public abstract boolean isAffectedByEvent(ElementEvent event, Domain domain);

  public abstract void selfValidate(DomainBase domain);

  @JsonIgnore
  public abstract Class<?> getResultType(DomainBase domain);
}
