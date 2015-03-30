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
package de.linearbits.jhpl.test;

import java.util.Arrays;
import java.util.Iterator;

import de.linearbits.jhpl.Lattice;

/**
 * Test class
 * @author Fabian Prasser
 *
 */
public class Test11 {

    /**
     * Main
     * @param args
     */
    public static void main(String[] args) {
        test11();
    }

    /**
     * Test method
     */
    private static void test11() {
        
        System.out.println("");
        System.out.println("**************************");
        System.out.println("* TEST    11             *");
        System.out.println("**************************");
        System.out.println("");
        System.out.println("Testing space mappings");
        System.out.println("");

        // Elements per dimension
        String[][] elements = new String[][]{ {"A", "B", "C"},
                                              {"A", "B"},
                                              {"A", "B", "C", "D"}};
        
        // Create lattice with String-keys and Integer-values
        Lattice<String, Integer> lattice = new Lattice<String, Integer>(elements);
        
        Iterator<int[]> iter = lattice.unsafe().listAllNodes();
        while (iter.hasNext()) {
            int[] node = iter.next();
            System.out.println("Node: " + Arrays.toString(node));
            System.out.println(" - source: " + Arrays.toString(lattice.space().toSource(node)) + " / " + Arrays.toString(lattice.space().toIndex(lattice.space().toSource(node))));
            System.out.println(" - id    : " + lattice.space().toId(node) + " / " + Arrays.toString(lattice.space().toIndex(lattice.space().toId(node))));
        }
    }    
}
