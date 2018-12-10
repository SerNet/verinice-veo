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

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * This interface describes method to store maps of string to object
 * of ony kind.
 */
public interface MapRepository {

    public List<Map<String, Object>> readAll() throws IOException;

    public Map<String, Object> read(String id) throws IOException;

    public void update(String filename, Map<String, Object> content) throws IOException;

    /**
     * Returns the id of the created element.
     */
    public String create(Map<String, Object> content) throws IOException;

    public void delete(String id) throws IOException;
}
