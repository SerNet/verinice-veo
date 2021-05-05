/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2017  Daniel Murygin.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.ie;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * A callable task to import one link from a VNA to database.
 *
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
@Component
@Scope("prototype")
public class LinkImportTask implements Callable<LinkImportContext> {

    private static final Logger LOG = LoggerFactory.getLogger(LinkImportTask.class);

    private LinkImportContext context;

    @Autowired
    @javax.annotation.Resource(name = "SchemaTypeIdMapper")
    private TypeIdMapper typeIdMapper;

    public LinkImportTask() {
        super();
    }

    public LinkImportTask(LinkImportContext context) {
        super();
        this.context = context;
    }

    /**
     * @see java.util.concurrent.Callable#call()
     */
    @Override
    public LinkImportContext call() throws Exception {
        try {
            importLink();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Link imported, start id: {}, end id: {}", context.getSource(),
                          context.getDestination());
            }
        } catch (Exception e) {
            LOG.error("Error while importing link, start id: {}, end id: {}", context.getSource(),
                      context.getDestination(), e);
        }
        return context;
    }

    private void importLink() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Start importing link, start id: {}, end id: {}...", context.getSource(),
                      context.getDestination());
        }
        if (isImported()) {
            // TODO create link, persist link and add it to source and destination link
            // lists.
        }
    }

    private boolean isImported() {
        String veriniceLinkTypeId = context.getSyncLink()
                                           .getRelationId();
        String veoLinkTypeId = getVeoElementTypeId(veriniceLinkTypeId);
        boolean isVeoPropertyId = (veoLinkTypeId != null);
        if (!isVeoPropertyId) {
            context.addMissingMappingProperty(veriniceLinkTypeId);
        }
        return isVeoPropertyId;
    }

    private String getVeoElementTypeId(String vnaTypeId) {
        return typeIdMapper.getVeoElementTypeId(vnaTypeId);
    }

    public void setContext(LinkImportContext context) {
        this.context = context;
    }

}
