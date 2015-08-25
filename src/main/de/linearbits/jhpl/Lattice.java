/* ******************************************************************************
 * Copyright (c) 2015 Fabian Prasser.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Fabian Prasser - initial API and implementation
 * ****************************************************************************
 */
package de.linearbits.jhpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import de.linearbits.jhpl.JHPLIterator.WrappedIntArrayIterator;
import de.linearbits.jhpl.JHPLIterator.WrappedLongIterator;
import de.linearbits.jhpl.JHPLTrie.ElementComparator;
import de.linearbits.jhpl.PredictiveProperty.Direction;

/**
 * This class implements a storage structure for information about elements in very large lattices. To avoid 
 * materialization, it supports "predictive" properties, i.e., properties which are inherited to predecessors 
 * or successors of an element in the lattice. Properties can be predictive either for all (direct and indirect) 
 * successors or for all (direct and indirect) predecessors or for both (direct and indirect) successors and
 * (direct and indirect) predecessors of a given element. In addition to predictive properties, data can 
 * be associated to individual nodes.<br>
 * <br>
 * The methods in this class are optimized for read access and have the following run-time complexities:
 * <ul>
 *  <li>getData(node): Retrieves the associated data. Guaranteed O(1).</li>
 *  <li>putData(node): Associates data with a node. Guaranteed O(1).</li>
 *  <li>contains(node): Returns whether any data is stored about a node. Guaranteed O(1).</li>
 *  <li>hasProperty(node): Returns whether any property is associated with a node. Guaranteed O(1).</li>
 *  <li>hasProperty(node, property): Determines whether a node is associated with a (predictive) property. Guaranteed O(1).</li>
 *  <li>putProperty(node, property): Associates a node and predecessors or successors with a (predictive) property. 
 *      The worst-case run-time complexity of this operation is O(#nodes for which put has already been called with this property).
 *      Depending on your access pattern (e.g. sequential in terms of a path from bottom to top), it may be reduced up to 
 *      a complexity of amortized O(1).</li>
 *  <li>removeProperty(node, property): Removes the association between a node and predecessors or successors with a (predictive) property. 
 *      The worst-case run-time complexity of this operation is O(#nodes for which put has been called with this property).
 *      Depending on your access pattern (e.g. sequential in terms of a path from bottom to top), it may be reduced up to 
 *      a complexity of amortized O(1).</li>
 * </ul>
 * 
 * @author Fabian Prasser
 *
 * @param <T> The type of values in the dimensions of the lattice
 * @param <U> The type of associated data
 */
public class Lattice<T, U> {

    /** Data */
    private final JHPLData<T, U>                            data;
    /** All materialized nodes */
    private final JHPLTrie                                  master;
    /** Nodes */
    private final JHPLNodes<T>                              nodes;
    /** Tries for properties */
    private final Map<PredictiveProperty, JHPLTrie>         propertiesDown;
    /** Tries for properties */
    private final Map<PredictiveProperty, JHPLTrie>         propertiesUp;
    /** Tries for properties */
    private final Map<PredictiveProperty, JHPLMap<Boolean>> propertiesNone;
    /** Space */
    private final JHPLSpace<T>                              space;
    /** Unsafe */
    private final JHPLUnsafe                                unsafe;
    /** Number of nodes */
    private final long                                      numNodes;
    /** Tack modifications */
    private boolean                                         modified = false;

    /**
     * Constructs a new lattice
     * 
     * @param elements One array of elements per dimension, ordered from the lowest to the highest element
     */
    @SuppressWarnings("unchecked")
    public Lattice(T[]... elements) {

        if (elements == null) {
            throw new NullPointerException("Elements must not be null");
        }
        if (elements.length == 0) {
            throw new IllegalArgumentException("Elements must not be of size zero");
        }
        for (int i=0; i<elements.length; i++) {
            if (elements[i] == null) {
                throw new NullPointerException("Elements must not be null");
            }
            if (elements[i].length == 0) {
                throw new IllegalArgumentException("Elements must not be of size zero");
            }
            for (T t : elements[i]) {
                if (t == null) {
                    throw new NullPointerException("Elements must not contain null");
                }
            }
        }
        
        double dSize = 1d;
        long lSize = 1l;
        for (T[] dimension : elements) {
            dSize *= dimension.length;
            lSize *= dimension.length;
        }
        if (dSize > Long.MAX_VALUE) {
            throw new IllegalArgumentException("Lattice must not have more than Long.MAX_VALUE elements");
        }
        this.numNodes = lSize;
        
        this.nodes = new JHPLNodes<T>(this, elements);
        this.space = new JHPLSpace<T>(nodes, elements);
        this.data = new JHPLData<T, U>(space, elements);
        this.propertiesUp = new HashMap<PredictiveProperty, JHPLTrie>();
        this.propertiesDown = new HashMap<PredictiveProperty, JHPLTrie>();
        this.propertiesNone = new HashMap<PredictiveProperty, JHPLMap<Boolean>>();
        this.master = new JHPLTrie(this);
        this.unsafe = new JHPLUnsafe(this);
    }
        
    /**
     * Returns whether this lattice stores any information about the given node.
     * This is a guaranteed O(1) operation.
     * @param node
     * @return
     */
    public boolean contains(int[] node) {
        return master.contains(node);
    }
    
    /**
     * Returns a pretty accurate estimation of the memory consumed by this lattice
     * @return
     */
    public long getByteSize() {
        long size = 0;
        size += this.data.getByteSize();
        size += this.master.getByteSize();
        for (JHPLTrie trie : this.propertiesUp.values()) {
            size += trie.getByteSize();
        }
        for (JHPLTrie trie : this.propertiesDown.values()) {
            size += trie.getByteSize();
        }
        for (JHPLMap<Boolean> map : this.propertiesNone.values()) {
            size += map.getByteSize();
        }
        return size;
    }
    
    /**
     * Returns the data associated with the given node, <code>null</code> if there is none. <br>
     * <br>
     * This is a guaranteed O(1) operation for any node.
     * 
     * @param node
     * @return
     */
    public U getData(int[] node) {
        this.nodes.checkNode(node);
        return this.data.get(node);
    }
    
    /**
     * Returns whether the node has any property. This is a guaranteed O(1) operation.
     * @param node
     * @return
     */
    public boolean hasProperty(int[] node) {
        
        for (PredictiveProperty property : this.propertiesUp.keySet()) {
            if (this.propertiesUp.get(property).contains(node, ElementComparator.LEQ)){
                return true;
            }
        }
        for (PredictiveProperty property : this.propertiesDown.keySet()) {
            if (this.propertiesDown.get(property).contains(node, ElementComparator.GEQ)){
                return true;
            }
        }
        for (PredictiveProperty property : this.propertiesNone.keySet()) {
            Boolean result = this.propertiesNone.get(property).get(space().toId(node));
            result = result == null ? false : result;
            return result;
        }
        return false;
    }
    
    /**
     * Returns whether the given node has the given property. <br>
     * <br>
     * This is a guaranteed O(1) operation for any node.
     * @param node
     * @param property
     * @return
     */
    public boolean hasProperty(int[] node, PredictiveProperty property) {
        checkProperty(property);
        this.nodes.checkNode(node);
        if (property.getDirection() == Direction.UP) {
            return this.propertiesUp.get(property).contains(node, ElementComparator.LEQ);
        } else if (property.getDirection() == Direction.DOWN) {
            return this.propertiesDown.get(property).contains(node, ElementComparator.GEQ);
        } else if (property.getDirection() == Direction.BOTH) {
            return (this.propertiesUp.get(property).contains(node, ElementComparator.LEQ) || 
                    this.propertiesDown.get(property).contains(node, ElementComparator.GEQ));
        } else {
            Boolean result = this.propertiesNone.get(property).get(space().toId(node));
            result = result == null ? false : result;
            return result;
        }
    }

    /**
     * Returns whether the given node has the given property. <br>
     * <br>
     * This is a guaranteed O(1) operation for any node.
     * @param node
     * @param property
     * @return
     */
    public boolean hasProperty(long identifier, PredictiveProperty property) {
        checkProperty(property);
        if (property.getDirection() == Direction.UP) {
            return this.propertiesUp.get(property).contains(identifier, ElementComparator.LEQ, nodes.getMultiplier());
        } else if (property.getDirection() == Direction.DOWN) {
            return this.propertiesDown.get(property).contains(identifier, ElementComparator.GEQ, nodes.getMultiplier());
        } else if (property.getDirection() == Direction.BOTH) {
            return (this.propertiesUp.get(property).contains(identifier, ElementComparator.LEQ, nodes.getMultiplier()) || 
                    this.propertiesDown.get(property).contains(identifier, ElementComparator.GEQ, nodes.getMultiplier()));
        } else {
            Boolean result = this.propertiesNone.get(property).get(identifier);
            result = result == null ? false : result;
            return result;
        }
    }
    
    /** 
     * Enumerates all nodes stored in the lattice
     * @return
     */
    public Iterator<int[]> listNodes() {
        return new WrappedIntArrayIterator(this, this.master.iterator());
    }

    /**
     * Enumerates all nodes stored on the given level
     * @param level
     * @return
     */
    public Iterator<int[]> listNodes(int level) {
        return new WrappedIntArrayIterator(this, this.master.iterator(level));
    }
    
    /** 
     * Enumerates all nodes stored in the lattice
     * @return
     */
    public Iterator<Long> listNodesAsIdentifiers() {
        return new WrappedLongIterator(this, this.master.iteratorLong(this.nodes.getMultiplier()));
    }

    /**
     * Returns a class for working with nodes
     * @return
     */
    public JHPLNodes<T> nodes() {
        return nodes;
    }

    /**
     * Returns the number of dimensions of this lattice
     * @return
     */
    public int numDimensions() {
        return this.nodes.getDimensions();
    }

    /**
     * Returns the number of levels in this lattice
     * @return
     */
    public int numLevels() {
        return master.getLevels();
    }
    
    /**
     * Returns the number of nodes in this lattice
     * @return
     */
    public long numNodes(){
        return numNodes;
    }

    /**
     * Associates the given node with the given data. <br>
     * <br>
     * The worst-case run-time complexity of this operation is O(#nodes for which put has already been called).
     * Analogously to put operations on hash tables it is actually amortized O(1). 
     * @param node
     * @param data
     */
    public void putData(int[] node, U data) {
        
        this.nodes.checkNode(node);
        this.setModified();
        
        this.data.put(node, data);

        // Store in master trie
        this.master.put(node);
    }
    
    /**
     * Stores the given property for the given node. If the property is predictive in an upwards direction, it 
     * will also be stored for all successors of the given node. If the property is predictive in a downwards direction,
     * it will also be stored for all predecessors of the given node.<br>
     * <br>
     * The worst-case run-time complexity of this operation is O(#nodes for which put has already been called with this property).
     * Depending on your access pattern (e.g. sequential in terms of a path from bottom to top), it may be reduced up to 
     * a complexity of amortized O(1). 
     * 
     * @param node
     * @param property
     */
    public void putProperty(int[] node, PredictiveProperty property) {

        this.nodes.checkNode(node);
        this.checkProperty(property);
        this.setModified();
        
        // Store in master trie
        this.master.put(node);
        
        // Note: Don't remove this. It is very important for the whole thing to work correctly! Example: 
        // Assume property A is predictive in an upwards direction.
        // We first add property A for (1, 2, 1)
        // We then add property A for (1, 3, 25). This will be caught by this check. 
        // If not: we *query* for (1, 3, 20) and the result will be false 
        if (hasProperty(node, property)) {
            return;
        }

        // Note: Don't remove this. It is very important for the whole thing to work correctly! Example: 
        // Assume property A is predictive in an upwards direction.
        // We first add property A for (1, 3, 25)
        // We then add property A for (1, 2, 1). This will be caught by this check. 
        // If not: we *query* for (1, 3, 20) and the result will be false 
        removeProperty(node, property);
        
        if (property.getDirection() == Direction.UP) {
            this.propertiesUp.get(property).put(node);
        } else if (property.getDirection() == Direction.DOWN) {
            this.propertiesDown.get(property).put(node);
        } else if (property.getDirection() == Direction.BOTH) {
            this.propertiesUp.get(property).put(node); 
            this.propertiesDown.get(property).put(node);
        } else {
            this.propertiesNone.get(property).put(space().toId(node), true); 
        }
    }

    /**
     * Stores the given property for the given node. If the property is predictive in an upwards direction, it 
     * will also be stored for all successors of the given node. If the property is predictive in a downwards direction,
     * it will also be stored for all predecessors of the given node.<br>
     * <br>
     * The worst-case run-time complexity of this operation is O(#nodes for which put has already been called with this property).
     * Depending on your access pattern (e.g. sequential in terms of a path from bottom to top), it may be reduced up to 
     * a complexity of amortized O(1). 
     * 
     * @param node
     * @param property
     */
    public void putProperty(long identifier, PredictiveProperty property) {

        this.setModified();
        
        // Store in master trie
        this.master.put(identifier, nodes.getMultiplier());
        
        // Note: Don't remove this. It is very important for the whole thing to work correctly! Example: 
        // Assume property A is predictive in an upwards direction.
        // We first add property A for (1, 2, 1)
        // We then add property A for (1, 3, 25). This will be caught by this check. 
        // If not: we *query* for (1, 3, 20) and the result will be false 
        if (hasProperty(identifier, property)) {
            return;
        }

        // Note: Don't remove this. It is very important for the whole thing to work correctly! Example: 
        // Assume property A is predictive in an upwards direction.
        // We first add property A for (1, 3, 25)
        // We then add property A for (1, 2, 1). This will be caught by this check. 
        // If not: we *query* for (1, 3, 20) and the result will be false 
        removeProperty(identifier, property);
        
        if (property.getDirection() == Direction.UP) {
            this.propertiesUp.get(property).put(identifier, nodes.getMultiplier());
        } else if (property.getDirection() == Direction.DOWN) {
            this.propertiesDown.get(property).put(identifier, nodes.getMultiplier());
        } else if (property.getDirection() == Direction.BOTH) {
            this.propertiesUp.get(property).put(identifier, nodes.getMultiplier()); 
            this.propertiesDown.get(property).put(identifier, nodes.getMultiplier());
        } else {
            this.propertiesNone.get(property).put(identifier, true); 
        }
    }
    
    /**
     * Clears the given property for the given node. If the property is predictive in an upwards direction, it 
     * will also be cleared for all successors of the given node. If the property is predictive in a downwards direction,
     * it will also be cleared for all predecessors of the given node.<br>
     * <br>
     * The worst-case run-time complexity of this operation is O(#nodes for which put has been called with this property).
     * Depending on your access pattern (e.g. sequential in terms of a path from bottom to top), it may be reduced up to 
     * a complexity of amortized O(1). 
     * 
     * @param node
     * @param property
     */
    public void removeProperty(int[] node, PredictiveProperty property) {

        checkProperty(property);
        this.nodes.checkNode(node);
        this.setModified();
        if (property.getDirection() == Direction.UP) {
            this.propertiesUp.get(property).clear(node, ElementComparator.GEQ);
        } else if (property.getDirection() == Direction.DOWN) {
            this.propertiesDown.get(property).clear(node, ElementComparator.LEQ);
        } else if (property.getDirection() == Direction.BOTH) {
            this.propertiesUp.get(property).clear(node, ElementComparator.GEQ);
            this.propertiesDown.get(property).clear(node, ElementComparator.LEQ);
        } else {
            this.propertiesNone.get(property).put(space().toId(node), null);
        }
    }

    /**
     * Clears the given property for the given node. If the property is predictive in an upwards direction, it 
     * will also be cleared for all successors of the given node. If the property is predictive in a downwards direction,
     * it will also be cleared for all predecessors of the given node.<br>
     * <br>
     * The worst-case run-time complexity of this operation is O(#nodes for which put has been called with this property).
     * Depending on your access pattern (e.g. sequential in terms of a path from bottom to top), it may be reduced up to 
     * a complexity of amortized O(1). 
     * 
     * @param node
     * @param property
     */
    public void removeProperty(long identifier, PredictiveProperty property) {

        this.setModified();
        if (property.getDirection() == Direction.UP) {
            this.propertiesUp.get(property).clear(identifier, ElementComparator.GEQ, nodes.getMultiplier());
        } else if (property.getDirection() == Direction.DOWN) {
            this.propertiesDown.get(property).clear(identifier, ElementComparator.LEQ, nodes.getMultiplier());
        } else if (property.getDirection() == Direction.BOTH) {
            this.propertiesUp.get(property).clear(identifier, ElementComparator.GEQ, nodes.getMultiplier());
            this.propertiesDown.get(property).clear(identifier, ElementComparator.LEQ, nodes.getMultiplier());
        } else {
            this.propertiesNone.get(property).put(identifier, null);
        }
    }
    
    /**
     * Returns a class for mapping between spaces
     * @return
     */
    public JHPLSpace<T> space() {
        return space;
    }
    

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Lattice\n");
        if (!propertiesUp.isEmpty()) {
            builder.append("├── Upwards-predictive properties\n");
            toString(builder, propertiesUp);
        }
        if (!propertiesDown.isEmpty()) {
            builder.append("├── Downwards-predictive properties\n");
            toString(builder, propertiesDown);
        }
        if (!propertiesNone.isEmpty()) {
            builder.append("├── Non-predictive properties\n");
            toStringNone(builder, propertiesNone);
        }
        builder.append("├── Master\n");
        builder.append(master.toString("|   └── ", "|       "));
        builder.append("└── Memory: ").append(getByteSize()).append(" [bytes]\n");
        return builder.toString();
    }
    
    /**
     * Allows for accessing methods that may not safe to be used on very large lattices
     * @return
     */
    public JHPLUnsafe unsafe() {
        return this.unsafe;
    }
    
    /**
     * Internal method that checks properties for validity
     * @param property
     */
    private void checkProperty(PredictiveProperty property) {
        
        if (property == null) {
            throw new NullPointerException("Property must not be null");
        }
        
        if (property.getDirection() == Direction.UP) {
            if (!this.propertiesUp.containsKey(property)) {
                this.propertiesUp.put(property, new JHPLTrie(this));
            }
        } else if (property.getDirection() == Direction.DOWN) {
            if (!this.propertiesDown.containsKey(property)) {
                this.propertiesDown.put(property, new JHPLTrie(this));
            }
        } else if (property.getDirection() == Direction.BOTH) {
            if (!this.propertiesUp.containsKey(property)) {
                this.propertiesUp.put(property, new JHPLTrie(this));
            }
            if (!this.propertiesDown.containsKey(property)) {
                this.propertiesDown.put(property, new JHPLTrie(this));
            }
        } else {
            if (!this.propertiesNone.containsKey(property)) {
                this.propertiesNone.put(property, new JHPLMap<Boolean>());
            }
        }
    }

    /**
     * Enumerates all nodes on the given level regardless of whether or not they are stored in the lattice. Note: hasNext() is
     * not implemented. Simply iterate until <code>null</code> is returned.
     * @return
     */
    private Iterator<Long> listAllNodesAsIdentifiersImpl(final int level, final long[] multiplier) {

        // Initialize
        final Stack<Long> identifiers = new Stack<Long>();
        final int[] heights = this.nodes.getHeights();
        final int dimensions = this.nodes.getDimensions();
        final int[] element = new int[dimensions];
        final JHPLStack offsets = new JHPLStack(dimensions);
        final int[] mins = new int[dimensions];
        
        // Determine minimal indices
        // TODO: These may be determined on-demand with more accuracy
        for (int i = 0; i < mins.length; i++) {
            int diff = numLevels() - heights[i];
            mins[i] = level - diff;
            mins[i] = mins[i] < 0 ? 0 : mins[i];
        }
        offsets.push(0);
        identifiers.push(0L);
        element[0] = 0;
        
        // Return
        return new Iterator<Long>() {

            /** Current level*/
            int current = 0;
            
            @Override public boolean hasNext() { throw new UnsupportedOperationException(); }

            @Override
            public Long next() {
                
                // Iterate
                while (true) {
                    
                    // End of node
                    while (offsets.peek() == heights[offsets.size() - 1] || current > level) {
                        int idx = offsets.size() - 1;
                        current -= element[idx];
                        element[idx] = 0;
                        offsets.pop();
                        identifiers.pop();
                        if (offsets.empty()) {
                            return null;
                        }
                    }
                    
                    // Check and increment
                    offsets.inc();
                    
                    // Store
                    int val = offsets.peek() - 1;
                    int idx = offsets.size() - 1;
                    current = current - element[idx] + val;
                    element[idx] = val;
                    
                    // Branch
                    if (offsets.size() < dimensions) {
                        identifiers.push(identifiers.peek() + (val * multiplier[idx]));
                        offsets.push(mins[offsets.size()]); // Inner node
                    } else if (current == level) {
                        return identifiers.peek() + (val * multiplier[idx]); // Leaf node on required level
                    }
                }
            }
            @Override public void remove() { throw new UnsupportedOperationException(); }
        };
    }

    /**
     * Enumerates all nodes regardless of whether or not they are stored in the lattice. Note: hasNext() is
     * not implemented. Simply iterate until <code>null</code> is returned.
     * @return
     */
    private Iterator<Long> listAllNodesAsIdentifiersImpl(final long[] multiplier) {

        // Initialize
        final Stack<Long> identifiers = new Stack<Long>();
        final int[] heights = this.nodes.getHeights();
        final int dimensions = this.nodes.getDimensions();
        final JHPLStack offsets = new JHPLStack(dimensions);
        offsets.push(0);
        identifiers.push(0L);
        
        // Return
        return new Iterator<Long>() {
            
            @Override public boolean hasNext() { throw new UnsupportedOperationException(); }

            @Override
            public Long next() {
                
                // Iterate
                while (true) {
                    
                    // End of node
                    while (offsets.peek() == heights[offsets.size() - 1]) {
                        offsets.pop();
                        identifiers.pop();
                        if (offsets.empty()) {
                            return null;
                        }
                    }
                    
                    // Check and increment
                    offsets.inc();
                    
                    // Store
                    long identifier = identifiers.peek() + (offsets.peek() - 1) * multiplier[offsets.size() - 1];
                    
                    // Branch
                    if (offsets.size() < dimensions) {
                        identifiers.push(identifier);
                        offsets.push(0); // Inner node
                    } else {
                        return identifier; // Leaf node
                    }
                }
            }
            @Override public void remove() { throw new UnsupportedOperationException(); }
        };
    }


    /**
     * Enumerates all nodes regardless of whether or not they are stored in the lattice. Note: hasNext() is
     * not implemented. Simply iterate until <code>null</code> is returned.
     * @return
     */
    private Iterator<int[]> listAllNodesImpl() {

        // Initialize
        final int[] heights = this.nodes.getHeights();
        final int dimensions = this.nodes.getDimensions();
        final int[] element = new int[dimensions];
        final JHPLStack offsets = new JHPLStack(dimensions);
        offsets.push(0);
        element[0] = 0;
        
        // Return
        return new Iterator<int[]>() {
            
            @Override public boolean hasNext() { throw new UnsupportedOperationException(); }

            @Override
            public int[] next() {
                
                // Iterate
                while (true) {
                    
                    // End of node
                    while (offsets.peek() == heights[offsets.size() - 1]) {
                        offsets.pop();
                        if (offsets.empty()) {
                            return null;
                        }
                    }
                    
                    // Check and increment
                    offsets.inc();
                    
                    // Store
                    element[offsets.size() - 1] = offsets.peek() - 1;
                    
                    // Branch
                    if (offsets.size() < dimensions) {
                        offsets.push(0); // Inner node
                    } else {
                        return element; // Leaf node
                    }
                }
            }
            @Override public void remove() { throw new UnsupportedOperationException(); }
        };
    }

    /**
     * Enumerates all nodes on the given level regardless of whether or not they are stored in the lattice. Note: hasNext() is
     * not implemented. Simply iterate until <code>null</code> is returned.
     * @return
     */
    private Iterator<int[]> listAllNodesImpl(final int level) {

        // Initialize
        final int[] heights = this.nodes.getHeights();
        final int dimensions = this.nodes.getDimensions();
        final int[] element = new int[dimensions];
        final JHPLStack offsets = new JHPLStack(dimensions);
        final int[] mins = new int[dimensions];
        
        // Determine minimal indices
        // TODO: These may be determined on-demand with more accuracy
        for (int i = 0; i < mins.length; i++) {
            int diff = numLevels() - heights[i];
            mins[i] = level - diff;
            mins[i] = mins[i] < 0 ? 0 : mins[i];
        }
        offsets.push(0);
        element[0] = 0;
        
        // Return
        return new Iterator<int[]>() {

            /** Current level*/
            int current = 0;
            
            @Override public boolean hasNext() { throw new UnsupportedOperationException(); }

            @Override
            public int[] next() {
                
                // Iterate
                while (true) {
                    
                    // End of node
                    while (offsets.peek() == heights[offsets.size() - 1] || current > level) {
                        int idx = offsets.size() - 1;
                        current -= element[idx];
                        element[idx] = 0;
                        offsets.pop();
                        if (offsets.empty()) {
                            return null;
                        }
                    }
                    
                    // Check and increment
                    offsets.inc();
                    
                    // Store
                    int val = offsets.peek() - 1;
                    int idx = offsets.size() - 1;
                    current = current - element[idx] + val;
                    element[idx] = val;
                    
                    // Branch
                    if (offsets.size() < dimensions) {
                        offsets.push(mins[offsets.size()]); // Inner node
                    } else if (current == level) {
                        return element; // Leaf node on required level
                    }
                }
            }
            @Override public void remove() { throw new UnsupportedOperationException(); }
        };
    }

    
    /**
     * Materializes the whole lattice
     * @param element
     * @param heights
     * @param dimension
     * @param trie
     */
    private void materialize(int[] element, int level, int[] heights, int dimension, JHPLTrie trie) {
        if (dimension == heights.length) {
            trie.put(element);
        } else {
            for (int i=0; i<heights[dimension]; i++) {
                element[dimension] = i;
                materialize(element, level + i, heights, dimension + 1, trie);
            }
        }
    }

    /**
     * To string
     * @param builder
     * @param properties
     */
    private void toString(StringBuilder builder , Map<PredictiveProperty, JHPLTrie> properties) {
        List<PredictiveProperty> list = new ArrayList<PredictiveProperty>();
        list.addAll(properties.keySet());
        for (int i=0; i<list.size()-1; i++) {
            PredictiveProperty property = list.get(i);
            builder.append("|   ├── ").append(property.getLabel()).append("\n");
            builder.append(properties.get(property).toString("|   |   └── ", "|   |       "));
        }
        if (!list.isEmpty()) {
            PredictiveProperty property = list.get(list.size()-1);
            builder.append("|   └── ").append(property.getLabel()).append("\n");
            builder.append(properties.get(property).toString("|       └── ", "|           "));
        }
    }
    
    /**
     * To string
     * @param builder
     * @param properties
     */
    private void toStringNone(StringBuilder builder , Map<PredictiveProperty, JHPLMap<Boolean>> properties) {
        List<PredictiveProperty> list = new ArrayList<PredictiveProperty>();
        list.addAll(properties.keySet());
        for (int i=0; i<list.size()-1; i++) {
            PredictiveProperty property = list.get(i);
            builder.append("|   ├── ").append(property.getLabel()).append("\n");
            builder.append("|   |   └── Not implemented");
        }
    }

    /**
     * For checking for concurrent modifications
     */
    boolean isModified() {
        return this.modified;
    }

    /**
     * Enumerates all nodes regardless of whether or not they are stored in the lattice
     * @return
     */
    Iterator<int[]> listAllNodes() {
        return new WrappedIntArrayIterator(null, this.listAllNodesImpl());
    }

    /**
     * Enumerates all nodes on the given level regardless of whether or not they are stored in the lattice
     * @return
     */
    Iterator<int[]> listAllNodes(int level) {
        return new WrappedIntArrayIterator(null, this.listAllNodesImpl(level));
    }
    
    /**
     * Enumerates all nodes regardless of whether or not they are stored in the lattice
     * @return
     */
    Iterator<Long> listAllNodesAsIdentifiers() {
        return new WrappedLongIterator(null, this.listAllNodesAsIdentifiersImpl(nodes.getMultiplier()));
    }

    /**
     * Enumerates all nodes on the given level regardless of whether or not they are stored in the lattice
     * @return
     */
    Iterator<Long> listAllNodesAsIdentifiers(int level) {
        return new WrappedLongIterator(null, this.listAllNodesAsIdentifiersImpl(level, nodes.getMultiplier()));
    }

    /**
     * Materializes the whole lattice. This method is similar to calling put() for each node returned
     * by enumerateAllNodes(). It is here for your convenience, only. 
     */
    void materialize() {
        int[] element = new int[nodes.getDimensions()];
        int[] heights = nodes.getHeights();
        materialize(element, 0, heights, 0, master);
    }

    /**
     * For checking for concurrent modifications
     */
    void setModified() {
        this.modified = true;
    }
    
    /**
     * For checking for concurrent modifications
     */
    void setUnmodified() {
        this.modified = false;
    }
}