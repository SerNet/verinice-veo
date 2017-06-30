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
import java.util.List;
import java.util.Map;
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
import org.veo.client.schema.ModelSchemaRestClient;
import org.veo.model.Element;
import org.veo.schema.model.ElementDefinition;
import org.veo.schema.model.PropertyDefinition;
import org.veo.service.ElementService;
import org.veo.web.bean.model.PrimefacesTreeNode;
import org.veo.web.util.NumericStringComparator;

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

    private static final Logger logger = LoggerFactory.getLogger(TreeBean.class.getName());

    // @Inject
    // private ElementRepository elementRepository;
    //
    @Inject
    private ElementService elementService;

    @Inject
    private CacheService cacheService;

    @Inject
    private ModelSchemaRestClient schemaService;

    @Inject
    private ElementSelectionRegistry selectionRegistry;

    private PrimefacesTreeNode<Element> root;
    private PrimefacesTreeNode<Element> singleSelectedTreeNode;
    private HashMap<String, ElementDefinition> definitionMap;
    private Map<String, Map<String, PropertyDefinition>> propertyDefinitionMap;

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
    public void setSingleSelectedNode(TreeNode singleSelectedTreeNode) {
        if (logger.isDebugEnabled()) {
            logger.debug("set single selection: " + singleSelectedTreeNode);
        }

        if (singleSelectedTreeNode instanceof PrimefacesTreeNode<?>) {
            this.singleSelectedTreeNode = (PrimefacesTreeNode<Element>) singleSelectedTreeNode;
            String uuid = this.singleSelectedTreeNode.getModel().getUuid();
            Element element = loadSelectedElement(uuid);
            this.singleSelectedTreeNode.setData(element);
            selectionRegistry.setSelectedElement(element);
        } else {
            if (logger.isInfoEnabled())
                logger.info("Not type of PrimefaceTreenode");
        }

    }

    private Element loadSelectedElement(String uuid) {
        return elementService.loadWithAllReferences(uuid);
    }

//    private PrimefacesTreeNode<Element> createRoot_FromCache() {
//        final Element element = new Element();
//        element.setTitle("root");
//        root = new PrimefacesTreeNode<>(element);
//
//        List<String> allRootElements = elementService.allRootElements();
//        for (String string : allRootElements) {
//            try {
//                Element element2 = cacheService.getElementCache().get(string);
//                transformElement(element2, root);
//            } catch (ExecutionException e) {
//                logger.error("Error getting from element cache.", e);
//            }
//        }
//        return root;
//    }
//
//    private void transformElement(Element element2, PrimefacesTreeNode<Element> root2) {
//
//    }

    /**
     * Create the whole tree by selection the data from the repository data.
     * 
     * @return
     */
    private PrimefacesTreeNode<Element> createRoot() {
        final Element element = new Element();
        element.setTitle("root");
        root = new PrimefacesTreeNode<>(element);

        long currentTimeMillis = System.currentTimeMillis();
        if (logger.isDebugEnabled()) {
            logger.debug("Load data start: " + currentTimeMillis);
        }
        Iterable<Element> findAll = elementService.findAll();
        if (logger.isDebugEnabled()) {
            logger.debug("Load Data stop: " + (System.currentTimeMillis() - currentTimeMillis));
        }

        if (definitionMap == null)
            createDefinitionMaps();

        HashMap<Element, PrimefacesTreeNode<Element>> hashMap = new HashMap<>();
        Set<Element> elements = new HashSet<>(50);

        FluentIterable.from(findAll).stream().sorted(new NumericStringComparator()).forEach(t -> {
            if (t.getParent() == null) {
                PrimefacesTreeNode<Element> treeNode = new PrimefacesTreeNode<>(t, root);
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

    private void createDefinitionMaps() {
        definitionMap = new HashMap<>();
        propertyDefinitionMap = new HashMap<>();
        List<ElementDefinition> elementTypes = schemaService.getElementTypes();

        elementTypes.stream().forEach(e -> {
            Map<String, PropertyDefinition> m = new HashMap<>();
            e.getProperties().forEach(pd -> m.put(pd.getName(), pd));
            propertyDefinitionMap.put(e.getElementType(), m);
            definitionMap.put(e.getElementType(), e);
        });
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

    public void setSchemaService(ModelSchemaRestClient schemaService) {
        this.schemaService = schemaService;
    }

    public void setCacheService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public Map<String, ElementDefinition> getDefinitionMap() {
        return definitionMap;
    }

    public Map<String, Map<String, PropertyDefinition>> getPropertyDefinitionMap() {
        return propertyDefinitionMap;
    }

}
