/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.modelmapper.ModelMapper;
import org.veo.core.entity.Key;
import org.veo.core.entity.asset.Asset;
import org.veo.core.entity.process.Process;

public class ProcessDto {
    private String id;
    private String name;
    private Asset[] assets;
    private String validFrom;
    private String validUntil;
    
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public ProcessDto(String id, String name, Asset[] assets) {
        super();
        this.id = id;
        this.name = name;
        this.assets = assets;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Asset[] getAssets() {
        return assets;
    }

    public void setAssets(Asset[] assets) {
        this.assets = assets;
    }
    
    public static ProcessDto from(Process asset) {
        ModelMapper mapper = new ModelMapper();
        return mapper.map(asset, ProcessDto.class);
    }
    
    public Process toProcess() {
        Process process = new Process(Key.uuidFrom(this.id), this.name);
        return process;
    }
    
    public Date getValidFromConverted(String timezoneId) throws ParseException {
        return getConvertedDate(this.validFrom, timezoneId);
    }
    
    public void setValidFrom(Date date, String timezoneId) {
        dateFormat.setTimeZone(TimeZone.getTimeZone(timezoneId));
        this.validFrom = dateFormat.format(date);
    }
    
    public Date getValidUntilConverted(String timezoneId) throws ParseException {
        return getConvertedDate(this.validUntil, timezoneId);
    }
    
    public void setValidUntil(Date date, String timezoneId) {
        dateFormat.setTimeZone(TimeZone.getTimeZone(timezoneId));
        this.validUntil = dateFormat.format(date);
    }
    
    private Date getConvertedDate(String date, String timezoneId) throws ParseException {
        dateFormat.setTimeZone(TimeZone.getTimeZone(timezoneId));
        return dateFormat.parse(date);
    }
    
}
