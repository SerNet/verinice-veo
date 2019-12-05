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
 ******************************************************************************/
package org.veo.service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.veo.commons.VeoException;
import org.veo.model.HistoryEntry;
import org.veo.persistence.HistoryRepository;

@Service
public class HistoryServiceImpl implements HistoryService {

    @Autowired
    private HistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    public HistoryServiceImpl() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void save(String uuid, Map<String, Object> content) {

        HistoryEntry entry = createHistoryEntry();
        entry.setDataId(uuid);
        try {
            entry.setData(objectMapper.writeValueAsString(content));
            historyRepository.save(entry);
        } catch (JsonProcessingException e) {
            throw new VeoException(VeoException.Error.UNKNOWN,
                    "unable to write history content as json", e);
        }

    }

    @Override
    public void delete(String uuid) {

        HistoryEntry entry = createHistoryEntry();
        entry.setDataId(uuid);
        historyRepository.save(entry);

    }

    private static HistoryEntry createHistoryEntry() {
        HistoryEntry entry = new HistoryEntry();
        entry.setTimestamp(ZonedDateTime.now());
        Authentication auth = SecurityContextHolder.getContext()
                                                   .getAuthentication();
        if (auth == null) {
            throw new VeoException(VeoException.Error.AUTHENTICATION_REQUIRED,
                    "versioning requires authentication");
        }
        String user = (String) auth.getPrincipal();
        entry.setAuthor(user);
        return entry;
    }

    @Override
    public List<HistoryEntry> getHistory(String uuid) {
        return historyRepository.findByDataId(uuid);
    }
}
