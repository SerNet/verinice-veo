package org.veo.versioning;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.veo.commons.VeoException;

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
            throw new VeoException(VeoException.Error.UNKNOWN, "unable to write history content as json", e);
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new VeoException(VeoException.Error.AUTHENTICATION_REQUIRED, "versioning requires authentication");
        }
        String user = (String)auth.getPrincipal();
        entry.setAuthor(user);
        return entry;
    }

    @Override
    public List<HistoryEntry> getHistory(String uuid) {
        return historyRepository.findByDataId(uuid);
    }
}
