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


/**
 * This class implements a simple trie for integers that is materialized in a backing integer array
 * @author Fabian Prasser
 */
class JHPLTrieEQ extends JHPLTrie{

    /**
     * Constructs a new trie
     * @param lattice
     */
    JHPLTrieEQ(Lattice<?, ?> lattice) {
        super(lattice, false);
    }

    /**
     * Clears all elements from this trie for the given element
     * @param element
     * @param dimension
     * @param offset
     * @return Whether some elements are still referenced by this node
     */
    boolean clear(int[] element, int dimension, int offset) {

        // Init
        int elementOffset = offset + element[dimension];

        // Terminate
        if (dimension == dimensions - 1) {
            buffer.memory[elementOffset] = JHPLBuffer.FLAG_NOT_AVAILABLE;
            // Recursion
        } else {
            int pointer = buffer.memory[elementOffset];
            if (pointer != JHPLBuffer.FLAG_NOT_AVAILABLE) {
                if (!clear(element, dimension + 1, pointer)) {
                    buffer.memory[elementOffset] = JHPLBuffer.FLAG_NOT_AVAILABLE;
                    used -= heights[dimension + 1];
                }
            }
        }

        // Return
        for (int i = offset; i < offset + heights[dimension]; i++) {
            if (buffer.memory[i] != JHPLBuffer.FLAG_NOT_AVAILABLE) { return true; }
        }
        return false;
    }

    /**
     * Queries this trie for the given element
     * 
     * @param element
     * @param dimension
     * @param offset
     */
    boolean contains(int[] element, int dimension, int offset) {
        
        // Init
        offset = 0;
        
        // Foreach
        for (dimension = 0; dimension < element.length; dimension++) {
    
            // Increment
            offset += element[dimension];
    
            // Find
            int pointer = buffer.memory[offset];
    
            // Terminate
            if (pointer == JHPLBuffer.FLAG_NOT_AVAILABLE) {
                return false;
                
            // Next
            } else {
                offset = pointer;
            }
        }
        
        // Terminate
        return true;
    }
    
    /**
     * Helper for putting an element into this trie
     * @param element
     * @param dimension
     * @param offset
     */
    void put(int[] element, int dimension, int offset) {
       
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

    @Override
    JHPLTrie newInstance() {
        return new JHPLTrieEQ(this.lattice);
    }
}