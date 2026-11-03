/*
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
 */
package org.veo.core.usecase.domaintemplate;

import javax.annotation.Nullable;

import com.github.zafarkhaja.semver.Version;

import org.veo.core.entity.DomainBase;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Profile;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.specification.ElementTypeDefinitionValidator;
import org.veo.core.usecase.DomainChangeService;
import org.veo.core.usecase.base.TemplateItemValidator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class DomainTemplateValidator {
  static void validateDomainTemplate(
      DomainTemplate domainTemplate,
      @Nullable DomainTemplate next,
      @Nullable DomainTemplate previous) {
    validateTranslations(domainTemplate);
    validateCatalogAndProfiles(domainTemplate);
    validateVersion(domainTemplate.getTemplateVersion());
    DomainChangeService.validateMigrationDefinition(domainTemplate, previous);
    if (next != null) {
      try {
        DomainChangeService.validateMigrationDefinition(next, domainTemplate);
      } catch (UnprocessableDataException e) {
        throw new UnprocessableDataException(
            "The next major version %s of the template is not compatible with given template: %s"
                .formatted(next.getTemplateVersion(), e.getMessage()));
      }
    }
  }

  private static void validateCatalogAndProfiles(DomainTemplate domainTemplate) {
    domainTemplate.getCatalogItems().forEach(TemplateItemValidator::validate);
    domainTemplate.getProfiles().stream()
        .flatMap((Profile profile) -> profile.getItems().stream())
        .forEach(TemplateItemValidator::validate);
  }

  private static void validateTranslations(DomainBase domain) {
    if (domain.getTranslations() == null || domain.getTranslations().getTranslations().isEmpty()) {
      throw new UnprocessableDataException("No translations");
    }
    domain.getTranslations().getTranslations().entrySet().stream()
        .forEach(e -> {
          if (e.getValue().getName() == null || e.getValue().getName().isEmpty()) {
            throw new UnprocessableDataException(
                "Translated template name missing for '%s'.".formatted(e.getKey().getLanguage()));
          }
        });
    domain.getElementTypeDefinitions().forEach(ElementTypeDefinitionValidator::validate);
  }

  static void validateVersion(Version version) {
    if (!version.preReleaseVersion().isEmpty() || !version.buildMetadata().isEmpty()) {
      throw new IllegalArgumentException(
          "Pre-release & metadata labels are not supported for domain template versions");
    }
  }
}
