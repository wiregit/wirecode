/*
 * Weighable.java
 *
 * Created on November 21, 2000, 8:04 PM
 */

package com.limegroup.gnutella.util;

/**
*
* @author  Anurag Singla
*/


/**
* The instances of classes that implement this interface are weighable i.e. 
* they can be compared based upon their weight (or importance).
* This interface allows the weight to be increased only.
*/
public interface Weighable 
{

    /**
    * Gives the weight of the instance
    * @return the weight
    */
    public int getWeight();
    
    
    /**
    * sets the weight for this instance
    * @param weight the weight
    */
    public void addWeight(int weight);
    
}
