package org.veo.service;

import java.util.List;
import java.util.Map;

import org.veo.model.HistoryEntry;

public interface HistoryService {

    void save(String uuid, Map<String, Object> content);

    void delete(String uuid);

    List<HistoryEntry> getHistory(String uuid);
}
