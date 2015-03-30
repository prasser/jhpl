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
public class Test8 {

    /**
     * Main
     * @param args
     */
    public static void main(String[] args) {
        test8();
    }

    /**
     * Test method
     */
    private static void test8() {
        
        System.out.println("");
        System.out.println("**************************");
        System.out.println("* TEST    8              *");
        System.out.println("**************************");
        System.out.println("");


        // Elements per dimension
        String[][] elements = new String[][]{ {"A", "B", "C", "D"},
                                              {"A", "B"},
                                              {"A", "B", "C"}};
        
        // Create lattice with String-keys and Integer-values
        Lattice<String, Integer> lattice = new Lattice<String, Integer>(elements);
        lattice.unsafe().materialize();
        Iterator<int[]> iter = lattice.listNodes();
        int count = 0;
        while (iter.hasNext()) {
            count++;
            System.out.println(Arrays.toString(iter.next()));
        }
        System.out.println("Is: "+ count+" should: " + (4*2*3));
    }    
}
