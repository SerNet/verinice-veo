package org.veo.service;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.veo.model.Element;
import org.veo.model.VeoException;
import org.veo.persistence.ElementRepository;

/**
 * Database implementation of the ElementMapService.
 *
 * This service uses Spring Data to load, find and persist elements.
 */
@Service("ElementDatabaseService")
@Lazy
public class ElementMapDbService implements ElementMapService {

    @Autowired
    ElementRepository elementRepository;

    @Autowired
    JsonFactory jsonFactory;

    @Autowired
    ElementFactory elementFactory;

    @Override
    public List<Map<String, Object>> findAll() throws IOException {
        Iterable<Element> allElements = elementRepository.findAll();
        return getResultList(allElements);
    }

    @Override
    public Map<String, Object> find(String id) throws IOException {
        if (!elementRepository.existsById(id)) {
            throw new VeoException(VeoException.Error.ELEMENT_NOT_FOUND,
                    String.format(VeoException.ELEMENT_NOT_EXISTS, id));
        }
        Map<String, Object> result = null;
        Element element = elementRepository.findByUuid(id);
        if (element != null) {
            result = jsonFactory.createJson(element);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> findChildren(String parentId) throws IOException {
        if (!elementRepository.existsById(parentId)) {
            throw new VeoException(VeoException.Error.ELEMENT_NOT_FOUND,
                    String.format(VeoException.ELEMENT_NOT_EXISTS, parentId));
        }
        Iterable<Element> allElements = elementRepository.findByParentId(parentId);
        return getResultList(allElements);
    }

    @Override
    public void save(String id, Map<String, Object> json) throws IOException {
        Element element = elementRepository.findByUuid(id);
        if (element == null) {
            throw new VeoException(VeoException.Error.ELEMENT_NOT_FOUND,
                    String.format(VeoException.ELEMENT_NOT_EXISTS, id));
        }
        json.put(JsonFactory.ID, id);
        element = elementFactory.updateElement(json, element);
        elementRepository.save(element);
    }

    @Override
    public String saveNew(Map<String, Object> json) throws IOException {
        Element element = elementFactory.createElement(json);
        String uuid = element.getUuid();
        if (uuid != null && elementRepository.existsById(uuid)) {
            throw new VeoException(VeoException.Error.ELEMENT_EXISTS,
                    "Element with uuid " + uuid + " already exists.");
        }
        return elementRepository.save(element).getUuid();
    }

    @Override
    public void delete(String id) throws IOException {
        elementRepository.deleteById(id);
    }

    private List<Map<String, Object>> getResultList(Iterable<Element> allElements) {
        List<Map<String, Object>> result = new LinkedList<>();
        for (Element element : allElements) {
            result.add(jsonFactory.createJson(element));
        }
        return result;
    }
}