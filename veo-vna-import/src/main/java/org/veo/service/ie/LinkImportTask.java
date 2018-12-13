/*******************************************************************************
 * Copyright (c) 2017 Daniel Murygin.
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
 ******************************************************************************/
package org.veo.service.ie;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.veo.model.Element;
import org.veo.model.Link;
import org.veo.service.LinkService;

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
    LinkService linkService;

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

    /*
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
            LOG.error("Error while importing link, start id: " + context.getSource() + ", end id: "
                    + context.getDestination(), e);
        }
        return context;
    }

    private void importLink() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Start importing link, start id: {}, end id: {}...", context.getSource(),
                    context.getDestination());
        }
        if (isImported()) {
            Element source = context.getSource();
            Element destination = context.getDestination();
            Link link = new Link();
            link.setSource(source);
            link.setDestination(destination);
            link.setTypeId(getVeoElementTypeId(context.getSyncLink().getRelationId()));
            linkService.save(link);
            source.getLinksOutgoing().add(link);
            destination.getLinksIncoming().add(link);
            context.setLink(link);
        }
    }

    private boolean isImported() {
        String veriniceLinkTypeId = context.getSyncLink().getRelationId();
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
