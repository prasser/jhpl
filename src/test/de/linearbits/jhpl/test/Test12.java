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
import de.linearbits.jhpl.PredictiveProperty;
import de.linearbits.jhpl.PredictiveProperty.Direction;

/**
 * Test class
 * @author Fabian Prasser
 *
 */
public class Test12 {

    /**
     * Main
     * @param args
     */
    public static void main(String[] args) {
        test12();
    }

    /**
     * Enumerates all elements from the iterator
     * @param iter
     * @return the number of elements enumerated
     */
    private static int enumerate(Iterator<Long> iter) {

        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        return count;
    }

    /**
     * Formats a given byte size
     * @param bytes
     * @param si
     * @return
     */
    private static String formatByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
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
     * Test method
     */
    private static void test12() {
        
        System.out.println("");
        System.out.println("**************************");
        System.out.println("* TEST    12             *");
        System.out.println("**************************");
        System.out.println("");
        System.out.println("Creating lattice with 1M elements");
        System.out.println("");

        // Create lattice
        Lattice<String, Integer> lattice = getLattice(6);
        
        PredictiveProperty property1 = new PredictiveProperty(Direction.UP);
        PredictiveProperty property2 = new PredictiveProperty(Direction.UP);
        PredictiveProperty property3 = new PredictiveProperty(Direction.DOWN);
        PredictiveProperty property4 = new PredictiveProperty(Direction.DOWN);
        PredictiveProperty property5 = new PredictiveProperty(Direction.BOTH);
        
        System.out.println("Randomly tagging 5 properties 10000 times each");
        long time = System.currentTimeMillis();
        for (int i=0; i<10000; i++) {
            long id = (long)(Math.random() * (lattice.numNodes() - 1));
            lattice.putProperty(lattice.space().toIndex(id), property1);
        }
        for (int i=0; i<10000; i++) {
            long id = (long)(Math.random() * (lattice.numNodes() - 1));
            lattice.putProperty(lattice.space().toIndex(id), property2);
        }
        for (int i=0; i<10000; i++) {
            long id = (long)(Math.random() * (lattice.numNodes() - 1));
            lattice.putProperty(lattice.space().toIndex(id), property3);
        }
        for (int i=0; i<10000; i++) {
            long id = (long)(Math.random() * (lattice.numNodes() - 1));
            lattice.putProperty(lattice.space().toIndex(id), property4);
        }
        for (int i=0; i<10000; i++) {
            long id = (long)(Math.random() * (lattice.numNodes() - 1));
            lattice.putProperty(lattice.space().toIndex(id), property5);
        }
        time = System.currentTimeMillis() - time;
        System.out.println(" - Time needed: "+ time);
        System.out.println(" - Space required: " + formatByteCount(lattice.getByteSize(), true));
        
        
        System.out.println("");
        System.out.println("Listing all nodes on each level for each property with space mapping");
        time = System.currentTimeMillis();
        int total = 0;
        for (int level = 0; level < lattice.numLevels(); level++) {
            total += enumerate(lattice.space().indexIteratorToIdIterator(lattice.unsafe().listNodesWithProperty(property1, level)));
            total += enumerate(lattice.space().indexIteratorToIdIterator(lattice.unsafe().listNodesWithProperty(property2, level)));
            total += enumerate(lattice.space().indexIteratorToIdIterator(lattice.unsafe().listNodesWithProperty(property3, level)));
            total += enumerate(lattice.space().indexIteratorToIdIterator(lattice.unsafe().listNodesWithProperty(property4, level)));
            total += enumerate(lattice.space().indexIteratorToIdIterator(lattice.unsafe().listNodesWithProperty(property5, level)));
        }
        time = System.currentTimeMillis() - time;
        System.out.println(" - Nodes enumerated: "+ total);
        System.out.println(" - Time needed: "+ time);

        System.out.println("");
        System.out.println("Listing all nodes on each level with a property with space mapping");
        time = System.currentTimeMillis();
        total = 0;
        for (int level = 0; level < lattice.numLevels(); level++) {
            total += enumerate(lattice.space().indexIteratorToIdIterator(lattice.unsafe().listNodesWithProperty(level)));
        }
        time = System.currentTimeMillis() - time;
        System.out.println(" - Nodes enumerated: "+ total);
        System.out.println(" - Time needed: "+ time);

        System.out.println("");
        System.out.println("Listing all nodes on each level without a property with space mapping");
        time = System.currentTimeMillis();
        total = 0;
        for (int level = 0; level < lattice.numLevels(); level++) {
            total += enumerate(lattice.space().indexIteratorToIdIterator(lattice.unsafe().listNodesWithoutProperty(level)));
        }
        time = System.currentTimeMillis() - time;
        System.out.println(" - Nodes enumerated: "+ total);
        System.out.println(" - Time needed: "+ time);

        System.out.println("");
        System.out.println("Listing all nodes without any property");
        time = System.currentTimeMillis();
        total = enumerate(lattice.space().indexIteratorToIdIterator(lattice.unsafe().listNodesWithoutProperty()));
        time = System.currentTimeMillis() - time;
        System.out.println(" - Nodes enumerated: "+ total);
        System.out.println(" - Time needed: "+ time);
    }
        
}
