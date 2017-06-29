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
 *     Sebastian Hagedorn sh (at) sernet.de - initial API and implementation
 ******************************************************************************/
package org.veo.schema;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

/**
 * loads all json files of the defined location
 * /src/main/resources/elementdefinitions
 * 
 * tries to access the location in a filesystem-based way
 * and, in addition to that, in a jar-file way
 * 
 * results are merged
 * 
 * @author sh
 *
 */
public class ElementDefinitionResourceLoader {

    private static final String DEFINITIONS_DIR_NAME = "elementdefinitions";
    private static final String JSON_FILE_EXTENSION = "json";
    
    private ElementDefinitionResourceLoader(){}
    
    public static Set<File> getElementDefinitions(){
        Set<File> definitions = new HashSet<>(100);
        StringBuilder sb = new StringBuilder();
        sb.append(File.separatorChar);
        sb.append(DEFINITIONS_DIR_NAME);
        sb.append(File.separatorChar);
        
        ClassPathResource resource  = 
                new ClassPathResource(sb.toString());
        File definitionFolder;
        try {
            definitionFolder = resource.getFile();
            Collection<File> jsonFiles = FileUtils.listFiles(
                    definitionFolder,  new String[]{JSON_FILE_EXTENSION}, true);
            definitions.addAll(jsonFiles);

            final File jarFile = new File(ElementDefinitionResourceLoader.class.
                    getProtectionDomain().getCodeSource().getLocation().getPath());
            
            if (jarFile.isDirectory()){
                File definitionDir = FileUtils.getFile(jarFile, DEFINITIONS_DIR_NAME);
                if (definitionDir != null && definitionDir.isDirectory()){
                    for (File f : FileUtils.listFiles(definitionDir, new String[]{JSON_FILE_EXTENSION}, true)){
                        definitions.add(f);
                    };
                }
            }
            
        } catch (IOException e) {
            Logger.getLogger(ElementDefinitionResourceLoader.class)
            .error("Error loading json files",e );
        }
        return definitions;
    }
    

    

    
}
