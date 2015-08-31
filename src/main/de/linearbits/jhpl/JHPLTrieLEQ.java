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
class JHPLTrieLEQ extends JHPLTrie {

    /**
     * Constructs a new trie
     * @param lattice
     */
    JHPLTrieLEQ(Lattice<?, ?> lattice) {
        super(lattice, true, Integer.MAX_VALUE);
    }
     
    /**
     * Queries this trie for the given element
     * 
     * @param element
     * @param dimension
     * @param offset
     */
    private boolean containsEQ(int[] element) {
        
        // Init
        int offset = 0;
        
        // Foreach
        for (int dimension = 0; dimension < element.length; dimension++) {
    
            // Increment
            offset += element[dimension] + 1;
    
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
    
    boolean _contains(int[] element, int level, int dimension, int offset) {
        
        if (dimension == dimensions) {
            return true;          
        } else {
            
             // Check level
            if (dimension < dimensions - 1 && buffer.memory[offset] >= level) {
                return false;
            }
            
            for (int i = element[dimension] + 1; i >= 1; i--) {
                int pointer = buffer.memory[offset + i];
                if (pointer != JHPLBuffer.FLAG_NOT_AVAILABLE && _contains(element, level, dimension + 1, pointer)) { 
                    return true; 
                }
            }
        }
        return false;
    }
    
    @Override
    boolean clear(int[] element, int dimension, int offset) {

        // Init
        int elementOffset = offset + element[dimension] + 1;

        // Terminate
        if (dimension == dimensions - 1) {
            for (int i = 0; i < heights[dimension] - element[dimension]; i++) {
                buffer.memory[elementOffset + i] = JHPLBuffer.FLAG_NOT_AVAILABLE;
            }
            // Recursion
        } else {
            for (int i = 0; i < heights[dimension] - element[dimension]; i++) {
                int pointer = buffer.memory[elementOffset + i];
                if (pointer != JHPLBuffer.FLAG_NOT_AVAILABLE) {
                    if (!clear(element, dimension + 1, pointer)) {
                        buffer.memory[elementOffset + i] = JHPLBuffer.FLAG_NOT_AVAILABLE;
                        used -= heights[dimension + 1] + 1;
                    }
                }
            }
        }

        // Return
        for (int i = offset + 1; i < offset + heights[dimension] + 1; i++) {
            if (buffer.memory[i] != JHPLBuffer.FLAG_NOT_AVAILABLE) { return true; }
        }
        return false;
    }

    @Override
    boolean contains(int[] element, int level, int dimension, int offset) {
        
        // We need to check this, to allow for pruning with min(level)>=level instead of min(level)>level only.
        if (level != bound && containsEQ(element)) {
            return true;
        }
        
        // Now, check
        return _contains(element, level, dimension, offset);
    }
    
    @Override
    JHPLTrie newInstance() {
        return new JHPLTrieLEQ(this.lattice);
    }

    @Override
    void put(int[] element, int level, int dimension, int offset) {
       
        int base = offset;
        offset += element[dimension] + 1;
        
        if (dimension == dimensions - 1) {
            buffer.memory[offset] = JHPLBuffer.FLAG_AVAILABLE;
            // TODO: On the last page, we always leave the min-level at its initial value
            return;
        } 
        
        if (buffer.memory[offset] == JHPLBuffer.FLAG_NOT_AVAILABLE){
            int pointer = buffer.allocate(heights[dimension + 1] + 1);
            used += heights[dimension + 1] + 1;
            buffer.memory[offset] = pointer;
            buffer.memory[pointer] = bound - 1;
        } 
        buffer.memory[base] = Math.min(level, buffer.memory[base]);
        put(element, level, dimension + 1, buffer.memory[offset]);
    }
}