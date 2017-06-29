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
package org.veo.web.bean.schema;

import java.util.HashMap;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.NodeUnselectEvent;
import org.primefaces.model.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veo.client.schema.ModelSchemaRestClient;
import org.veo.model.Element;
import org.veo.persistence.ElementRepository;
import org.veo.schema.model.ElementDefinition;
import org.veo.web.bean.model.PrimefacesTreeNode;

/**
 * @author urszeidler
 *
 */
// @Component
@ManagedBean(name = "schemaTreeBean")
@SessionScoped
public class SchemaTreeBean {
    private static final Logger logger = LoggerFactory.getLogger(SchemaTreeBean.class.getName());

    private PrimefacesTreeNode<Element> root;
    private PrimefacesTreeNode<Element> singleSelectedTreeNode;

    @ManagedProperty("#{applicationBean.schemaService}")
    private ModelSchemaRestClient schemaService;
    @ManagedProperty("#{applicationBean.elementRepository}")
    private ElementRepository elementRepository;

    private HashMap<String, ElementDefinition> definitionMap;

    private PrimefacesTreeNode<Element> createRoot() {
        final Element element = new Element();
        root = new PrimefacesTreeNode<>(element);

        definitionMap = new HashMap<>();
        List<ElementDefinition> elementTypes = schemaService.getElementTypes();

        elementTypes.stream().forEach(e -> {
            definitionMap.put(e.getElementType(), e);
        });
        
        
//        
//        
//        HashMap<ElementDefinition, PrimefacesTreeNode<ElementDefinition>> hashMap = new HashMap<>();
//        // Set<ElementDefinition> elements = new HashSet<>(50);
//        elementTypes.forEach(t -> {
//            // PrimefacesTreeNode<ElementDefinition> treeNode =
//            new PrimefacesTreeNode<>(t, root);
//            // if (t.getParentId()== 0) {
//            // hashMap.put(t, treeNode);
//            // } else {
//            // elements.add(t);
//            // }
//        });

        return root;
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

    public PrimefacesTreeNode<Element> getRoot() {
        if (root == null) {
            root = createRoot();
        }
        return root;
    }

    public void setRoot(PrimefacesTreeNode<Element> root) {
        this.root = root;
    }

    public Element getSelectedElement() {
        if (singleSelectedTreeNode == null)
            return null;
        return singleSelectedTreeNode.getModel();
    }

    public ModelSchemaRestClient getSchemaService() {
        return schemaService;
    }

    public void setSchemaService(ModelSchemaRestClient schemaService) {
        this.schemaService = schemaService;
    }

    public TreeNode getSingleSelectedNode() {
        return singleSelectedTreeNode;
    }

    public void setSingleSelectedNode(TreeNode singleSelectedTreeNode) {
        if (logger.isDebugEnabled()) {
            logger.debug("set single selection: " + singleSelectedTreeNode);
        }

        if (singleSelectedTreeNode != null
                && singleSelectedTreeNode.getClass().isAssignableFrom(PrimefacesTreeNode.class)) {
            this.singleSelectedTreeNode = (PrimefacesTreeNode<Element>) singleSelectedTreeNode;
        } else {
            if (logger.isInfoEnabled())
                logger.info("Not type of PrimefaceTreenode");
        }

    }

}
