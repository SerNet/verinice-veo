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
package org.veo.core.entity;

import static java.util.function.Function.identity;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This class contains translations from/to all known resource collections to their names & types.
 *
 * <p>This should be maintained to be the only place where new resource types must be added.
 */
@AllArgsConstructor
public enum EntityType {
  ASSET(Asset.class, Asset.SINGULAR_TERM, Asset.PLURAL_TERM),
  ASSET_RISK(AssetRisk.class, AssetRisk.SINGULAR_TERM, AssetRisk.PLURAL_TERM),
  CLIENT(Client.class, Client.SINGULAR_TERM, Client.PLURAL_TERM),
  CONTROL(Control.class, Control.SINGULAR_TERM, Control.PLURAL_TERM),
  CATALOGITEM(CatalogItem.class, CatalogItem.SINGULAR_TERM, CatalogItem.PLURAL_TERM),
  DOCUMENT(Document.class, Document.SINGULAR_TERM, Document.PLURAL_TERM),
  DOMAIN(Domain.class, Domain.SINGULAR_TERM, Domain.PLURAL_TERM),
  DOMAINTEMPLATE(DomainTemplate.class, DomainTemplate.SINGULAR_TERM, DomainTemplate.PLURAL_TERM),
  INCIDENT(Incident.class, Incident.SINGULAR_TERM, Incident.PLURAL_TERM),
  PERSON(Person.class, Person.SINGULAR_TERM, Person.PLURAL_TERM),
  PROCESS(Process.class, Process.SINGULAR_TERM, Process.PLURAL_TERM),
  PROCESS_RISK(ProcessRisk.class, ProcessRisk.SINGULAR_TERM, ProcessRisk.PLURAL_TERM),
  PROFILE(Profile.class, Profile.SINGULAR_TERM, Profile.PLURAL_TERM),
  PROFILE_ITEM(ProfileItem.class, ProfileItem.SINGULAR_TERM, ProfileItem.PLURAL_TERM),
  UNIT(Unit.class, Unit.SINGULAR_TERM, Unit.PLURAL_TERM),
  SCENARIO(Scenario.class, Scenario.SINGULAR_TERM, Scenario.PLURAL_TERM),
  SCOPE(Scope.class, Scope.SINGULAR_TERM, Scope.PLURAL_TERM),
  SCOPE_RISK(ScopeRisk.class, ScopeRisk.SINGULAR_TERM, ScopeRisk.PLURAL_TERM),
  USER_CONFIGURATION(
      UserConfiguration.class, UserConfiguration.SINGULAR_TERM, UserConfiguration.PLURAL_TERM);

  @Getter private final Class<? extends Entity> type;
  @Getter private final String singularTerm;
  @Getter private final String pluralTerm;

  public static final Set<String> PLURAL_TERMS =
      Stream.of(values()).map(et -> et.pluralTerm).collect(Collectors.toUnmodifiableSet());

  public static final Set<Class<? extends Entity>> TYPES =
      Stream.of(values()).map(et -> et.type).collect(Collectors.toUnmodifiableSet());

  public static final Set<EntityType> ELEMENT_TYPES =
      Stream.of(values())
          .filter(type -> Element.class.isAssignableFrom(type.type))
          .collect(Collectors.toUnmodifiableSet());

  public static final Set<EntityType> RISK_AFFECTED_TYPES =
      Stream.of(values())
          .filter(type -> RiskAffected.class.isAssignableFrom(type.type))
          .collect(Collectors.toUnmodifiableSet());

  public static final Set<String> ELEMENT_SINGULAR_TERMS =
      ELEMENT_TYPES.stream().map(et -> et.singularTerm).collect(Collectors.toUnmodifiableSet());

  public static final Set<String> ELEMENT_PLURAL_TERMS =
      ELEMENT_TYPES.stream().map(et -> et.pluralTerm).collect(Collectors.toUnmodifiableSet());

  public static final Set<Class<? extends Element>> ELEMENT_TYPE_CLASSES =
      ELEMENT_TYPES.stream()
          .map(t -> (Class<? extends Element>) t.type)
          .collect(Collectors.toUnmodifiableSet());

  public static final Set<String> TYPE_DESIGNATORS =
      Set.of(
          AbstractRisk.TYPE_DESIGNATOR,
          Asset.TYPE_DESIGNATOR,
          Control.TYPE_DESIGNATOR,
          Document.TYPE_DESIGNATOR,
          Incident.TYPE_DESIGNATOR,
          Person.TYPE_DESIGNATOR,
          Process.TYPE_DESIGNATOR,
          Scenario.TYPE_DESIGNATOR,
          Scope.TYPE_DESIGNATOR);

  public static final Set<EntityType> RISK_RELETATED_ELEMENTS =
      ELEMENT_TYPES.stream()
          .filter(type -> RiskRelated.class.isAssignableFrom(type.type))
          .collect(Collectors.toUnmodifiableSet());

  private static final Map<String, Class<? extends Entity>> TYPE_BY_PLURAL_TERM =
      Stream.of(values()).collect(Collectors.toMap(EntityType::getPluralTerm, EntityType::getType));

  private static final Map<String, EntityType> ENTITY_TYPE_BY_SINGULAR_TERM =
      Stream.of(values()).collect(Collectors.toMap(EntityType::getSingularTerm, identity()));

  private static final Map<Class<? extends Entity>, String> SINGULAR_TERM_BY_TYPE =
      Stream.of(values())
          .collect(Collectors.toMap(EntityType::getType, EntityType::getSingularTerm));

  private static final Map<Class<? extends Entity>, String> PLURAL_TERM_BY_TYPE =
      Stream.of(values()).collect(Collectors.toMap(EntityType::getType, EntityType::getPluralTerm));

  public static Class<? extends Entity> getTypeForPluralTerm(String pluralTerm) {
    return Optional.ofNullable(TYPE_BY_PLURAL_TERM.get(pluralTerm))
        .orElseThrow(
            () -> new IllegalArgumentException("Unknown entity type '%s'".formatted(pluralTerm)));
  }

  public static EntityType getBySingularTerm(String singularTerm) {
    return Optional.ofNullable(ENTITY_TYPE_BY_SINGULAR_TERM.get(singularTerm)).orElseThrow();
  }

  public static String getSingularTermByType(Class<? extends Entity> type) {
    return Optional.ofNullable(SINGULAR_TERM_BY_TYPE.get(type)).orElseThrow();
  }

  public static String getPluralTermByType(Class<? extends Entity> type) {
    return Optional.ofNullable(PLURAL_TERM_BY_TYPE.get(type)).orElseThrow();
  }

  public static void validateElementType(String elementType) {
    if (!ELEMENT_SINGULAR_TERMS.contains(elementType)) {
      throw new IllegalArgumentException(
          "'%s' is not a valid element type - must be one of %s"
              .formatted(
                  elementType,
                  ELEMENT_SINGULAR_TERMS.stream().sorted().collect(Collectors.joining(", "))));
    }
  }
}
