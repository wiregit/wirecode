/*
 * XMLSchemaRepository.java
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
public class XMLSchemaRepository extends java.lang.Object
{
    
    /**
     * Mapping from URI (string) to an instance of XMLSchema
     */
    private Map /* Schema URI ==> XMLSchema */ _uriSchemaMap 
        = new HashMap();
    
    /**
     * An instance of this class
     */
    private XMLSchemaRepository _instance = null;
    
    /** Creates new XMLSchemaRepository */
    private XMLSchemaRepository()
    {
        initialize();
    }
    
    /**
     * Initializes the internal data structures
     */
    protected void initialize()
    {
        //get the schema files
        File[] schemaFiles = 
                XMLProperties.instance().getAllXMLSchemaFiles();
        
        XMLSchema xmlSchema;
        //create schema objects and put them in the _uriSchemaMap
        for(int i=0; i < schemaFiles.length; i++)
        {
            try
            {
                xmlSchema = new XMLSchema(schemaFiles[i]);
                _uriSchemaMap.put(xmlSchema.getSchemaIdentifier(), xmlSchema);
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
    public XMLSchemaRepository instance()
    {
        if(_instance == null)
            _instance = new XMLSchemaRepository();
        
        return _instance;
    }
    
    /**
     * Returns the schema corresponding to the given URI
     * @param uri The URI which identifies the schema to be returned.
     * @return The schema corresponding to the given uri. If no mapping
     * exists, returns null.
     */
    public XMLSchema getSchema(String uri)
    {
        return (XMLSchema)_uriSchemaMap.get(uri);
    }
    
}
