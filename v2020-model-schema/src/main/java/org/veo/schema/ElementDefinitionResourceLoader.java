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
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.veo.schema.model.ElementDefinition;

import com.google.gson.JsonSyntaxException;

/**
 * @author sh
 *
 */
public class ElementDefinitionResourceLoader {

    
    public static Set<File> getElementDefinitions(){
        Set<File> definitions = new HashSet<>(100);
        ClassPathResource resource  = new ClassPathResource("/elementdefinitions/");
        File definitionFolder;
        try {
            definitionFolder = resource.getFile();
            Collection<File> jsonFiles = FileUtils.listFiles(definitionFolder,  new String[] { "json"}, true);
            definitions.addAll(jsonFiles);
        } catch (IOException e) {
            Logger.getLogger(ElementDefinitionResourceLoader.class).error("Error loading json files",e );
        }
        return definitions;
    }
    

    

    
}
