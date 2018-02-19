/*******************************************************************************
 * Copyright (c) 2017 Sebastian Hagedorn
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
 *
 * Contributors:
 *     Sebastian Hagedorn - initial API and implementation
 *     Daniel Murygin
 ******************************************************************************/
package org.veo.schema;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Loads all json files of the defined location
 * /src/main/resources/elementdefinitions
 * 
 * Tries to access the location in a filesystem-based way
 * and, in addition to that, in a jar-file way. Both results are merged
 * ans returned.
 * 
 * @author Sebastian Hagedorn
 * @author Daniel Murygin
 *
 */
public class ElementDefinitionResourceLoader {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ElementDefinitionResourceLoader.class);

    private static final String DEFINITIONS_DIR_NAME = "elementdefinitions";
    private static final String JSON_FILE_EXTENSION = "json";
    
    private ElementDefinitionResourceLoader(){}
    
    public static Set<File> getElementDefinitions(){
        Set<File> definitions = new HashSet<>(100);
        try {
            definitions.addAll(loadElementDefinitionsFromFileSystem());
            definitions.addAll(loadElementDefinitionsFromJar());
        } catch (Exception e) {
            LOG.error("Error while loading element definitions.", e);
        }
        return definitions;
    }

    private static Set<File>  loadElementDefinitionsFromFileSystem() throws IOException {
        Set<File> definitions = new HashSet<>(100);
        File definitionFolder = getElementDefinitionsDirectory();
        Collection<File> jsonFiles = FileUtils.listFiles(
                definitionFolder,  new String[]{JSON_FILE_EXTENSION}, true);
        definitions.addAll(jsonFiles);
        return definitions;
    }

    private static File getElementDefinitionsDirectory() throws IOException {
        String elementDefinitionsDirectory = getElementDefinitionsDirectoryPath();
        ClassPathResource resource  =
                new ClassPathResource(elementDefinitionsDirectory);
        return resource.getFile();
    }

    private static String getElementDefinitionsDirectoryPath() {
        StringBuilder sb = new StringBuilder();
        sb.append(File.separatorChar);
        sb.append(DEFINITIONS_DIR_NAME);
        sb.append(File.separatorChar);
        return sb.toString();
    }

    private static Set<File> loadElementDefinitionsFromJar() {
        Set<File> definitions = new HashSet<>(100);
        final File jarFile = new File(ElementDefinitionResourceLoader.class.
                getProtectionDomain().getCodeSource().getLocation().getPath());
        if (jarFile.isDirectory()){
            File definitionDir = FileUtils.getFile(jarFile, DEFINITIONS_DIR_NAME);
            if (definitionDir != null && definitionDir.isDirectory()){
                for (File f : FileUtils.listFiles(definitionDir, new String[]{JSON_FILE_EXTENSION}, true)){
                    definitions.add(f);
                }
            }
        }
        return definitions;
    }

}
