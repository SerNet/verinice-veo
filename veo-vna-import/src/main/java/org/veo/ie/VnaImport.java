/*******************************************************************************
 * Copyright (c) 2015 Daniel Murygin.
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
package org.veo.ie;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.sernet.sync.data.SyncData;
import de.sernet.sync.data.SyncLink;
import de.sernet.sync.data.SyncObject;
import de.sernet.sync.mapping.SyncMapping;
import de.sernet.sync.mapping.SyncMapping.MapObjectType;
import de.sernet.sync.sync.SyncRequest;

import org.veo.core.entity.EntityLayerSupertype;

/**
 * This service imports a verinice archive (VNA).
 *
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
@Service
@Scope("prototype")
public class VnaImport {

    private static final Logger LOG = LoggerFactory.getLogger(VnaImport.class);

    private static final int DEFAULT_NUMBER_OF_THREADS = 3;
    private static final int SHUTDOWN_TIMEOUT_IN_SECONDS = 60;

    private int numberOfThreads = DEFAULT_NUMBER_OF_THREADS;

    private CompletionService<ElementImportContext> elementImportCompletionService;
    private CompletionService<LinkImportContext> linkImportCompletionService;
    private ImportContext importContext;
    private int numberOfElements;
    private int numberOfLinks;

    @Autowired
    private ObjectFactory<ElementImportTask> elementImportTaskFactory;

    @Autowired
    private ObjectFactory<LinkImportTask> linkImportTaskFactory;

    @Value("${veo.vna-type-mapping-missing.write}")
    private boolean writeMissingProperties = true;

    @Value("${veo.vna-type-mapping-missing.file}")
    private String missingMappingPropertiesFilePath = "./vna-type-mapping-missing.properties";

    /**
     * Imports a VNA from a byte array.
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Transactional(isolation = Isolation.DEFAULT, propagation = Propagation.REQUIRED)
    public void importVna(InputStream vnaStream) throws InterruptedException, ExecutionException {
        ExecutorService taskExecutor = createExecutor();
        elementImportCompletionService = new ExecutorCompletionService<>(taskExecutor);
        linkImportCompletionService = new ExecutorCompletionService<>(taskExecutor);
        importContext = new ImportContext();
        try {
            importXml(Vna.getXml(vnaStream));
            handleMissingProperties();
        } finally {
            shutdownAndAwaitTermination(taskExecutor);
        }
    }

    private void importXml(SyncRequest syncRequest)
            throws InterruptedException, ExecutionException {
        List<SyncObject> syncObjectList = getSyncObjectList(syncRequest);
        List<MapObjectType> mapObjectTypeList = getMapObjectTypeList(syncRequest);
        LOG.debug("Starting import of objects...");
        numberOfElements = 0;
        importObjectList(null, syncObjectList, mapObjectTypeList);
        if (LOG.isInfoEnabled()) {
            LOG.info("{} objects imported.", numberOfElements);
        }
        List<SyncLink> syncLinkList = getSyncLinkList(syncRequest);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Number of links: {}, starting import...", syncLinkList.size());
        }
        numberOfLinks = 0;
        importLinkList(syncLinkList);
        LOG.info("{} links imported.", numberOfLinks);
    }

    private void importObjectList(EntityLayerSupertype parent, List<SyncObject> syncObjectList,
            List<MapObjectType> mapObjectTypeList) throws InterruptedException, ExecutionException {
        if (syncObjectList != null) {
            for (SyncObject syncObject : syncObjectList) {
                ElementImportTask importTask = elementImportTaskFactory.getObject();
                importTask.setContext(new ElementImportContext(parent, syncObject,
                        mapObjectTypeList));
                elementImportCompletionService.submit(importTask);
            }
            waitForObjectResults(syncObjectList.size());
        }
    }

    private void waitForObjectResults(int n) throws InterruptedException, ExecutionException {
        for (int i = 0; i < n; ++i) {
            ElementImportContext elementImportContext = elementImportCompletionService.take()
                                                                                      .get();
            if (elementImportContext != null) {
                afterImport(elementImportContext);
            }
        }
    }

    private void afterImport(ElementImportContext elementImportContext)
            throws InterruptedException, ExecutionException {
        this.importContext.addElement(elementImportContext);
        this.importContext.addAllMissingMappingProperties(elementImportContext.getMissingMappingProperties());
        EntityLayerSupertype importedElement = elementImportContext.getElement();
        if (importedElement != null) {
            numberOfElements++;
            importObjectList(importedElement, elementImportContext.getSyncObject()
                                                                  .getChildren(),
                             elementImportContext.getMapObjectTypeList());
        }
    }

    private void afterImport(LinkImportContext linkImportContext) {
        this.importContext.addAllMissingMappingProperties(linkImportContext.getMissingMappingProperties());
        if (linkImportContext.getLink() != null) {
            numberOfLinks++;
        }
    }

    private void importLinkList(List<SyncLink> syncLinkList)
            throws InterruptedException, ExecutionException {
        if (syncLinkList != null) {
            numberOfLinks = 0;
            for (SyncLink syncLink : syncLinkList) {
                EntityLayerSupertype source = importContext.getElement(syncLink.getDependant());
                EntityLayerSupertype destination = importContext.getElement(syncLink.getDependency());
                if (source == null || destination == null) {
                    continue;
                }
                LinkImportContext context = new LinkImportContext(syncLink, source, destination);
                LinkImportTask importThread = linkImportTaskFactory.getObject();
                importThread.setContext(context);
                linkImportCompletionService.submit(importThread);
                numberOfLinks++;
            }
            waitForLinkResults(numberOfLinks);
        }
    }

    private void waitForLinkResults(int n) throws InterruptedException, ExecutionException {
        for (int i = 0; i < n; ++i) {
            LinkImportContext linkImportContext = linkImportCompletionService.take()
                                                                             .get();
            if (linkImportContext != null) {
                afterImport(linkImportContext);
            }
        }
    }

    private List<SyncObject> getSyncObjectList(SyncRequest syncRequest) {
        return Optional.ofNullable(syncRequest)
                       .map(SyncRequest::getSyncData)
                       .map(SyncData::getSyncObject)
                       .orElse(Collections.emptyList());
    }

    private List<SyncLink> getSyncLinkList(SyncRequest syncRequest) {
        return Optional.ofNullable(syncRequest)
                       .map(SyncRequest::getSyncData)
                       .map(SyncData::getSyncLink)
                       .orElse(Collections.emptyList());
    }

    private List<MapObjectType> getMapObjectTypeList(SyncRequest syncRequest) {
        return Optional.ofNullable(syncRequest)
                       .map(SyncRequest::getSyncMapping)
                       .map(SyncMapping::getMapObjectType)
                       .orElse(Collections.emptyList());
    }

    private void handleMissingProperties() {
        Properties missingProperties = importContext.getMissingMappingProperties();
        if (writeMissingProperties && !missingProperties.isEmpty()) {
            saveProperties(missingProperties, missingMappingPropertiesFilePath);
        }
    }

    public void saveProperties(Properties prop, String filePath) {
        try (Writer writer = Files.newBufferedWriter(Paths.get(filePath), StandardCharsets.UTF_8)) {
            prop.store(writer, null);
        } catch (IOException ex) {
            LOG.error("Error while writing VNA veo schemata mapping properties", ex);
        }
    }

    private ExecutorService createExecutor() {
        if (LOG.isInfoEnabled()) {
            LOG.info("Number of threads: {}", getNumberOfThreads());
        }
        return Executors.newFixedThreadPool(getNumberOfThreads());
    }

    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(getShutdownTimeoutInSeconds(), TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(getShutdownTimeoutInSeconds(), TimeUnit.SECONDS)) {
                    LOG.error("Thread pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread()
                  .interrupt();
        }
    }

    private int getShutdownTimeoutInSeconds() {
        return SHUTDOWN_TIMEOUT_IN_SECONDS;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public void setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

}
