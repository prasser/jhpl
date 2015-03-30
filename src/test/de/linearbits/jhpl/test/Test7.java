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
public class Test7 {

    /**
     * Main
     * @param args
     */
    public static void main(String[] args) {
        test7();
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
    private static void test7() {
        
        System.out.println("");
        System.out.println("**************************");
        System.out.println("* TEST    7              *");
        System.out.println("**************************");
        System.out.println("");
        System.out.println("Setting a property for all nodes in a lattice with 1 million elements");
        System.out.println("");
        
        PredictiveProperty property1 = new PredictiveProperty(Direction.UP);
        PredictiveProperty property2 = new PredictiveProperty(Direction.DOWN);
        
        long time = System.currentTimeMillis();
        Lattice<String, Integer> lattice = getLattice(6);
        lattice.unsafe().materialize();
        System.out.println(" - Materializing in " + (System.currentTimeMillis() - time) + " [ms]");
        System.out.println(" - Current size: " + formatByteCount(lattice.getByteSize(), true));
        
        time = System.currentTimeMillis();
        Iterator<int[]> iter = lattice.unsafe().listAllNodes();
        while (iter.hasNext()) {
            lattice.putProperty(iter.next(), property1);
        }
        System.out.print(" - Best case in " + (System.currentTimeMillis() - time) + " [ms]");
        System.out.println(" - Current size: " + formatByteCount(lattice.getByteSize(), true));

        time = System.currentTimeMillis();
        iter = lattice.unsafe().listAllNodes();
        while (iter.hasNext()) {
            lattice.putProperty(iter.next(), property2);
        }
        System.out.print(" - Worst case in " + (System.currentTimeMillis() - time) + " [ms]");
        System.out.println(" - Current size: " + formatByteCount(lattice.getByteSize(), true));
    }    
}
