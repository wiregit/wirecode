/*
 * LimeXMLSchemaRepository.java
 *
 * Created on April 12, 2001, 4:00 PM
 */

package com.limegroup.gnutella.xml;
import java.util.*;
import java.io.*;

/**
 * Stores and provides access to various XML schemas that me might have
 * @author  asingla
 * @version
 */
public class LimeXMLSchemaRepository extends java.lang.Object
{
    
    /**
     * Mapping from URI (string) to an instance of XMLSchema
     */
    private Map /* Schema URI (String) ==> XMLSchema */ _uriSchemaMap 
        = new HashMap();
    
    /**
     * An instance of this class
     */
    private LimeXMLSchemaRepository _instance = null;
    
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
        
        LimeXMLSchema limeXmlSchema;
        //create schema objects and put them in the _uriSchemaMap
        for(int i=0; i < schemaFiles.length; i++)
        {
            try
            {
                limeXmlSchema = new LimeXMLSchema(schemaFiles[i]);
                _uriSchemaMap.put(limeXmlSchema.getSchemaIdentifier(),limeXmlSchema);
            }
            catch(IOException ioe)
            {
                //no problem
            }
        }
        
    }
    
    /**
     * Returns an instance of this class. Adheres to Singleton design pattern.
     * So, only one instance of the class is created.
     */
    public LimeXMLSchemaRepository instance()
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
    
}
