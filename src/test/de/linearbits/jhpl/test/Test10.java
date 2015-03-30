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

import java.util.Iterator;

import de.linearbits.jhpl.Lattice;

/**
 * Test class
 * @author Fabian Prasser
 *
 */
public class Test10 {

    /**
     * Main
     * @param args
     */
    public static void main(String[] args) {
        test10();
    }
    
    /**
     * Enumerates all elements from the iterator and prints some statistics
     * @param iter
     * @return the number of elements enumerated
     */
    private static int enumerate(Iterator<int[]> iter) {

        long time = System.currentTimeMillis();
        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        time = System.currentTimeMillis() - time;

        System.out.print("Enumerated ");
        System.out.print(count);
        System.out.print(" elements in ");
        System.out.print(time);
        System.out.println(" [ms]");
        
        return count;
    }
        
    /**
     * Test method
     */
    private static void test10() {
        
        System.out.println("");
        System.out.println("**************************");
        System.out.println("* TEST    10             *");
        System.out.println("**************************");
        System.out.println("");
        System.out.println("Enumerating nodes in a lattice level-by-level");
        System.out.println("");

        // Elements per dimension
        String[][] elements = new String[][]{ {"A", "B", "C", "D"},
                                              {"A", "B"},
                                              {"A", "B", "C"}};
        
        // Create lattice with String-keys and Integer-values
        Lattice<String, Integer> lattice = new Lattice<String, Integer>(elements);
        lattice.unsafe().materialize();

        long time = System.currentTimeMillis();
        int total = 0;
        for (int level = 0; level < lattice.numLevels(); level++) {
            System.out.print("Enumerating nodes on level ");
            System.out.print(level);
            System.out.print(": ");
            total += enumerate(lattice.listNodes(level));
        }
        System.out.println("");
        System.out.println("Total number of nodes: " + total);
        System.out.println("Total time: " + (System.currentTimeMillis() - time) + " [ms]");
    }    
}
