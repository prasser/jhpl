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
public class Test1 {
    
    /**
     * Main
     * @param args
     */
    public static void main(String[] args) {
        test1();
    }

    /**
     * Prints the nodes from the given iterator
     * @param lattice
     * @param iter
     */
    private static void printIterator(Lattice<String, Integer> lattice, Iterator<int[]> iter) {
        int count = 0;
        while (iter.hasNext()) {
            count++;
            System.out.print(Arrays.toString(lattice.space().toSource(iter.next())));
            System.out.print(" ");
        }
        System.out.println("(Total: " + count + ")");
    }
    

    /**
     * Prints basic information about a node
     * @param lattice
     * @param node
     */
    private static void printNode(Lattice<String, Integer> lattice, int[] node) {
        
        System.out.println("Node");
        System.out.println(" - Indices     : " + Arrays.toString(node));
        System.out.println(" - Values      : " + Arrays.toString(lattice.space().toSource(node)));
        System.out.print  (" - Successors  : ");
        printIterator(lattice, lattice.nodes().listSuccessors(node));
        System.out.print  (" - Predecessors: ");
        printIterator(lattice, lattice.nodes().listPredecessors(node));
        
    }

    /**
     * Test method
     */
    private static void test1() {
        
        System.out.println("");
        System.out.println("**************************");
        System.out.println("* TEST    1              *");
        System.out.println("**************************");
        System.out.println("");
        
        // Elements per dimension
        String[][] elements = new String[][]{ {"A", "B", "C", "D"},
                                              {"A", "B"},
                                              {"A", "B", "C"}};
        
        // Create lattice with String-keys and Integer-values
        Lattice<String, Integer> lattice = new Lattice<String, Integer>(elements);
        
        // Use static access to top and bottom
        printNode(lattice, lattice.nodes().getTop());
        printNode(lattice, lattice.nodes().getBottom());
        
        // Use builder pattern
        printNode(lattice, lattice.nodes().build().next("B").next("B").next("C").create());
        printNode(lattice, lattice.nodes().build().next("B").next("A").next("B").create());
    }    
}
