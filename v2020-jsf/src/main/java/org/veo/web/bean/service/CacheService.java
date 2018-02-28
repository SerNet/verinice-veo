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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.veo.client.schema.ModelSchemaRestClient;
import org.veo.model.Element;
import org.veo.schema.model.ElementDefinition;
import org.veo.schema.model.PropertyDefinition;
import org.veo.schema.rest.ElementDefinitionResource;
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

    @Value("${elementCache.maxElements:2500}")
    private long maxElementCache;
    @Value("${propertyDefinitionCache.expireTime:60}")
    private long expireTime;

    private LoadingCache<String, Element> elementCache;
    private LoadingCache<String, Map<String, PropertyDefinition>> propertyDefinitionCache;
    private Map<String, ElementDefinition> definitionMap;

    /**
     * Create a loading cache for elements. The elements are read from the
     * database with uuid as key.
     */
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
                .maximumSize(maxElementCache).build(new CacheLoader<String, Element>() {
                    public Element load(String uuid) {

                        long currentTimeMillis = System.currentTimeMillis();
                        Element findOneWithChildren = elementService.loadWithAllReferences(uuid);

                        if (logger.isDebugEnabled()) {
                            logger.debug("loadWithAllReferences need: "
                                    + (System.currentTimeMillis() - currentTimeMillis));
                        }

                        return findOneWithChildren;
                    }
                });
    }

    /**
     * Create a loading cache for Element definitions. The elementDefinitins are
     * read from an rest service. The cache uses a time dependent removal
     * strategies. The time is set in seconds.
     */
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
                .expireAfterAccess(expireTime, TimeUnit.SECONDS)
                .removalListener(propertyDefinitionRemovalListener)
                .build(new CacheLoader<String, Map<String, PropertyDefinition>>() {

                    @Override
                    public Map<String, PropertyDefinition> load(String key) throws Exception {
                        ElementDefinition elementType = schemaService.getElementType(key).getElementDefinition();
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
            Collection<ElementDefinitionResource> elementTypes = schemaService.getElementTypes();
            elementTypes.stream().forEach(e -> {
                definitionMap.put(e.getElementDefinition().getElementType(), e.getElementDefinition());
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
        if (logger.isDebugEnabled()) {
            logger.debug("Get element definition by id:" + type);
        }

        try {
            return propertyDefinitionCache.get(type);
        } catch (Exception e) {
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
        if (logger.isDebugEnabled()) {
            logger.debug("getElement by uuid: " + uuid);
        }

        try {
            return elementCache.get(uuid);
        } catch (Exception e) {
            logger.error("Error while loading uuid: " + uuid + " from cache, returning null.", e);
            return null;
        }
    }
    
    public void removeElementByUuid(String uuid) {
        elementCache.invalidate(uuid);        
    }

    public Map<String, ElementDefinition> getDefinitionMap() {
        return definitionMap;
    }
}
