/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jochen Kemnade
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
package org.veo.core.usecase.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.veo.core.entity.BreakingChange;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Key;
import org.veo.core.entity.definitions.CustomAspectDefinition;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.definitions.attribute.AttributeDefinition;
import org.veo.core.entity.definitions.attribute.EnumAttributeDefinition;
import org.veo.core.entity.definitions.attribute.ListAttributeDefinition;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.DomainRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetBreakingChangesUseCase
    implements TransactionalUseCase<
        GetBreakingChangesUseCase.InputData, GetBreakingChangesUseCase.OutputData> {

  private final DomainRepository repository;

  @Override
  public OutputData execute(InputData input) {
    var domain = repository.getActiveById(input.domainId, input.authenticatedClientId);
    var template = domain.getDomainTemplate();
    if (template == null) {
      throw new NotFoundException("No domain template found for domain %s", input.domainId);
    }
    return new OutputData(determineBreakingChanges(domain, template));
  }

  private List<BreakingChange> determineBreakingChanges(Domain domain, DomainTemplate template) {
    List<BreakingChange> result = new ArrayList<>();
    for (ElementTypeDefinition etdTemplate : template.getElementTypeDefinitions()) {
      String elementType = etdTemplate.getElementType();
      ElementTypeDefinition etdDomain = domain.getElementTypeDefinition(elementType);
      Map<String, CustomAspectDefinition> casDomain = etdDomain.getCustomAspects();
      for (Entry<String, CustomAspectDefinition> e : etdTemplate.getCustomAspects().entrySet()) {
        String customAspectId = e.getKey();
        CustomAspectDefinition caTemplate = e.getValue();
        CustomAspectDefinition caDomain = casDomain.get(e.getKey());

        if (caDomain == null) {
          // CA was removed
          caTemplate
              .getAttributeDefinitions()
              .forEach(
                  (id, def) -> {
                    result.add(BreakingChange.removal(elementType, customAspectId, id, def));
                  });
        } else {
          Map<String, AttributeDefinition> defsDomain = caDomain.getAttributeDefinitions();
          caTemplate
              .getAttributeDefinitions()
              .forEach(
                  (id, defTemplate) -> {
                    AttributeDefinition defDomain = defsDomain.get(id);
                    if (defDomain == null) {
                      // attribute was removed
                      result.add(
                          BreakingChange.removal(elementType, customAspectId, id, defTemplate));
                    } else {
                      if (!isCompatibleChange(defTemplate, defDomain)) {
                        // definition was changed in an incompatible way
                        result.add(
                            BreakingChange.modification(
                                elementType, customAspectId, id, defTemplate, defDomain));
                      }
                    }
                  });
        }
      }
      // TODO: #3277 also check link definition compatibility
    }

    return result;
  }

  private boolean isCompatibleChange(
      AttributeDefinition defTemplate, AttributeDefinition defDomain) {
    if (defDomain.equals(defTemplate)) {
      return true;
    }
    if (defTemplate instanceof EnumAttributeDefinition eadTemplate
        && defDomain instanceof EnumAttributeDefinition eadDomain) {
      return eadDomain.getAllowedValues().containsAll(eadTemplate.getAllowedValues());
    }
    if (defTemplate instanceof ListAttributeDefinition ladTemplate
        && defDomain instanceof ListAttributeDefinition ladDomain) {
      return isCompatibleChange(ladTemplate.getItemDefinition(), ladDomain.getItemDefinition());
    }
    return false;
  }

  public record InputData(Key<UUID> authenticatedClientId, Key<UUID> domainId)
      implements UseCase.InputData {}

  public record OutputData(List<BreakingChange> breakingChanges) implements UseCase.OutputData {}
}
