/*
 * LimeXMLSchemaRepository.java
 *
 * Created on April 12, 2001, 4:00 PM
 */

package com.limegroup.gnutella.xml;
import java.util.*;
import java.io.*;

/**
 * Stores and provides access to various XML schemas that me might have.
 * Singleton class
 * @author  asingla
 * @version
 */
public class LimeXMLSchemaRepository extends java.lang.Object
{
    
    /**
     * Mapping from URI (string) to an instance of XMLSchema
     */
    private Map /* Schema URI (String) ==> LimeXMLSchema */ _uriSchemaMap 
        = new HashMap();
    
    /**
     * An instance of this class
     */
    private static LimeXMLSchemaRepository _instance = null;
    
    /** Creates new LimeXMLSchemaRepository */
    private LimeXMLSchemaRepository()
    {
        initialize();
    }
    
    /**
     * Initializes the internal data structures
     * @requires Should be called from Constructor only
     */
    protected void initialize()
    {
        //get the schema files
        File[] schemaFiles = 
                LimeXMLProperties.instance().getAllXMLSchemaFiles();
        
        //if there are some files there,initialize from those files
        if(schemaFiles != null)
        {
            LimeXMLSchema limeXmlSchema;
            //create schema objects and put them in the _uriSchemaMap
            for(int i=0; i < schemaFiles.length; i++)
            {
                try
                {
                    limeXmlSchema = new LimeXMLSchema(schemaFiles[i]);
                    _uriSchemaMap.put(limeXmlSchema.getSchemaURI(),limeXmlSchema);
                }
                catch(IOException ioe)
                {
                    //no problem
                }//end of try
            }//end of for
        }//end of if
    }//end of fn initialize
    
    /**
     * Returns an instance of this class. Adheres to Singleton design pattern.
     * So, only one instance of the class is created.
     */
    public static LimeXMLSchemaRepository instance()
    {
        if(_instance == null)
            _instance = new LimeXMLSchemaRepository();
        
        return _instance;
    }
    
    /**
     * Returns the schema corresponding to the given URI
     * @param uri The URI which identifies the schema to be returned.
     * @return The schema corresponding to the given uri. If no mapping
     * exists, returns null.
     */
    public LimeXMLSchema getSchema(String uri)
    {
        synchronized(_uriSchemaMap)
        {
            return (LimeXMLSchema)_uriSchemaMap.get(uri);
        }
    }
    
    /**
     * Returns the URIs scooresponding to the schemas that we have
     * @return the URIs scooresponding to the schemas that we have
     */ 
    public String[] getAvailableSchemaURIs()
    {
        synchronized(_uriSchemaMap)
        {
            Set keySet = _uriSchemaMap.keySet();
            return (String[])keySet.toArray(new String[0]);
        }
    }
    
    
    public static void main(String[] args)
    {
        Test();
    }
    
    private static void Test()
    {
        String[] availableSchemas 
            = LimeXMLSchemaRepository.instance().getAvailableSchemaURIs();
        for(int i=0; i < availableSchemas.length; i++)
        {
            System.out.println("schema = " + availableSchemas[i]);
        }
    }
}
