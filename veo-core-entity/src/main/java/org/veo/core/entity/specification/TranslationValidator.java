/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Alexander Koderman
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
package org.veo.core.entity.specification;

import static java.util.function.Predicate.not;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.veo.core.entity.Nameable;
import org.veo.core.entity.TranslationException;
import org.veo.core.entity.definitions.CustomAspectDefinition;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.definitions.LinkDefinition;
import org.veo.core.entity.definitions.SubTypeDefinition;
import org.veo.core.entity.definitions.attribute.AttributeDefinition;
import org.veo.core.entity.riskdefinition.RiskDefinition;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TranslationValidator {
  /**
   * The naming convention used for the translation of subtype statuses is: {@code
   * <TYPE>_<SUBTYPE>_status_<STATUS>}
   */
  public static final String SUBTYPE_STATUS_PATTERN = "%s_%s_status_%s";

  public static final Set<String> NAMEABLE_ATTRIBUTES =
      Set.of(Nameable.NAME, Nameable.ABBREVIATION, Nameable.DESCRIPTION);

  public record Violation(String language, Reason reason, String key) {}

  public enum Reason {
    MISSING,
    SUPERFLUOUS
  }

  private static void checkLanguage(String lang) {
    try {
      new Locale.Builder().setLanguage(lang).build();
    } catch (IllformedLocaleException e) {
      throw new TranslationException(
          "Unknown language %s for translation in object schema.".formatted(lang));
    }
  }

  public static void validate(ElementTypeDefinition definition) {
    validate(
        definition.getTranslations(),
        definition.getElementType(),
        definition.getCustomAspects(),
        definition.getLinks(),
        definition.getSubTypes());
  }

  /**
   * Compare the translations against the provided parameters. Will detect missing and
   * superfluous/mistyped translation keys.
   *
   * @param translations
   * @param type
   * @param customAspects
   * @param links
   * @param subTypes
   */
  public static void validate(
      Map<String, Map<String, String>> translations,
      String type,
      Map<String, CustomAspectDefinition> customAspects,
      Map<String, LinkDefinition> links,
      Map<String, SubTypeDefinition> subTypes) {

    translations.keySet().forEach(TranslationValidator::checkLanguage);

    List<String> allEntityKeys = new ArrayList<>();
    allEntityKeys.addAll(attributeTranslationKeys(customAspects));
    allEntityKeys.addAll(attributeTranslationKeys(links));
    allEntityKeys.addAll(linkIdTranslationKeys(links));
    allEntityKeys.addAll(statusTranslationKeys(type, subTypes));

    var violations =
        translations.keySet().stream()
            .flatMap(l -> validateLanguage(l, translations.get(l), allEntityKeys))
            .toList();

    if (violations.size() > 0) {
      throw new TranslationException(violations);
    }
  }

  public static void validate(RiskDefinition riskDefinition) {
    riskDefinition
        .getRiskMethod()
        .getTranslations()
        .keySet()
        .forEach(TranslationValidator::checkLanguage);
    riskDefinition
        .getProbability()
        .getTranslations()
        .keySet()
        .forEach(TranslationValidator::checkLanguage);
    riskDefinition
        .getCategories()
        .forEach(c -> c.getTranslations().keySet().forEach(TranslationValidator::checkLanguage));
    riskDefinition
        .getRiskValues()
        .forEach(c -> c.getTranslations().keySet().forEach(TranslationValidator::checkLanguage));

    List<Violation> violations = new ArrayList<>();

    String validationContext = "riskDefinition " + riskDefinition.getId();
    violations.addAll(
        riskDefinition.getRiskMethod().getTranslations().entrySet().stream()
            .map(
                t -> {
                  return validateAttributeTranslations(
                      t.getKey(),
                      t.getValue(),
                      Set.of("impactMethod", Nameable.DESCRIPTION),
                      validationContext + " risk method: ");
                })
            .flatMap(l -> l.stream())
            .collect(Collectors.toList()));

    violations.addAll(
        riskDefinition.getProbability().getTranslations().entrySet().stream()
            .map(
                t ->
                    validateNameableTranslations(
                        t.getKey(), t.getValue(), validationContext + " probability: "))
            .flatMap(l -> l.stream())
            .collect(Collectors.toList()));

    violations.addAll(
        riskDefinition.getImplementationStateDefinition().getTranslations().entrySet().stream()
            .map(
                t ->
                    validateNameableTranslations(
                        t.getKey(), t.getValue(), validationContext + " implementation state: "))
            .flatMap(l -> l.stream())
            .collect(Collectors.toList()));

    violations.addAll(
        riskDefinition.getCategories().stream()
            .flatMap(t -> t.getTranslations().entrySet().stream())
            .map(
                t ->
                    validateNameableTranslations(
                        t.getKey(),
                        t.getValue(),
                        validationContext + " category " + t.getKey() + ": "))
            .flatMap(l -> l.stream())
            .collect(Collectors.toList()));

    violations.addAll(
        riskDefinition.getRiskValues().stream()
            .flatMap(t -> t.getTranslations().entrySet().stream())
            .map(
                t ->
                    validateNameableTranslations(
                        t.getKey(), t.getValue(), validationContext + " risk-values: "))
            .flatMap(l -> l.stream())
            .collect(Collectors.toList()));

    if (violations.size() > 0) {
      throw new TranslationException(violations);
    }
  }

  private static List<Violation> validateNameableTranslations(
      String lang, Map<String, String> translations, String context) {
    return validateAttributeTranslations(lang, translations, NAMEABLE_ATTRIBUTES, context);
  }

  /** Checks if the translation contains only the given attributes. */
  private static List<Violation> validateAttributeTranslations(
      String lang, Map<String, String> translations, Set<String> attributes, String context) {
    return Stream.concat(
            attributes.stream()
                .filter(key -> !translations.containsKey(key))
                .map(key -> new Violation(lang, Reason.MISSING, context + key)),
            translations.keySet().stream()
                .filter(key -> !attributes.contains(key))
                .map(key -> new Violation(lang, Reason.SUPERFLUOUS, context + key)))
        .toList();
  }

  /** Link IDs are translated - custom-aspect-IDs are not. */
  private static Collection<String> linkIdTranslationKeys(
      @NonNull Map<String, LinkDefinition> links) {
    return links.keySet().stream().toList();
  }

  private static Stream<Violation> validateLanguage(
      String lang, Map<String, String> translations, List<String> entityKeys) {
    List<Violation> violations = new ArrayList<>();
    var translationKeys = translations.keySet().stream().toList();
    violations.addAll(noMissingTranslations(lang, entityKeys, translationKeys));
    violations.addAll(noSuperfluousTranslations(lang, entityKeys, translationKeys));
    return violations.stream();
  }

  private static List<Violation> noSuperfluousTranslations(
      String lang, List<String> entityKeys, List<String> translations) {
    return keysInAButNotB(translations, entityKeys).stream()
        .map(k -> new Violation(lang, Reason.SUPERFLUOUS, k))
        .toList();
  }

  private static List<Violation> noMissingTranslations(
      String lang, List<String> entityKeys, List<String> translations) {
    return keysInAButNotB(entityKeys, translations).stream()
        .map(k -> new Violation(lang, Reason.MISSING, k))
        .toList();
  }

  private static List<String> keysInAButNotB(List<String> listA, List<String> listB) {
    return listA.stream().filter(not(listB::contains)).toList();
  }

  private static List<String> statusTranslationKeys(
      @NonNull String type, Map<String, SubTypeDefinition> subTypes) {
    return subTypes.entrySet().stream()
        .flatMap(e -> toStatusKeys(type, e.getKey(), e.getValue()))
        .collect(Collectors.toList());
  }

  private static Stream<String> toStatusKeys(
      String type, String subtype, SubTypeDefinition subType) {
    return subType.getStatuses().stream()
        .map(
            s ->
                SUBTYPE_STATUS_PATTERN.formatted(
                    type.toLowerCase(), // type must be lower case
                    subtype, // subtype and status must retain mixed case characters
                    s));
  }

  private static List<String> attributeTranslationKeys(
      @NonNull Map<String, ? extends CustomAspectDefinition> customAspects) {
    return customAspects.values().stream()
        .flatMap(ca -> ca.getAttributeDefinitions().entrySet().stream())
        .flatMap(
            attrPair ->
                attributeSchemaTranslationKeys(attrPair.getKey(), attrPair.getValue()).stream())
        .collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  private static List<String> attributeSchemaTranslationKeys(
      String attributeKey, AttributeDefinition attributeDefinition) {
    var result = new ArrayList<>(List.of(attributeKey));
    result.addAll(attributeDefinition.getTranslationKeys());
    return result;
  }
}
