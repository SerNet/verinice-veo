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
package org.veo.web.bean.model;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.veo.model.Element;

/**
 * A gui wrapper around the {@link Element}. 
 * 
 * @author urszeidler
 *
 */
public class PrimefacesTreeNode<T> extends DefaultTreeNode {

    /**
     * 
     */
    private static final long serialVersionUID = 6230854818386012617L;

    public PrimefacesTreeNode() {
		super();
	}

	public PrimefacesTreeNode(T data, TreeNode parent) {
		super(data, parent);
	}

	public PrimefacesTreeNode(T data) {
		super(data);
	}

	public PrimefacesTreeNode(String type, T data, TreeNode parent) {
		super(type, data, parent);
	}

	@SuppressWarnings("unchecked")
    public T getModel() {
		return (T) getData();
	}
}
