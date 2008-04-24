/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.spi.commons;

import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.Serializable;

/**
 * <code>NodeInfoImpl</code> implements a serializable <code>NodeInfo</code>
 * based on another node info.
 */
public class NodeInfoImpl extends ItemInfoImpl implements NodeInfo {

    /**
     * The node id of the underlying node.
     */
    private final NodeId id;

    /**
     * 1-based index of the underlying node.
     */
    private final int index;

    /**
     * The name of the primary node type.
     */
    private final Name primaryTypeName;

    /**
     * The names of assigned mixins.
     */
    private final Name[] mixinNames;

    /**
     * The list of {@link PropertyId}s that reference this node info.
     */
    private final List references;

    /**
     * The list of {@link PropertyId}s of this node info.
     */
    private final List propertyIds;

    /**
     * Creates a new serializable <code>NodeInfo</code> for the given
     * <code>NodeInfo</code>.
     *
     * @param nodeInfo
     */
    public static NodeInfo createSerializableNodeInfo(
            NodeInfo nodeInfo, final IdFactory idFactory) {
        if (nodeInfo instanceof Serializable) {
            return nodeInfo;
        } else {
            PropertyId[] refs = nodeInfo.getReferences();
            List serRefs = new ArrayList();
            for (int i = 0; i < refs.length; i++) {
                NodeId parentId = refs[i].getParentId();
                parentId = idFactory.createNodeId(
                        parentId.getUniqueID(), parentId.getPath());
                serRefs.add(idFactory.createPropertyId(parentId, refs[i].getName()));
            }
            NodeId nodeId = nodeInfo.getId();
            nodeId = idFactory.createNodeId(nodeId.getUniqueID(), nodeId.getPath());
            final Iterator propIds = nodeInfo.getPropertyIds();
            return new NodeInfoImpl(nodeInfo.getName(),
                    nodeInfo.getPath(), nodeId,
                    nodeInfo.getIndex(), nodeInfo.getNodetype(),
                    nodeInfo.getMixins(), serRefs.iterator(),
                    new Iterator() {
                        public boolean hasNext() {
                            return propIds.hasNext();
                        }
                        public Object next() {
                            PropertyId propId = (PropertyId) propIds.next();
                            NodeId parentId = propId.getParentId();
                            idFactory.createNodeId(
                                    parentId.getUniqueID(), parentId.getPath());
                            return idFactory.createPropertyId(
                                    parentId, propId.getName());
                        }
                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    });
        }
    }

    /**
     * Creates a new node info from the given parameters.
     *
     * @param parentId        the parent id.
     * @param name            the name of this item.
     * @param path            the path to this item.
     * @param id              the id of this item.
     * @param index           the index of this item.
     * @param primaryTypeName the name of the primary node type.
     * @param mixinNames      the names of the assigned mixins.
     * @param references      the references to this node.
     * @param propertyIds     the properties of this node.
     * @deprecated Use {@link #NodeInfoImpl(Name, Path, NodeId, int, Name, Name[], Iterator, Iterator)}
     * instead. The parentId is not used any more.
     */
    public NodeInfoImpl(NodeId parentId, Name name, Path path, NodeId id,
                        int index, Name primaryTypeName, Name[] mixinNames,
                        Iterator references, Iterator propertyIds) {
         this(name, path, id, index, primaryTypeName, mixinNames, references, propertyIds);
    }

    /**
     * Creates a new node info from the given parameters.
     *
     * @param name            the name of this item.
     * @param path            the path to this item.
     * @param id              the id of this item.
     * @param index           the index of this item.
     * @param primaryTypeName the name of the primary node type.
     * @param mixinNames      the names of the assigned mixins.
     * @param references      the references to this node.
     * @param propertyIds     the properties of this node.
     */
    public NodeInfoImpl(Name name, Path path, NodeId id,
                        int index, Name primaryTypeName, Name[] mixinNames,
                        Iterator references, Iterator propertyIds) {
        super(name, path, true);
        this.id = id;
        this.index = index;
        this.primaryTypeName = primaryTypeName;
        this.mixinNames = mixinNames;
        this.references = new ArrayList();
        while (references.hasNext()) {
            this.references.add(references.next());
        }
        this.propertyIds = new ArrayList();
        while (propertyIds.hasNext()) {
            this.propertyIds.add(propertyIds.next());
        }
    }

    //-------------------------------< NodeInfo >-------------------------------

    /**
     * {@inheritDoc}
     */
    public NodeId getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public int getIndex() {
        return index;
    }

    /**
     * {@inheritDoc}
     */
    public Name getNodetype() {
        return primaryTypeName;
    }

    /**
     * {@inheritDoc}
     */
    public Name[] getMixins() {
        Name[] ret = new Name[mixinNames.length];
        System.arraycopy(mixinNames, 0, ret, 0, mixinNames.length);
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    public PropertyId[] getReferences() {
        return (PropertyId[]) references.toArray(new PropertyId[references.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator getPropertyIds() {
        return propertyIds.iterator();
    }
}