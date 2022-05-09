/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.adapter.service.domaintemplate.dto.TransformDomainTemplateDto;
import org.veo.adapter.service.domaintemplate.dto.TransformElementDto;
import org.veo.adapter.service.domaintemplate.dto.TransformUnitDumpDto;
import org.veo.core.entity.riskdefinition.RiskDefinition;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

/**
 * A simple domaintemplate builder. This is used by the assembleDomainTemplates Gradle task and
 * should not be called from anywhere else. This is a temporary solution until VEO-399 is
 * implemented.
 */
@Slf4j
@Deprecated
// TODO VEO-399 remove this class
public class DomainTemplateAssemblerMain {
  private static final ReferenceAssembler REFERENCE_ASSEMBLER = new LocalReferenceAssembler();
  private static final ReferenceDeserializer DESERIALIZER =
      new ReferenceDeserializer(REFERENCE_ASSEMBLER);
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .addMixIn(AbstractElementDto.class, TransformElementDto.class)
          .registerModule(new SimpleModule().addDeserializer(IdRef.class, DESERIALIZER))
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .setSerializationInclusion(JsonInclude.Include.NON_NULL);

  public static void main(String[] args) {
    try {
      var snippetPath = Path.of(System.getenv("domaintemplate.dir"));
      var name = System.getenv("domaintemplate.name");
      var version = System.getenv("domaintemplate.templateVersion");
      var revision = System.getenv("domaintemplate.revision");
      var id = new DomainTemplateIdGeneratorImpl().createDomainTemplateId(name, version, revision);
      DomainTemplateAssembler assembler =
          new DomainTemplateAssembler(
              REFERENCE_ASSEMBLER,
              id,
              name,
              System.getenv("domaintemplate.abbreviation"),
              System.getenv("domaintemplate.description"),
              System.getenv("domaintemplate.authority"),
              version,
              revision);

      var typeAssembler = new ElementTypeDefinitionAssembler();
      assembler.setElementTypeDefinitions(
          typeAssembler.loadDefinitions(snippetPath.resolve("types").toFile()));

      for (var prefix : System.getenv("domaintemplate.catalogPrefixes").split(",")) {
        assembler.addCatalog(
            System.getenv(prefix + ".catalog.name"),
            System.getenv(prefix + ".prefix"),
            readCatalogItems(snippetPath.resolve(prefix)));
      }

      assembler.setRiskDefinitions(readRiskDefinitions(snippetPath.resolve("riskdefinitions")));
      TransformDomainTemplateDto templateDto = assembler.createDomainTemplateDto();
      templateDto.setDemoUnitElements(
          assembler.processDemoUnit(
              readDemoUnitElements(new File(System.getenv("domaintemplate.unit-dump-file")))));
      OBJECT_MAPPER
          .writerFor(TransformDomainTemplateDto.class)
          .writeValue(new File(System.getenv("domaintemplate.out.file")), templateDto);
    } catch (Exception e) {
      log.error("Error writing domain", e);
      System.exit(1);
    }
  }

  private static Map<String, RiskDefinition> readRiskDefinitions(Path riskDefinitionPath) {
    Map<String, RiskDefinition> m = new HashMap<>();

    File[] files = riskDefinitionPath.toFile().listFiles((f, name) -> name.endsWith(".json"));
    if (files != null) {
      for (File file : files) {
        RiskDefinition def = readInstanceFile(file, RiskDefinition.class);
        m.put(def.getId(), def);
      }
    }
    return m;
  }

  private static Set<AbstractElementDto> readDemoUnitElements(File demoUnit) {
    TransformUnitDumpDto exportDto = readInstanceFile(demoUnit, TransformUnitDumpDto.class);
    return exportDto.getElements();
  }

  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  private static Map<String, AbstractElementDto> readCatalogItems(Path dir)
      throws DomainTemplateSnippetException {
    return readElements(dir.toFile().listFiles(f -> f.getName().endsWith(".json")));
  }

  private static Map<String, AbstractElementDto> readElements(File[] resources)
      throws DomainTemplateSnippetException {
    return Arrays.stream(resources)
        .map(r -> DomainTemplateAssemblerMain.readInstanceFile(r, AbstractElementDto.class))
        .filter(IdentifiableDto.class::isInstance)
        .collect(Collectors.toMap(e -> ((IdentifiableDto) e).getId(), e -> e));
  }

  private static <T> T readInstanceFile(File resource, Class<T> dtoType)
      throws DomainTemplateSnippetException {
    log.info("process file: {}", resource);
    try (BufferedReader br = Files.newBufferedReader(resource.toPath(), StandardCharsets.UTF_8)) {
      return OBJECT_MAPPER.readValue(br, dtoType);
    } catch (IOException ex) {
      throw new DomainTemplateSnippetException(resource, ex);
    }
  }
}
