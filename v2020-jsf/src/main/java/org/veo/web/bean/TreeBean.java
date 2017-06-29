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
package org.veo.web.bean;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.primefaces.event.NodeExpandEvent;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.NodeUnselectEvent;
import org.primefaces.model.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.veo.model.Element;
import org.veo.model.ElementProperty;
import org.veo.persistence.ElementRepository;
import org.veo.web.bean.ElementEditor.PropertyEditor;
import org.veo.web.util.NumericStringComparator;

import com.google.common.collect.Lists;

/**
 * The management bean for the element tree.
 * 
 * @author urszeidler
 *
 */
@Component
@ManagedBean
@SessionScoped
public class TreeBean {

    private static final Logger logger = LoggerFactory.getLogger(TreeBean.class.getName());

    @Autowired
    private ElementRepository elementRepository;
    @Autowired
    private CacheService cacheService;
    private PrimefacesTreeNode root;

    private PrimefacesTreeNode singleSelectedTreeNode;

    public void onNodeExpand(NodeExpandEvent event) {
        TreeNode parent = event.getTreeNode();
        if (logger.isDebugEnabled()) {
            logger.debug("onNodeExpand: " + parent.getData());
        }

    }

    public void onNodeSelect(NodeSelectEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("Node Data ::" + event.getTreeNode().getData() + " :: Selected");
        }

    }

    public void onNodeUnSelect(NodeUnselectEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("Node Data ::" + event.getTreeNode().getData() + " :: UnSelected");
        }
    }

    public TreeNode getSingleSelectedNode() {
        return singleSelectedTreeNode;
    }

    public Element getSelectedElement() {
        if (singleSelectedTreeNode == null)
            return null;
        return singleSelectedTreeNode.getModel();
    }

    public void setSingleSelectedNode(TreeNode singleSelectedTreeNode) {
        if (logger.isDebugEnabled()) {
            logger.debug("set single selection: " + singleSelectedTreeNode);
        }

        if (singleSelectedTreeNode != null
                && singleSelectedTreeNode.getClass().isAssignableFrom(PrimefacesTreeNode.class)) {
            this.singleSelectedTreeNode = (PrimefacesTreeNode) singleSelectedTreeNode;
        } else {
            if (logger.isInfoEnabled())
                logger.info("Not type of PrimefaceTreenode");
        }

    }

    private PrimefacesTreeNode createRoot_FromCache() {
        final Element element = new Element();
        element.setTitle("root");
        root = new PrimefacesTreeNode(element);

        List<String> allRootElements = elementRepository.allRootElements();
        for (String string : allRootElements) {
            try {
                Element element2 = cacheService.getElementCache().get(string);
                transformElement(element2,root);
            } catch (ExecutionException e) {
                logger.error("Error getting from element cache.",e);
            }
        }
        
        return root;
    }

    private void transformElement(Element element2, PrimefacesTreeNode root2) {
        // TODO Auto-generated method stub
        
    }

    private PrimefacesTreeNode createRoot() {
        final Element element = new Element();
        element.setTitle("root");
        root = new PrimefacesTreeNode(element);

        long currentTimeMillis = System.currentTimeMillis();
        if (logger.isDebugEnabled()) {
            logger.debug("Load data: start" + currentTimeMillis);
        }
        Iterable<Element> findAll = elementRepository.findAll();
        if (logger.isDebugEnabled()) {
            logger.debug("Load Data stop: " + (System.currentTimeMillis() - currentTimeMillis));
        }

        HashMap<Element, PrimefacesTreeNode> hashMap = new HashMap<>();
        Set<Element> elements = new HashSet<>(50);
        findAll.forEach(t -> {
            if (t.getParent() == null) {
                PrimefacesTreeNode treeNode = new PrimefacesTreeNode(t, root);
                hashMap.put(t, treeNode);
            } else {
                elements.add(t);
            }
        });

        currentTimeMillis = System.currentTimeMillis();
        if (logger.isDebugEnabled()) {
            logger.debug("transform elements start: " + currentTimeMillis);
        }
        tranformElements(hashMap, elements);
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "transform elements stop: " + (System.currentTimeMillis() - currentTimeMillis));
        }
        return root;
    }

    private void tranformElements(HashMap<Element, PrimefacesTreeNode> hashMap,
            Set<Element> elements) {
        if (logger.isDebugEnabled()) {
            logger.debug("transforming elements " + elements.size() + " to go.");
        }

        int size = hashMap.size();

        HashSet<Element> remainElements = new HashSet<>(elements);
        elements.stream().sorted(new NumericStringComparator()).forEach(e -> {
            PrimefacesTreeNode treeNode = hashMap.get(e.getParent());
            if (treeNode != null) {
                PrimefacesTreeNode childNode = new PrimefacesTreeNode(e, treeNode);
                hashMap.put(e, childNode);
                remainElements.remove(e);
            }
        });
        if (hashMap.size() > size) {
            tranformElements(hashMap, remainElements);
        } else if (!remainElements.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Not all elemets are transformed");
            }
        }
    }

    public PrimefacesTreeNode getRoot() {
        if (root == null) {
            root = createRoot();
        }
        return root;
    }

    public void setRoot(PrimefacesTreeNode root) {
        this.root = root;
    }

}
