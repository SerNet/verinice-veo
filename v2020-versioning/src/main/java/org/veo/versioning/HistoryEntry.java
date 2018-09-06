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

package org.veo.versioning;

import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class HistoryEntry {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String author;
    @Column(nullable = false)
    private ZonedDateTime timestamp;
    @Column(nullable = false)
    private String dataId;

    // allow arbitrary text length, *PostgreSQL specific*.
    @Column(columnDefinition="TEXT")
    private String data;

    public String getAuthor() {
        return author;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public String getData() {
        return data;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }
}
