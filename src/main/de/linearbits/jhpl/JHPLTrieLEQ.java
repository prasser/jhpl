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
        super(lattice, false);
    }

    @Override
    boolean clear(int[] element, int dimension, int offset) {

        // Init
        int elementOffset = offset + element[dimension];

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
                        used -= heights[dimension + 1];
                    }
                }
            }
        }

        // Return
        for (int i = offset; i < offset + heights[dimension]; i++) {
            if (buffer.memory[i] != JHPLBuffer.FLAG_NOT_AVAILABLE) { return true; }
        }
        return false;
    }
     
    @Override
    boolean contains(int[] element, int dimension, int offset) {

        if (dimension == dimensions) {
            return true;          
        } else {
            for (int i = element[dimension]; i >= 0; i--) {
                int pointer = buffer.memory[offset + i];
                if (pointer != JHPLBuffer.FLAG_NOT_AVAILABLE && contains(element, dimension + 1, pointer)) { 
                    return true; 
                }
            }
        }
        return false;
    }
    
    @Override
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
        return new JHPLTrieLEQ(this.lattice);
    }
}