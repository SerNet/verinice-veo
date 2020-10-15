/*******************************************************************************
 * Copyright (c) 2020 Jonas Jordan.
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
package org.veo.adapter.presenter.api.dto;

public interface VersionedDto {
    String getCreatedAt();

    void setCreatedAt(String createdAt);

    String getCreatedBy();

    void setCreatedBy(String createdBy);

    String getUpdatedAt();

    void setUpdatedAt(String updatedAt);

    String getUpdatedBy();

    void setUpdatedBy(String updatedBy);

    long getVersion();

    void setVersion(long version);
}
