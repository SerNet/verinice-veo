/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade
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
package org.veo;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;

/**
 * Workaround module for {@link JsonProperty} evaluation issue on non-getter
 * methods (https://github.com/victools/jsonschema-generator/issues/198) and the
 * missing {@link JsonProperty.Access} support.
 */
final class HonorJsonPropertyOnMethodsModule implements Module {
    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        SchemaGeneratorConfigPart<MethodScope> methodConfigPart = builder.forMethods();
        methodConfigPart.withPropertyNameOverrideResolver(method -> Optional.ofNullable(method.getAnnotation(JsonProperty.class))
                                                                            .map(ann -> ann.value()
                                                                                           .isBlank()
                                                                                                   ? null
                                                                                                   : ann.value()
                                                                                                        .strip())
                                                                            .orElse(null));
        methodConfigPart.withRequiredCheck(method -> Optional.ofNullable(method.getAnnotation(JsonProperty.class))
                                                             .map(JsonProperty::required)
                                                             .orElse(false));
        methodConfigPart.withReadOnlyCheck(method -> Optional.ofNullable(method.getAnnotation(JsonProperty.class))
                                                             .map(JsonProperty::access)
                                                             .orElse(Access.AUTO) == Access.READ_ONLY);
        methodConfigPart.withWriteOnlyCheck(method -> Optional.ofNullable(method.getAnnotation(JsonProperty.class))
                                                              .map(JsonProperty::access)
                                                              .orElse(Access.AUTO) == Access.WRITE_ONLY);
    }
}