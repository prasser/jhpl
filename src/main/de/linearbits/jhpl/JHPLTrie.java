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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * This class implements a simple trie for integers that is materialized in a backing integer array
 * @author Fabian Prasser
 */
class JHPLTrie {

    /** 
     * Comparator
     * 
     * @author Fabian Prasser
     */
    static enum ElementComparator {
        /** Equals */
        EQ, 
        /** Greater than or equals */
        GEQ,
        /** Less than or equals */
        LEQ 
    }
    
    /** Constant*/
    private static final double COMPACTION_THRESHOLD = 0.2d;

    /** The buffer */
    private final JHPLBuffer    buffer;
    /** The number of dimensions */
    private final int           dimensions;
    /** The height of each dimension */
    private final int[]         heights;
    /** The Lattice */
    private final Lattice<?, ?> lattice;
    /** The number of levels */
    private final int           levels;
    /** The number of used memory units */
    private int                 used;

    /**
     * Constructs a new trie
     * @param lattice
     */
    JHPLTrie(Lattice<?, ?> lattice) {
        
        // Initialize. Root node will be at offset 0
        this.dimensions = lattice.nodes().getDimensions();
        this.heights = lattice.nodes().getHeights();
        this.buffer = new JHPLBuffer();
        this.buffer.allocate(heights[0]);
        this.used = heights[0];
        this.lattice = lattice;
        int sum = 0;
        for (int i = 0; i < this.heights.length; i++) {
            sum += this.heights[i] - 1;
        }
        this.levels = sum + 1;
    }
    
    /**
     * Clears all elements from this trie for the given element
     * @param element
     * @param dimension
     * @param offset
     * @param comparator
     * @return Whether some elements are still referenced by this node
     */
    private boolean clear(int[] element, int dimension, int offset, ElementComparator comparator) {
        
        // Init
        int elementOffset = offset + element[dimension];
        
        // Terminate
        if (dimension == dimensions - 1){
            
            switch (comparator) {
            case EQ:
                buffer.memory[elementOffset] = JHPLBuffer.FLAG_NOT_AVAILABLE;
                break;
            case GEQ:
                for (int i = 0; i < heights[dimension] - element[dimension]; i++) {
                    buffer.memory[elementOffset + i] = JHPLBuffer.FLAG_NOT_AVAILABLE; 
                }
                break;
            case LEQ:
                for (int i = 0; i <= element[dimension]; i++) {
                    buffer.memory[elementOffset - i] = JHPLBuffer.FLAG_NOT_AVAILABLE; 
                }
                break;
            }
        
        // Recursion
        } else {
            switch (comparator) {
            case EQ:
                int pointer = buffer.memory[elementOffset];
                if (pointer != JHPLBuffer.FLAG_NOT_AVAILABLE) {
                    if (!clear(element, dimension + 1, pointer, comparator)) {
                        buffer.memory[elementOffset] = JHPLBuffer.FLAG_NOT_AVAILABLE;
                        used -= heights[dimension + 1];
                    }
                }
                break;
            case GEQ:
                for (int i = 0; i < heights[dimension] - element[dimension]; i++) {
                    pointer = buffer.memory[elementOffset + i];
                    if (pointer != JHPLBuffer.FLAG_NOT_AVAILABLE) {
                        if (!clear(element, dimension + 1, pointer, comparator)) {
                            buffer.memory[elementOffset + i] = JHPLBuffer.FLAG_NOT_AVAILABLE;
                            used -= heights[dimension + 1];
                        }
                    }
                }
                break;
            case LEQ:
                for (int i = 0; i <= element[dimension]; i++) {
                    pointer = buffer.memory[elementOffset - i];
                    if (pointer != JHPLBuffer.FLAG_NOT_AVAILABLE) {
                        if (!clear(element, dimension + 1, pointer, comparator)) {
                            buffer.memory[elementOffset - i] = JHPLBuffer.FLAG_NOT_AVAILABLE;
                            used -= heights[dimension + 1];
                        }
                    }
                }
                break;
            }
        }
        
        // Return
        for (int i = offset; i < offset + heights[dimension]; i++) {
            if (buffer.memory[i] != JHPLBuffer.FLAG_NOT_AVAILABLE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compaction method on the trie
     */
    private void compactify() {
        Iterator<int[]> iterator = this.iterator();
        JHPLTrie other = new JHPLTrie(this.lattice);
        int[] element = iterator.next();
        while (element != null) {
            other.put(element);
            element = iterator.next();
        }
        this.buffer.replace(other.buffer);
    }
    
    /**
     * Queries this trie for the given element
     * @param element
     * @param dimension
     * @param offset
     * @param comparator
     */
    private boolean contains(int[] element, int dimension, int offset, ElementComparator comparator) {
        
        // Init
        offset += element[dimension];
        
        // Find
        int pointer = JHPLBuffer.FLAG_NOT_AVAILABLE;
        switch (comparator) {
        case EQ:
            pointer = buffer.memory[offset];
            
            // Terminate
            if (pointer == JHPLBuffer.FLAG_NOT_AVAILABLE) {
                return false;

            // Terminate
            } else if (dimension == dimensions - 1){
                return true;
                
            // Recursion
            } else {
                return contains(element, dimension + 1, pointer, comparator);
            }
            
        case GEQ:
            
            // Foreach
            for (int i = 0; i < heights[dimension] - element[dimension]; i++) {
                pointer = buffer.memory[offset + i];
                
                // Terminate
                if (pointer == JHPLBuffer.FLAG_NOT_AVAILABLE) {
                    continue;

                // Terminate
                } else if (dimension == dimensions - 1){
                    return true;
                    
                // Recursion
                } else if (contains(element, dimension + 1, pointer, comparator)) {
                    return true;
                }
            }
            return false;
        case LEQ:
            
            // Foreach
            for (int i = 0; i <= element[dimension]; i++) {
                pointer = buffer.memory[offset - i];

                // Terminate
                if (pointer == JHPLBuffer.FLAG_NOT_AVAILABLE) {
                    continue;

                // Terminate
                } else if (dimension == dimensions - 1){
                    return true;
                    
                // Recursion
                } else if (contains(element, dimension + 1, pointer, comparator)) {
                    return true;
                }
            }
            return false;
        default:
            throw new IllegalStateException("Unknown comparator");
        }
    }

    /**
     * Helper for putting an element into this trie
     * @param element
     * @param dimension
     * @param offset
     */
    private void put(int[] element, int dimension, int offset) {
       
        offset += element[dimension];
        
        if (dimension == dimensions - 1) {
            buffer.memory[offset] = JHPLBuffer.FLAG_AVAILABLE;
            return;
        } 
        
        if (buffer.memory[offset] == JHPLBuffer.FLAG_NOT_AVAILABLE){
            int pointer = buffer.allocate(heights[dimension + 1]);
            used += heights[dimension + 1];
            buffer.memory[offset] = pointer;
        }
        
        put(element, dimension + 1, buffer.memory[offset]);
    }

    /**
     * Helper for converting the trie to a string
     * @param prefix
     * @param isTail
     * @param offset
     * @param dimension
     * @return
     */
    private StringBuilder toString(String prefix, boolean isTail, int offset, int dimension) {
        StringBuilder builder = new StringBuilder();
        List<Integer> children = new ArrayList<Integer>();
        for (int i = offset; i<offset + heights[dimension]; i++) {
            if (buffer.memory[i] != JHPLBuffer.FLAG_NOT_AVAILABLE) {
                children.add(i);
            }
        }
        for (int j = 0; j < children.size() - 1; j++) {
            int i = children.get(j);
            builder.append(prefix).append(isTail ? "└── " : "├── ").append("[").append(i - offset).append("]\n");
            if (dimension != dimensions - 1) {
                builder.append(toString(prefix + (isTail ? "    " : "│   "), false, buffer.memory[i], dimension + 1));
            }
        }
        if (children.size() > 0) {
            int i = children.get(children.size() - 1);
            builder.append(prefix).append(isTail ? "└── " : "├── ").append("[").append(i - offset).append("]\n");
            if (dimension != dimensions - 1) {
                builder.append(toString(prefix + (isTail ? "    " : "│   "), true, buffer.memory[i], dimension + 1));
            }
        }
        return builder;
    }

    /**
     * Clears all entries from this trie for the given element
     * @param node
     * @param comparator
     * @return
     */
    void clear(int[] node, ElementComparator comparator) {
        
        // Clear
        clear(node, 0, 0, comparator);
        
        // Compaction
        double utilization = (double)used / (double)buffer.memory.length;
        if (utilization < COMPACTION_THRESHOLD) {
            compactify();
        }
    }

    /**
     * Queries this trie for the given element
     * @param node
     * @return
     */
    boolean contains(int[] node) {
        return contains(node, ElementComparator.EQ);
    }


    /**
     * Queries this trie for the given element
     * @param node
     * @param comparator
     * @return
     */
    boolean contains(int[] node, ElementComparator comparator) {
        return contains(node, 0, 0, comparator);
    }
    
    /**
     * Returns the memory consumption in bytes
     * @return
     */
    long getByteSize() {
        return this.buffer.memory.length * 4;
    }
    
    /**
     * Returns the number of levels
     * @return
     */
    int getLevels() {
        return this.levels;
    }


    /**
     * Returns an iterator over all elements in the trie. Note: hasNext() is not implemented. Simply iterate until
     * <code>null</code> is returned.
     * @return
     */
    Iterator<int[]> iterator() {
        
        // Initialize
        final int[] element = new int[this.dimensions];
        final JHPLStack offsets = new JHPLStack(this.dimensions);
        final JHPLStack pointers = new JHPLStack(this.dimensions);
        offsets.push(0);
        pointers.push(0);
        element[0] = 0;
        
        // Return
        return new Iterator<int[]>() {
            
            @Override public boolean hasNext() { throw new UnsupportedOperationException(); }

            @Override
            public int[] next() {
                
                // Iteratively traverse the trie
                while (true) {
                    
                    // End of node
                    while (offsets.peek() == heights[offsets.size() - 1]) {
                        offsets.pop();
                        pointers.pop();
                        if (offsets.empty()) {
                            return null;
                        }
                    }
                    
                    // Check and increment
                    int mem = buffer.memory[pointers.peek() + offsets.peek()];
                    offsets.inc();
                
                    // If available
                    if (mem != JHPLBuffer.FLAG_NOT_AVAILABLE) {
                        
                        element[offsets.size() - 1] = offsets.peek() - 1;
                        if (offsets.size() < dimensions) {
                            // Inner node
                            offsets.push(0);
                            pointers.push(mem);
                        } else {
                            // Leaf node
                            return element;
                        }
                    }
                }
            }
            @Override public void remove() { throw new UnsupportedOperationException(); }
        };
    }

    /**
     * Returns an iterator over all elements on the given level stored in the trie. 
     * Note: hasNext() is not implemented. Simply iterate until <code>null</code> is returned.
     * @param level
     * @return
     */
    Iterator<int[]> iterator(final int level) {
        
        // Initialize
        final int[] element = new int[this.dimensions];
        final JHPLStack offsets = new JHPLStack(this.dimensions);
        final JHPLStack pointers = new JHPLStack(this.dimensions);
        final int[] mins = new int[this.dimensions];
        offsets.push(0);
        pointers.push(0);
        element[0] = 0;
        
        // Determine minimal indices
        for (int i = 0; i < mins.length; i++) {
            int diff = levels - heights[i];
            mins[i] = level - diff;
            mins[i] = mins[i] < 0 ? 0 : mins[i];
        }
        
        // Return
        return new Iterator<int[]>() {
            
            /** Current level*/
            int current = 0;
            
            @Override public boolean hasNext() { throw new UnsupportedOperationException(); }

            @Override
            public int[] next() {
                
                // Iteratively traverse the trie
                while (true) {
                    
                    // (1) End of node, or  
                    // (2) already on a higher level as requested
                    while (offsets.peek() == heights[offsets.size() - 1]  || current > level) {
                        int idx = offsets.size() - 1;
                        current -= element[idx];
                        element[idx] = 0;
                        offsets.pop();
                        pointers.pop();
                        if (offsets.empty()) {
                            return null;
                        }
                    }
                    
                    // Check and increment
                    int mem = buffer.memory[pointers.peek() + offsets.peek()];
                    offsets.inc();
                    
                    // Available
                    if (mem != JHPLBuffer.FLAG_NOT_AVAILABLE) {
                        int val = offsets.peek() - 1;
                        int idx = offsets.size() - 1;
                        current = current - element[idx] + val;
                        element[idx] = val;

                        // Inner node
                        if (offsets.size() < dimensions) {
                            
                            // Initialize with minimal level
                            int min = mins[offsets.size()];
                            offsets.push(min);
                            pointers.push(mem + min);
                            
                        // Leaf node on the requested level
                        } else if (current == level) {
                            return element; 
                        }
                    }
                }
            }
            @Override public void remove() { throw new UnsupportedOperationException(); }
        };
    }
    
    /**
     * Puts an element into this trie
     * @param element
     */
    void put(int[] element) {
        put(element, 0, 0);
    }

    /**
     * To string method
     * @param prefix
     * @return
     */
    String toString(String prefix1, String prefix2) {
        
        int allocated = buffer.memory.length * 4;
        int used = this.used * 4;
        double relative = (double)used / (double)allocated * 100d;
        DecimalFormat format = new DecimalFormat("##0.00000");
        
        StringBuilder builder = new StringBuilder();
        builder.append(prefix1).append("Trie\n");
        builder.append(prefix2).append("├── Memory statistics\n");
        builder.append(prefix2).append("|   ├── Allocated: ").append(allocated).append(" [bytes]\n");
        builder.append(prefix2).append("|   ├── Used: ").append(used).append(" [bytes]\n");
        builder.append(prefix2).append("|   └── Relative: ").append(format.format(relative)).append(" [%]\n");
        builder.append(prefix2).append("├── Buffer\n");
        builder.append(prefix2).append("|   └── ").append(Arrays.toString(buffer.memory)).append("\n");
        builder.append(prefix2).append("└── Tree\n");
        builder.append(toString(prefix2 + "    ", false, 0, 0));
        builder.append(prefix2).append("    └── [EOT]\n");
        return builder.toString();
    }
}