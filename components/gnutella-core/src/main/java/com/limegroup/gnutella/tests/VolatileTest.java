/*
 * VolatileTest.java
 *
 * Created on November 29, 2000, 4:40 PM
 */

package com.limegroup.gnutella.tests;

/**
 *
 * @author  Anurag Singla
 */

/**
* Test to check if the actions on 'volatile' variables are actually
* as per the specs
*/
public class VolatileTest extends Object
{

//shared variables for threads
private volatile int sharedCount;
private volatile int threadsLeft;

/**
* number of updator threads to create
*/
private static final int THREADS = 1000;
    
    
/** Creates new VolatileTest */
public VolatileTest()
{
}


/**
* Creates some threads which access the shared volatile variables
* concurrently, without any explicit synchronization
*/ 
public void runTest()
{
    threadsLeft = 1000;
    sharedCount = 0;
    
      for(int i=0; i < THREADS; i++)
    {
        (new Thread(new Updator())).start();
    }
}

public static void main(String[] args)
{
    VolatileTest volatileTest = new VolatileTest();
    volatileTest.runTest();
}



/**
* Asseses and updates shared volatile variables without using any
* explicit synchronization mechanism
*/
private class Updator implements Runnable
{
 
    /**
    * Asseses and updates shared volatile variables without using any
    * explicit synchronization mechanism
    */
    public void run()
    {
        //some threads should sleep for som etim eto generate random behaviour
        //and also so that the other threads get created in the meantime
        //before these threads die
        try
        {
            //Note: The first thread will not sleep as sharedCount is equal 
            //to zero in the beginning
            //this is required so that value of sharedCount keeps on changing
            //while other threads test this condition
            if(sharedCount % 2 != 0)
            {
                Thread.sleep(250);
            }
        }
        catch(InterruptedException ie)
        {
            
        }
        
        //change shared values
        sharedCount++;
        
        //let some other thread take over
        Thread.yield();
        
        //change values again
        sharedCount--;
        threadsLeft--;
        
        //print to see the results
        //All the lines printed should have same value for 
        //sharedCount as well as threadsLeft
        System.out.println("sharedCount= " + sharedCount + " threadsLeft="
                                    + threadsLeft);
        
    }
    
}


}