/*******************************************************************************
 * Copyright (c) 2017 Urs Zeidler.
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
 *     Urs Zeidler uz<at>sernet.de - initial API and implementation
 ******************************************************************************/
package org.veo.web.bean.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.veo.client.schema.ModelSchemaRestClient;
import org.veo.model.Element;
import org.veo.schema.model.ElementDefinition;
import org.veo.schema.model.PropertyDefinition;
import org.veo.service.ElementService;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

/**
 * The cache service gives access to the internal caches which manange the
 * 
 * @author urszeidler
 *
 */
@Component
public class CacheService {
    private static final Logger logger = LoggerFactory.getLogger(CacheService.class.getName());

    @Autowired
    private ElementService elementService;
    @Autowired
    private ModelSchemaRestClient schemaService;

    private LoadingCache<String, Element> elementCache;
    private LoadingCache<String, Map<String, PropertyDefinition>> propertyDefinitionCache;
    private Map<String, ElementDefinition> definitionMap;

    private void createElementCache() {
        RemovalListener<String, Element> elementRemovalListener = new RemovalListener<String, Element>() {

            @Override
            public void onRemoval(RemovalNotification<String, Element> notification) {
                if (logger.isDebugEnabled()) {
                    logger.debug("removing :" + notification);
                }
            }
        };

        elementCache = CacheBuilder.newBuilder().removalListener(elementRemovalListener)
                .maximumSize(25000).build(new CacheLoader<String, Element>() {
                    public Element load(String uuid) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("loadind element with uuid:" + uuid);
                        }

                        long currentTimeMillis = System.currentTimeMillis();
                        Element findOneWithChildren = elementService.loadWithAllReferences(uuid);
                        if (logger.isDebugEnabled()) {
                            logger.debug("loadWithAllReferences stop: "
                                    + (System.currentTimeMillis() - currentTimeMillis));
                        }

                        return findOneWithChildren;
                    }
                });
    }

    private void createDefinitionCache() {
        RemovalListener<String, Map<String, PropertyDefinition>> propertyDefinitionRemovalListener = new RemovalListener<String, Map<String, PropertyDefinition>>() {

            @Override
            public void onRemoval(
                    RemovalNotification<String, Map<String, PropertyDefinition>> notification) {
                if (logger.isDebugEnabled()) {
                    logger.debug("removing :" + notification);
                }
            }
        };

        propertyDefinitionCache = CacheBuilder.newBuilder().maximumSize(2000)
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .removalListener(propertyDefinitionRemovalListener)
                .build(new CacheLoader<String, Map<String, PropertyDefinition>>() {

                    @Override
                    public Map<String, PropertyDefinition> load(String key) throws Exception {
                        ElementDefinition elementType = schemaService.getElementType(key);
                        if (elementType == null)
                            return Collections.emptyMap();

                        Map<String, PropertyDefinition> elementDefinitionMap = new HashMap<>(30);
                        elementType.getProperties()
                                .forEach(pd -> elementDefinitionMap.put(pd.getName(), pd));

                        return elementDefinitionMap;
                    }
                });
    }

    private void createDefinitionMaps() {
        definitionMap = new HashMap<>();
        try {
            List<ElementDefinition> elementTypes = schemaService.getElementTypes();
            elementTypes.stream().forEach(e -> {
                definitionMap.put(e.getElementType(), e);
            });
        } catch (Exception e) {
            logger.error("Error while getting the element types.", e);
        }
    }

    @PostConstruct
    public void initCache() {
        createElementCache();
        createDefinitionCache();
        createDefinitionMaps();
    }

    /**
     * Get the property description of a type. Will return an empty map when not
     * found.
     * 
     * @param type
     * @return
     */
    public Map<String, PropertyDefinition> getElementDefinitionByType(String type) {

        try {
            return propertyDefinitionCache.get(type);
        } catch (ExecutionException e) {
            logger.error("Error while getting the element description for type: " + type, e);
        }
        return Collections.emptyMap();
    }

    /**
     * Get an element from the cache. Will return null when the element is not
     * found.
     * 
     * @param uuid
     * @return
     */
    public Element getElementByUuid(String uuid) {
        try {
            return elementCache.get(uuid);
        } catch (ExecutionException e) {
            logger.error("Error while loadind uuid: " + uuid + " from cache, returning null.", e);
            return null;
        }
    }

    public Map<String, ElementDefinition> getDefinitionMap() {
        return definitionMap;
    }
}
