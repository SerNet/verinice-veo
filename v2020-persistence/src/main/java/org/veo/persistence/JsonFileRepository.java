/*******************************************************************************
 * Copyright (c) 2018 Alexander Ben Nasrallah.
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
 *     Alexander Ben Nasrallah <an@sernet.de> - initial API and implementation
 ******************************************************************************/
package org.veo.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This class implements a MapRepository wich simply writes maps to files in JSON.
 */
public class JsonFileRepository implements MapRepository {
    private File baseDir;
    private ObjectMapper mapper;
    private static final Logger logger = LoggerFactory.getLogger(JsonFileRepository.class.getName());

    /**
     * BaseDir has to exist
     */
    public JsonFileRepository(File baseDir) throws IOException {
        logger.info("New repository at {}", baseDir.getAbsolutePath());
        this.baseDir = baseDir;
        this.mapper = new ObjectMapper();
        Files.createDirectories(this.baseDir.toPath());
    }

    public List<Map<String, Object>> readAll() throws IOException {
        return FileSystemUtils.readObjectsFromDirectory(baseDir);
    }

    @Override
    public Map<String, Object> read(String id) throws IOException {
        return mapper.readValue(elementFile(id), Map.class);
    }

    @Override
    public void update(String filename, Map<String, Object> content) throws IOException {
        File file = elementFile(filename);
        if (!file.createNewFile()) {
            logger.error("Unable to create file {}", filename);
            throw new IOException("Unable to create file " + filename);
        }
        mapper.writeValue(file, content);
    }

    @Override
    public String create(Map<String, Object> content) throws IOException {
        String uuid = UUID.randomUUID().toString();
        content.put("id", uuid);
        update(uuid, content);
        return uuid;
    }

    @Override
    public void delete(String filename) throws IOException {
        File file = elementFile(filename);
        Files.delete(Paths.get(file.getAbsolutePath()));
    }

    private File elementFile(String id) {
        return new File(baseDir, id);
    }
}
