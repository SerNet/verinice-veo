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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class TranslationValidator {
  /**
   * The naming convention used for the translation of subtype statuses is: {@code
   * <TYPE>_<SUBTYPE>_status_<STATUS>}
   */
  public static final String SUBTYPE_NAME_PATTERN = "%s_%s_%s";

  public static final String SUBTYPE_STATUS_PATTERN = "%s_%s_status_%s";

  private static final Pattern LEADING_SPACE_PATTERN = Pattern.compile("^\\s.*$");
  private static final Pattern TRAILING_SPACE_PATTERN = Pattern.compile("^.*\\s$");

  public static final Set<String> NAMEABLE_ATTRIBUTES =
      Set.of(Nameable.NAME, Nameable.ABBREVIATION, Nameable.DESCRIPTION);

  public record Violation(Locale language, Reason reason, String key) {}

  @SuppressFBWarnings("PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_CLASS_NAMES")
  public enum Reason {
    MISSING,
    SUPERFLUOUS,
    LEADING_SPACES,
    TRAILING_SPACES
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
      Map<Locale, Map<String, String>> translations,
      String type,
      Map<String, CustomAspectDefinition> customAspects,
      Map<String, LinkDefinition> links,
      Map<String, SubTypeDefinition> subTypes) {

    List<String> allEntityKeys = new ArrayList<>();
    allEntityKeys.addAll(attributeTranslationKeys(customAspects));
    allEntityKeys.addAll(attributeTranslationKeys(links));
    allEntityKeys.addAll(linkIdTranslationKeys(links));
    allEntityKeys.addAll(subTypeNameTranslationKeys(type, subTypes));
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
            .flatMap(List::stream)
            .collect(Collectors.toList()));

    violations.addAll(
        riskDefinition.getProbability().getTranslations().entrySet().stream()
            .map(
                t ->
                    validateNameableTranslations(
                        t.getKey(), t.getValue(), validationContext + " probability: "))
            .flatMap(List::stream)
            .collect(Collectors.toList()));

    violations.addAll(
        riskDefinition.getImplementationStateDefinition().getTranslations().entrySet().stream()
            .map(
                t ->
                    validateNameableTranslations(
                        t.getKey(), t.getValue(), validationContext + " implementation state: "))
            .flatMap(List::stream)
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
            .flatMap(List::stream)
            .collect(Collectors.toList()));

    violations.addAll(
        riskDefinition.getRiskValues().stream()
            .flatMap(t -> t.getTranslations().entrySet().stream())
            .map(
                t ->
                    validateNameableTranslations(
                        t.getKey(), t.getValue(), validationContext + " risk-values: "))
            .flatMap(List::stream)
            .collect(Collectors.toList()));

    if (violations.size() > 0) {
      throw new TranslationException(violations);
    }
  }

  private static List<Violation> validateNameableTranslations(
      Locale lang, Map<String, String> translations, String context) {
    return validateAttributeTranslations(lang, translations, NAMEABLE_ATTRIBUTES, context);
  }

  /** Checks if the translation contains only the given attributes. */
  private static List<Violation> validateAttributeTranslations(
      Locale lang, Map<String, String> translations, Set<String> attributes, String context) {
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
      Locale lang, Map<String, String> translations, List<String> entityKeys) {
    List<Violation> violations = new ArrayList<>();
    var translationKeys = translations.keySet().stream().toList();
    log.debug("Validating language: {}", lang.toLanguageTag());
    violations.addAll(noMissingTranslations(lang, entityKeys, translationKeys));
    violations.addAll(noSuperfluousTranslations(lang, entityKeys, translationKeys));
    violations.addAll(noLeadingSpaces(lang, translations));
    violations.addAll(noTrailingSpaces(lang, translations));
    return violations.stream();
  }

  private static Collection<Violation> noLeadingSpaces(
      Locale lang, Map<String, String> translationKeys) {
    return translationKeys.entrySet().stream()
        .filter(entry -> LEADING_SPACE_PATTERN.matcher(entry.getValue()).find())
        .map(entry -> new Violation(lang, Reason.LEADING_SPACES, entry.getKey()))
        .collect(Collectors.toSet());
  }

  private static Collection<Violation> noTrailingSpaces(
      Locale lang, Map<String, String> translationKeys) {
    return translationKeys.entrySet().stream()
        .filter(entry -> TRAILING_SPACE_PATTERN.matcher(entry.getValue()).find())
        .map(entry -> new Violation(lang, Reason.TRAILING_SPACES, entry.getKey()))
        .collect(Collectors.toSet());
  }

  private static List<Violation> noSuperfluousTranslations(
      Locale lang, List<String> entityKeys, List<String> translations) {
    return keysInAButNotB(translations, entityKeys).stream()
        .map(k -> new Violation(lang, Reason.SUPERFLUOUS, k))
        .toList();
  }

  private static List<Violation> noMissingTranslations(
      Locale lang, List<String> entityKeys, List<String> translations) {
    return keysInAButNotB(entityKeys, translations).stream()
        .map(k -> new Violation(lang, Reason.MISSING, k))
        .toList();
  }

  private static List<String> keysInAButNotB(List<String> listA, List<String> listB) {
    return listA.stream().filter(not(listB::contains)).toList();
  }

  private static Collection<String> subTypeNameTranslationKeys(
      @NonNull String type, Map<String, SubTypeDefinition> subTypes) {
    return subTypes.entrySet().stream().flatMap(e -> toSubTypeNameKeys(type, e.getKey())).toList();
  }

  private static Stream<String> toSubTypeNameKeys(String type, String subType) {
    return Stream.of("singular", "plural")
        .map(sp -> SUBTYPE_NAME_PATTERN.formatted(type, subType, sp));
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
