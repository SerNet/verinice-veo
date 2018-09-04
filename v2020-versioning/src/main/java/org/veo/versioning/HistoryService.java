package org.veo.versioning;

import java.util.List;
import java.util.Map;

public interface HistoryService {

    void save(String uuid, Map<String, Object> content);

    void delete(String uuid);

    List<HistoryEntry> getHistory(String uuid);
}
