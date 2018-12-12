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
package org.veo.json;

import java.io.InputStream;

/**
 * This class provides methods to access resources for veo JSON processing, e.g. the meta-schema.
 */
public class Resources {
    private static final String META_SCHEMA_FILE_NAME = "meta.json";
    private static final String LINK_META_SCHEMA_FILE_NAME = "link_meta.json";

    private Resources() {

    }

    public static InputStream getMetaSchemaAsStream() {
        return Resources.class.getClassLoader().getResourceAsStream(META_SCHEMA_FILE_NAME);
    }

    public static InputStream getLinkMetaSchemaAsStream() {
        return Resources.class.getClassLoader().getResourceAsStream(LINK_META_SCHEMA_FILE_NAME);
    }
}
