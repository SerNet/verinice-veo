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

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads links.json file from directory
 * /src/main/resources/linkdefinitions.
 *
 * Tries to access the location in a filesystem-based way
 * and, in addition to that, in a jar-file way. Both results are merged
 * ans returned.
 *
 * @author Sebastian Hagedorn
 * @author Daniel Murygin
 */
public class LinkDefinitionResourceLoader {

    private static final String LINK_DEFINITION_FILENAME = "links.json";
    private static final String LINK_DEFINITION_SUBDIRNAME = "linkdefinitions";

    private static final Logger LOG = LoggerFactory.
            getLogger(LinkDefinitionResourceLoader.class);

    private LinkDefinitionResourceLoader() {
    }

    public static Set<File> getLinkDefinitionFile() {
        Set<File> definitions = new HashSet<>(100);
        try {
            definitions.addAll(loadLinkDefinitionsFromFileSystem());
            definitions.addAll(loadLinkDefinitionsFromJar());
        } catch (Exception e) {
            LOG.error("Error while loading link definitions.", e);
        }
        return definitions;
    }

    public static List<File> loadLinkDefinitionsFromFileSystem() throws IOException {
        List<File> fileList = new ArrayList<>(2);
        StringBuilder sb = new StringBuilder();
        sb.append(LINK_DEFINITION_SUBDIRNAME);
        sb.append(File.separatorChar);
        sb.append(LINK_DEFINITION_FILENAME);
        ClassPathResource resource =
                new ClassPathResource(sb.toString());

        File filesystemFile = resource.getFile();
        if (filesystemFile != null) {
            fileList.add(resource.getFile());
        }
        return fileList;
    }

    public static List<File> loadLinkDefinitionsFromJar() {
        List<File> fileList = new ArrayList<>(2);
        final File jarFile = new File(ElementDefinitionResourceLoader.class.
                getProtectionDomain().getCodeSource().getLocation().getPath());

        if (jarFile.isDirectory()) {
            for (File f : FileUtils.listFiles(jarFile, new String[]{LINK_DEFINITION_FILENAME}, true)) {
                fileList.add(f);
            }
            ;
        }
        return fileList;
    }
}
