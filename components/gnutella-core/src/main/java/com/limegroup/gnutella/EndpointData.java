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
private byte[] clientGUID = null;

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
* @param hostname Hostname of the node this endpoint refers to
* @param port The port number for the host
*/
public EndpointData(String hostname, int port)
{
    //initialize the fields in the super class
    super(hostname,port);
}

/**
* Creates a new EndpointData instance
* @param hostname Hostname of the node this endpoint refers to
* @param port The port number for the host
* @param files the number of files the host has
* @param kbytes the size of all of the files, in kilobytes
*/
public EndpointData(String hostname, int port, long files, long kbytes)
{
    //initialize the fields in the super class
    super(hostname, port, files, kbytes);
}


/**
* Creates a new EndpointData instance
* @param endpoint from where to copy the fields for the super class
* @param speed Speed of the host
* @param clientGUID Unique GUID for the host
*/
public EndpointData(Endpoint endpoint, long speed, byte[] clientGUID)
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
public void set(long speed, byte[] clientGUID)
{
    //set the fields
    this.speed = speed;
    this.clientGUID = clientGUID;
}


/**
* Checks if two instances of this class are same or not.
* They are same if the super class verifies that they are same, and in addition,
* if the client IDs are defined for both the instances, they should also be
* same.
* @param o The instance to compare with
*/
public boolean equals(Object o)
{
    //if ot of proper type, return false
    if (! (o instanceof EndpointData))
        return false;
    
    //type cast
    EndpointData endpointData = (EndpointData)o;
    
    //if super class returns false, return false
    if(! super.equals(endpointData))
        return false;
    //if the client IDs are defined for both the instances, 
    //they should also be same.
    if(clientGUID != null && endpointData.clientGUID != null)
    {
        //just to prevent object creation by converting to GUID instances,
        //comparing the bytes directly
        
        //if not of same size return false
        if(clientGUID.length != endpointData.clientGUID.length)
            return false;
        
        //compare the bytes. If any byte doesnt match return false
        for (int i=0; i<clientGUID.length; i++)
            if (clientGUID[i] != endpointData.clientGUID[i])
                return false;
    }
    
    //if passed all the above tests, return true
    return true;
    
    
}

}//end of class