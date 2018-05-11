/*******************************************************************************
 * Copyright (c) 2018 Jochen Kemnade.
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
 *     Jochen Kemnade <jk@sernet.de> - initial API and implementation
 ******************************************************************************/
package org.veo.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FileSystemUtils {

    public static List<Map<String, Object>> readObjectsFromDirectory(File directory) throws IOException {
        File[] files = directory.listFiles();

        List<Map<String, Object>> result = new ArrayList<>(files.length);
        for (File file : files) {
            final ObjectMapper mapper = new ObjectMapper();
            Map deserialized = mapper.readValue(file, Map.class);
            result.add(deserialized);
        }
        return result;
    }

    private FileSystemUtils() {

    }
}
