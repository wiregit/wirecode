/*
 * EndpointData.java
 *
 * Created on November 30, 2000, 4:37 PM
 */

package com.limegroup.gnutella;

import com.sun.java.util.collections.*;


/**
 *
 * @author  Anurag Singla
 * @version
 */
public class EndpointData extends Endpoint implements Cloneable, 
                        java.io.Serializable
{

/**
* Client GUID of the host this endpoint refers to
*/
private GUID clientGUID = null;

/**
* Speed in kilobytes of the host this endpoint refers to
*/
private long speed = 56;
    

/**
* Creates a new EndpointData instance from another endpoint
* @param endpoint from where to copy the fields
*/
public EndpointData(Endpoint endpoint)
{   
    //initialize the fields in the super class
    super(endpoint);
}

/**
* Creates a new EndpointData instance
* @param endpoint from where to copy the fields for the super class
* @param speed Speed of the host
* @param clientGUID Unique GUID for the host
*/
public EndpointData(Endpoint endpoint, long speed, GUID clientGUID)
{
    //initialize the fields in the super class
    super(endpoint);
    //set the speed and client GUID
    this.set(speed, clientGUID);
}


/**
* Sets the speed and clientGUID for the host this endpoint refers to
* @param speed Speed of the host
* @param clientGUID Unique GUID for the host
*/
public void set(long speed, GUID clientGUID)
{
    //set the fields
    this.speed = speed;
    this.clientGUID = clientGUID;
}



}//end of class