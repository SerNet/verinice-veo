/*******************************************************************************
 * Copyright (c) 2018 Daniel Murygin
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
 *     Daniel Murygin
 ******************************************************************************/
package org.veo.schema;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * Loads files from a classpath resource.
 *
 * @author Daniel Murygin
 */
public class ClasspathResourceLoader {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ClasspathResourceLoader.class);
    
    private ClasspathResourceLoader(){}

    /**
     * @param directory A directory from the classpath
     * @param extension A file extension
     * @return The content of all files with a extension from a directory .
     */
    public static Set<String> loadResources(String directory, String extension)  {
        Set<String> definitions = new HashSet<>(100);
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:" + directory + "/*." + extension);
            StringWriter writer;
            for (Resource r:resources) {
                writer = new StringWriter();
                IOUtils.copy(r.getInputStream(), writer, "UTF-8");
                definitions.add(writer.toString());
            }
        } catch (Exception e) {
            LOG.error("Error while loading definitions.", e);
        }
        return definitions;
    }

}