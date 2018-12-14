/*******************************************************************************
 * Copyright (c) 2018 Daniel Murygin.
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
 ******************************************************************************/
package org.veo.service.ie;

/**
 *
 * This interface provides methods for mapping IDs from verinice archives (VNA)
 * to IDs from veo.
 */
public interface TypeIdMapper {

    /**
     * @param vnaElementTypeId
     *            An element ID from a VNA
     * @return the veo element ID for the given ID from a VNA
     */
    String getVeoElementTypeId(String vnaElementTypeId);

    /**
     * @param vnaPropertyTypeid
     *            A property ID from a VNA
     * @return the veo property ID for the given ID from a VNA
     */
    String getVeoPropertyTypeId(String vnaPropertyTypeid);

}
