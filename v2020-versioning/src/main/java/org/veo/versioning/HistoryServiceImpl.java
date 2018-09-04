package org.veo.versioning;

import java.time.ZonedDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class HistoryServiceImpl implements HistoryService {

    @Autowired
    private HistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    public HistoryServiceImpl() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void save(String uuid, Map<String, Object> content) throws JsonProcessingException {

        HistoryEntry entry = createHistoryEntry();
        entry.setDataId(uuid);
        entry.setData(objectMapper.writeValueAsString(content));
        historyRepository.save(entry);

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
        // FIXME
        entry.setAuthor("unknown");
        return entry;
    }
}
