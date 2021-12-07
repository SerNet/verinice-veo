/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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
package org.veo.adapter.service.domaintemplate;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.veo.adapter.presenter.api.dto.ElementTypeDefinitionDto;
import org.veo.core.entity.definitions.CustomAspectDefinition;
import org.veo.core.entity.definitions.LinkDefinition;
import org.veo.core.entity.definitions.SubTypeDefinition;

public class ElementTypeDefinitionAssembler {
    private final ObjectMapper om = new ObjectMapper();

    public Map<String, ElementTypeDefinitionDto> loadDefinitions(File typesDir) {
        return Arrays.stream(typesDir.listFiles())
                     .filter(File::isDirectory)
                     .collect(Collectors.toMap(File::getName, this::loadDefinition));
    }

    private ElementTypeDefinitionDto loadDefinition(File dir) {
        var definition = new ElementTypeDefinitionDto();
        definition.setCustomAspects(readMap(dir, "customAspects", CustomAspectDefinition.class));
        definition.setLinks(readMap(dir, "links", LinkDefinition.class));
        definition.setSubTypes(readMap(dir, "subTypes", SubTypeDefinition.class));
        return definition;
    }

    private <T> Map<String, T> readMap(File directory, String subDir, Class<T> type) {
        return getSubDirFiles(directory, subDir).stream()
                                                .collect(Collectors.toMap(f -> f.getName()
                                                                                .replace(".json",
                                                                                         ""),
                                                                          f -> parse(f, type)));
    }

    private <T> T parse(File f, Class<T> type) {
        try {
            return om.readValue(f, type);
        } catch (IOException e) {
            throw new DomainTemplateSnippetException(f, e);
        }
    }

    private List<File> getSubDirFiles(File root, String subDirName) {
        var subDir = root.toPath()
                         .resolve(subDirName)
                         .toFile();
        if (subDir.exists()) {
            return List.of(subDir.listFiles());
        }
        return List.of();
    }
}
