package org.veo.versioning;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface HistoryService {

    void save(String uuid, Map<String, Object> content) throws JsonProcessingException;

    void delete(String uuid);

    List<HistoryEntry> getHistory(String uuid);
}
