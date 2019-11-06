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

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.modelmapper.ModelMapper;
import org.veo.core.entity.Key;
import org.veo.core.entity.process.Process;
import org.veo.core.usecase.process.GetProcessUseCase.OutputData;

public class ProcessDto {
    
    @Pattern(regexp="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", 
        flags = Pattern.Flag.CASE_INSENSITIVE, 
        message="ID must either be null (for new processes) or a valid UUID string following RFC 4122. ")
    private String id;
    
    @NotNull(message="A name must be present.") 
    @NotBlank
    @Size(min=1, max=255, message="The name must be between 1 and 255 characters long.")
    private String name;
    
    @NotNull
    @Pattern(regexp="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", 
            flags = Pattern.Flag.CASE_INSENSITIVE, 
            message="Unit ID must be a valid UUID string following RFC 4122.")
    private String unitId;
    
    @NotNull(message="An array of asset IDs must be present, but it may be empty.")
    @Size(min=0, max=1000000, message="Array size must be less than one million asset IDs.")
    private String[] assetIDs;
    
    @Pattern(regexp="\\d{4}-\\d{2}-\\d{2}")
    // additional date validation should be done on the parsed date object
    private String validFrom;
    
    @Pattern(regexp="\\d{4}-\\d{2}-\\d{2}")
    // additional date validation should be done on the parsed date object
    private String validUntil;

    public ProcessDto(String id, String unitId, String name, String[] assetIDs) {
        this.id = id;
        this.unitId = unitId;
        this.name = name;
        this.assetIDs = assetIDs;
        
    }

    public String getId() {
        return id;
    }
    
    

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
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

    public String[] getAssetIDs() {
        return assetIDs;
    }

    public void setAssetIDs(String[] assetIDs) {
        this.assetIDs = assetIDs;
    }

    public static ProcessDto from(Process process) {
        ModelMapper mapper = new ModelMapper();
        return mapper.map(process, ProcessDto.class);
    }

    public Process toProcess() {
        ModelMapper mapper = new ModelMapper();
        return mapper.map(this, Process.class);
        //return new Process(Key.uuidFrom(this.id), this.name); TODO implement mapping method from string to key for modelmapper
    }

    public String getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(String validFrom) {
        this.validFrom = validFrom;
    }

    public String getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(String validUntil) {
        this.validUntil = validUntil;
    }


}
