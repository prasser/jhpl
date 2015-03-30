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

import de.linearbits.jhpl.Lattice;
import de.linearbits.jhpl.PredictiveProperty;
import de.linearbits.jhpl.PredictiveProperty.Direction;

/**
 * Test class
 * @author Fabian Prasser
 *
 */
public class Test2 {

    /**
     * Main
     * @param args
     */
    public static void main(String[] args) {
        test2();
    }

    /**
     * Test method
     */
    private static void test2() {

        System.out.println("");
        System.out.println("**************************");
        System.out.println("* TEST    2              *");
        System.out.println("**************************");
        System.out.println("");
        
        // Elements per dimension
        String[][] elements = new String[][]{ {"A", "B", "C", "D"},
                                              {"A", "B"},
                                              {"A", "B", "C"}};

        // Create lattice with String-keys and Integer-values
        Lattice<String, Integer> lattice = new Lattice<String, Integer>(elements);
        
        // Store a predictive property
        PredictiveProperty property1 = new PredictiveProperty("Property1", Direction.UP);
        lattice.putProperty(lattice.nodes().getBottom(), property1);
        System.out.println(lattice.hasProperty(lattice.nodes().getTop(), property1));
        lattice.putProperty(lattice.nodes().build().next("B").next("B").next("C").create(), property1);
        System.out.println(lattice.hasProperty(lattice.nodes().build().next("B").next("B").next("B").create(), property1));
    }    
}
