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
public class Test5 {

    /**
     * Main
     * @param args
     */
    public static void main(String[] args) {
        test5();
    }

    /**
     * Returns a lattice of size 10^dimensions
     * @param dimensions
     * @return
     */
    private static Lattice<String, Integer> getLattice(int dimensions) {
        String[][] elements = new String[dimensions][];
        for (int i=0; i<dimensions; i++) {
            elements[i] = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        }
        return new Lattice<String, Integer>(elements);
    }

    /**
     * Returns the naive size of a lattice implementation
     * @param lattice
     * @return
     */
    private static long getNaiveSize(Lattice<String, Integer> lattice) {
        Iterator<int[]> iter = lattice.listNodes();
        long size = 0;
        while (iter.hasNext()) {
            int[] node = iter.next();
            size += 24; // Base class
            size += 24 + lattice.numDimensions() * 4; // Actual element
            
            int predecessors = 0;
            Iterator<int[]> iter2 = lattice.nodes().listPredecessors(node);
            while (iter2.hasNext()) {
                iter2.next();
                predecessors++;
            }
            size += 24 + predecessors * 8; // Pointers to predecessors
            
            int successors = 0;
            Iterator<int[]> iter3 = lattice.nodes().listSuccessors(node);
            while (iter3.hasNext()) {
                iter3.next();
                successors++;
            }
            size += 24 + successors * 8; // Pointers to successors
        }
        return size;
    }

    /**
     * Test method
     */
    private static void test5() {
        
        System.out.println("");
        System.out.println("**************************");
        System.out.println("* TEST    5              *");
        System.out.println("**************************");
        System.out.println("");
        
        for (int i = 1; i <= 7; i++) {
            Lattice<String, Integer> lattice = getLattice(i);
            long time = System.currentTimeMillis();
            lattice.unsafe().materialize();
            time = System.currentTimeMillis() - time;
            System.out.print("Lattice with ");
            System.out.print(Math.pow(10, i));
            System.out.print(" nodes has size: ");
            System.out.print(Util.formatByteCount(lattice.getByteSize(), true));
            System.out.print(" (naive: ");
            System.out.print(Util.formatByteCount(getNaiveSize(lattice), true));
            System.out.print(")");
            System.out.print(" and was materialized in ");
            System.out.print(time);
            System.out.println(" [ms]");
        }
    }    
}
