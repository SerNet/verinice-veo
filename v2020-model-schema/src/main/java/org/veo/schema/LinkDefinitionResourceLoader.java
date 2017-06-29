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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * @author sh
 *
 */
public class LinkDefinitionResourceLoader {

    private static final String LINK_DEFINITION_FILENAME = "links.json";
    private static final String LINK_DEFINITION_SUBDIRNAME = "linkdefinitions"; 
    
    private static final Logger log = LoggerFactory.
            getLogger(LinkDefinitionResourceLoader.class);
    
    private LinkDefinitionResourceLoader(){}

    public static List<File> getLinkDefinitionFile(){
        List<File> fileList = new ArrayList<>(2);
        StringBuilder sb = new StringBuilder();
        sb.append(LINK_DEFINITION_SUBDIRNAME);
        sb.append(File.separatorChar);
        sb.append(LINK_DEFINITION_FILENAME);
        ClassPathResource resource = 
                new ClassPathResource(sb.toString());
        try {
            File filesystemFile = resource.getFile();
            if ( filesystemFile != null){
                fileList.add(resource.getFile());
            }

            // getFile() only works, 
            // if file is existant in file system 
            // (not within a jar-file)
            
            final File jarFile = new File(ElementDefinitionResourceLoader.class.
                    getProtectionDomain().getCodeSource().getLocation().getPath());
            
            if(jarFile.isDirectory()){
                for(File f : FileUtils.listFiles(jarFile, new String[]{LINK_DEFINITION_FILENAME}, true)){
                    fileList.add(f);
                };
            }
        } catch (IOException e) {
            log.error("Error loading links.json file",e );
        }
        return fileList;
    }
}
