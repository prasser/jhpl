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

import java.util.Arrays;

/**
 * A simple stack
 * @author Fabian Prasser
 */
class JHPLStack {

    /** Buffer*/
    private final int[] buffer;
    /** Pointer*/
    private int size = 0;
    
    /**
     * Creates a new instance
     * @param size
     */
    JHPLStack(int size) {
        buffer = new int[size];
    }
    
    @Override
    public String toString() {
        return Arrays.toString(buffer) + " (" + size + ")";
    }
    
    /**
     * Returns whether this stack is empty
     * @return
     */
    boolean empty() {
        return size == 0;
    }

    /**
     * Increment
     * @return
     */
    void inc() {
        buffer[size - 1]++;
    }

    /**
     * Peek
     * @return
     */
    int peek() {
        return buffer[size - 1];
    }
    /**
     * Pop
     * @return
     */
    int pop() {
        int val = buffer[--size];
        return val;
    }

    /**
     * Push
     * @param element
     */
    void push(int element) {
        buffer[size++] = element;
    }
    
    /**
     * Returns the size
     * @return
     */
    int size() {
        return size;
    }
}