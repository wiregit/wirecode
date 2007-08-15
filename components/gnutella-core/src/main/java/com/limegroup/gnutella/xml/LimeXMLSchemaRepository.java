/*
 * LimeXMLSchemaRepository.java
 *
 * Created on April 12, 2001, 4:00 PM
 */

package com.limegroup.gnutella.xml;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores and provides access to various XML schemas that me might have.
 * Singleton class
 * @author  asingla
 */
public class LimeXMLSchemaRepository
{
    
    /**
     * Mapping from URI (string) to an instance of XMLSchema
     */
    private Map<String, LimeXMLSchema> _uriSchemaMap 
        = new HashMap<String, LimeXMLSchema>();
    
    /**
     * An instance of this class
     */
    private static LimeXMLSchemaRepository _instance = 
        new LimeXMLSchemaRepository();
    
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
        if(schemaFiles != null) {
            LimeXMLSchema limeXmlSchema;
            //create schema objects and put them in the _uriSchemaMap
            for(int i=0; i < schemaFiles.length; i++) {
                try
                {
                    limeXmlSchema = new LimeXMLSchema(schemaFiles[i]);
                    _uriSchemaMap.put(limeXmlSchema.getSchemaURI(),limeXmlSchema);
                } catch(IOException ioe) {}
            }
        }
    }
    
    /**
     * Returns an instance of this class. Adheres to Singleton design pattern.
     * So, only one instance of the class is created.
     */
    public static LimeXMLSchemaRepository instance()
    {
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
            return _uriSchemaMap.get(uri);
        }
    }
    
    /**
     * Returns all availble schemas.
     */
    public Collection<LimeXMLSchema> getAvailableSchemas() {
        return Collections.unmodifiableCollection(_uriSchemaMap.values());
    }
    
    /**
     * Returns the URIs scooresponding to the schemas that we have
     * @return the URIs scooresponding to the schemas that we have
     */ 
    public String[] getAvailableSchemaURIs()
    {
        String[] schemaURIs;
        synchronized(_uriSchemaMap) {
            schemaURIs = _uriSchemaMap.keySet().toArray(new String[0]);
        }
        Arrays.sort(schemaURIs);
        return schemaURIs;
        
    }
}
