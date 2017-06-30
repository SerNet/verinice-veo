/*******************************************************************************
 * Copyright (c) 2017 Sebastian Hagedorn
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
 *     Sebastian Hagedorn sh (at) sernet.de - initial API and implementation
 ******************************************************************************/
package org.veo.web.bean;

import java.util.HashSet;
import java.util.Set;

import javax.faces.bean.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veo.model.Link;
import org.veo.service.LinkService;

/**
 * @author sh
 *
 */
@Named("linkBean")
@SessionScoped
public class LinkBean {
    
    private static final Logger logger = LoggerFactory.getLogger(LinkBean.class.getName());

    // injected
    @Inject
    private LinkService linkService;
    
    private Set<Link> links;
    
    public Set<Link> getLinks() {
        if(links == null){
            links = new HashSet<>();
        }
        if(links.size() == 0){
            linkService.getAll().forEach(link->links.add(link));
        }
        return links;
    }

    public void setLinks(Set<Link> links) {
        this.links = links;
    }
}
