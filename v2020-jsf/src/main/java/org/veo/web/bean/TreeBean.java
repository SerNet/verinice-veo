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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.event.NodeExpandEvent;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.NodeUnselectEvent;
import org.primefaces.model.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veo.model.Element;
import org.veo.service.ElementService;
import org.veo.web.bean.model.PrimefacesTreeNode;
import org.veo.web.bean.service.CacheService;
import org.veo.util.string.NumericStringComparator;

import com.google.common.collect.FluentIterable;

/**
 * The management bean for the element tree.
 * 
 * @author urszeidler
 *
 */
@Named("treeBean")
@SessionScoped
public class TreeBean {

    private static final Logger logger = LoggerFactory.getLogger(TreeBean.class);

    @Inject
    private ElementService elementService;

    @Inject
    private CacheService cacheService;

    @Inject
    private ElementSelectionRegistry selectionRegistry;

    private PrimefacesTreeNode<Element> root;
    private PrimefacesTreeNode<Element> singleSelectedTreeNode;

    public void delete() {
        Element selectedElement = selectionRegistry.getSelectedElement();
        if (selectedElement == null || singleSelectedTreeNode == null)
            return;

        if (logger.isDebugEnabled()) {
            logger.debug("Deleting element: " + selectedElement);
        }

        elementService.delete(selectedElement);
        cacheService.removeElementByUuid(selectedElement.getUuid());
        TreeNode parent = singleSelectedTreeNode.getParent();
        parent.getChildren().remove(singleSelectedTreeNode);
    }

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

    public Element getSelectedElement() {
        if (singleSelectedTreeNode == null)
            return null;
        return singleSelectedTreeNode.getModel();
    }

    public TreeNode getSingleSelectedNode() {
        return singleSelectedTreeNode;
    }

    /**
     * Store the selection from the widget element.
     * 
     * @param singleSelectedTreeNode
     */
    @SuppressWarnings("unchecked")
    public void setSingleSelectedNode(TreeNode singleSelectedTreeNode) {
        if (logger.isDebugEnabled()) {
            logger.debug("set single selection: " + singleSelectedTreeNode);
        }
        if (singleSelectedTreeNode != null
                && singleSelectedTreeNode.equals(this.singleSelectedTreeNode))
            return;

        if (singleSelectedTreeNode instanceof PrimefacesTreeNode<?>) {
            this.singleSelectedTreeNode = (PrimefacesTreeNode<Element>) singleSelectedTreeNode;
            String uuid = this.singleSelectedTreeNode.getModel().getUuid();
            Element element = loadSelectedElement(uuid);
            if (element == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "Error while loading element from cache, element return as null, reset to orignal element.");
                }
                element = this.singleSelectedTreeNode.getModel();
            }
            this.singleSelectedTreeNode.setData(element);
            selectionRegistry.setSelectedElement(element);
        } else {
            if (logger.isInfoEnabled())
                logger.info("Not type of PrimefaceTreenode");
        }
    }

    private Element loadSelectedElement(String uuid) {
        if (logger.isDebugEnabled()) {
            logger.debug("load element by uuid: " + uuid);
        }
        return cacheService.getElementByUuid(uuid);
    }

    /**
     * Create the whole tree by selection the data from the
     * {@link ElementService}.
     * 
     * @return
     */
    private PrimefacesTreeNode<Element> createRoot() {
        final Element element = new Element();
        element.setTitle("root");
        root = new PrimefacesTreeNode<>(element);

        long currentTimeMillis = System.currentTimeMillis();
        Iterable<Element> findAll = elementService.findAll();
        if (logger.isDebugEnabled()) {
            logger.debug("Load Data need: " + (System.currentTimeMillis() - currentTimeMillis));
        }

        HashMap<Element, PrimefacesTreeNode<Element>> hashMap = new HashMap<>();
        Set<Element> elements = new HashSet<>(250);

        FluentIterable.from(findAll).stream().sorted(new NumericStringComparator()).forEach(t -> {
            if (t.getParent() == null) {
                PrimefacesTreeNode<Element> treeNode = new PrimefacesTreeNode<>(t, root);
                hashMap.put(t, treeNode);
            } else {
                elements.add(t);
            }
        });

        currentTimeMillis = System.currentTimeMillis();
        tranformElements(hashMap, elements);
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "transform elements need: " + (System.currentTimeMillis() - currentTimeMillis));
        }
        return root;
    }

    /**
     * Transform the sub elements of the tree.
     * 
     * @param hashMap
     *            the already transformed elements
     * @param elements
     *            the element need to transform
     */
    private void tranformElements(HashMap<Element, PrimefacesTreeNode<Element>> hashMap,
            Set<Element> elements) {
        if (logger.isDebugEnabled()) {
            logger.debug("transforming elements " + elements.size() + " to go.");
        }
        int size = hashMap.size();

        HashSet<Element> remainElements = new HashSet<>(elements);
        elements.stream().sorted(new NumericStringComparator()).forEach(e -> {
            PrimefacesTreeNode<Element> treeNode = hashMap.get(e.getParent());
            if (treeNode != null) {
                PrimefacesTreeNode<Element> childNode = new PrimefacesTreeNode<>(e, treeNode);
                hashMap.put(e, childNode);
                remainElements.remove(e);
            }
        });
        if (hashMap.size() > size) {
            tranformElements(hashMap, remainElements);
        } else if (!remainElements.isEmpty()) {
            // maybe do some thing more than log
            if (logger.isDebugEnabled()) {
                logger.debug("Not all elemets are transformed");
            }
        }
    }

    public PrimefacesTreeNode<Element> getRoot() {
        if (root == null) {
            root = createRoot();
        }
        return root;
    }

    public void setRoot(PrimefacesTreeNode<Element> root) {
        this.root = root;
    }
}
