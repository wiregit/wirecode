/*
 * ServerMethodTransformation.java
 *
 * Created on April 23, 2001, 10:29 AM
 */

package com.limegroup.gnutella.xml;
import java.util.*;

/**
 * Stores the information required to transform a xml documents confirming
 * to a particular XML schema to the representation and method understood by
 * the server.
 * @author  asingla
 * @version
 */
public class SchemaServerMethodTransformationData 
{
    //constants defining the methods to communicate with server
    public static final String HTTP_POST   = "HTTP_POST";
    public static final String HTTP_GET    = "HTTP_GET";
    
    /**
     * The server to connect to 
     */
    private String _serverHostname= "";
    
    /**
     * The server port to connect to
     */
    private int _serverPort = 80;
    
    /**
     * The method to be used to send data to the server
     */
    private String _method = HTTP_POST;
    
    /**
     * Path of the script to send the HTTP request to
     */
    private String _scriptPath = "";
    
    /** 
     * Map of CanonicalizedFieldNameInXMLSchema (String) ==>
     * NamesUnderstoodByServer (String)
     */
    private Map _fieldMap = new HashMap();
    
    /**
     * Sets the name of the server to communicate the data to the passed name
     * @param serverHostname The server to connect to 
     */
    public void setServerHostname(String serverHostname)
    {
        this._serverHostname = serverHostname;
    }
    
     /**
     * Returns the name of the server to communicate the data to 
     * @return The name of the server to connect to 
     */
    public String getServerHostname()
    {
        return this._serverHostname;
    }
    
    /**
     * Sets the port of the server to communicate the data to the passed port
     * @param serverPort The server  port to connect to 
     */
    public void setServerPort(int serverPort)
    {
        this._serverPort = serverPort;
    }
    
    /**
     * Returns the port of the server to communicate the data to 
     * @return The server  port to connect to 
     */
    public int getServerPort()
    {
        return this._serverPort;
    }
    
    
    /** 
     * Creates new ServerMethodTransformation, and initializesvarios fields
     * to the passed values. 
     */
    public SchemaServerMethodTransformationData(String serverHostname,
    int serverPort, String method, String scriptPath)
    {
        //initialize the fields
        this._serverHostname = serverHostname;
        this._serverPort = serverPort;
        this._method = method;
        this._scriptPath = scriptPath;
    }
    
    /**
     * Maps the given field to specified name
     * @param fieldName The field to be mapped
     * @param mappedName The new name for the field (name that the field is
     * mapped to
     */
    public void addFieldMapping(String fieldName, String mappedName)
    {
        _fieldMap.put(fieldName, mappedName);
    }
    
}
