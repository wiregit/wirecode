/*
 * SchemaFieldMap.java
 *
 * Created on April 23, 2001, 10:03 AM
 */

package com.limegroup.gnutella.xml;
import java.util.*;
import java.io.*;

/**
 * Stores the mapping from SchemaURI to corresponding 
 * SchemaServerMethodTransformationData.
 * SchemaURI (string) ==> SchemaServerMethodTransformationData
 * @author  asingla
 * @version
 */
public class SchemaTransformationDataMap
{
    /**
     * Stores the mapping from SchemaURI to corresponding 
     * SchemaServerMethodTransformationData.
     * SchemaURI (string) ==> SchemaServerMethodTransformationData
     * NamesUnderstoodByServer (String))
     */
    private Map _schemaTransformationDataMap;
    
    /**
     * an instance of SchemaTransformationDataMap
     * Note: Implements Singleton Design Pattern
     */
    private static SchemaTransformationDataMap _instance = null;
    
    /**
     * Creates new SchemaTransformationDataMap
     */
    private SchemaTransformationDataMap()
    {
        //initialize the map
        initialize();
    }
    
    /**
     * Returns an instance of SchemaTransformationDataMap.
     * Note: Implements Singleton Design Pattern
     */
    public static SchemaTransformationDataMap instance()
    {
        if(_instance == null)
            _instance = new SchemaTransformationDataMap();
        
        return _instance;
    }
    
    /**
     * Initializes the schemaTransformationDataMap from file
     * @modifies _schemaTransformationDataMap
     */
    private void initialize()
    {
        //load the schemaTransformationDataMap from file
        try
        {
            //read from file
            _schemaTransformationDataMap 
                = FileHandler.readSchemaTransformationDataMap();
        }
        catch(Exception e)
        {
            //if not able to read from file, create a new instance
            _schemaTransformationDataMap = new HashMap();
        }
    }
    
    /**
     * Adds the field mappings for the given schema uri
     * @param schemaURI A string (URI) that uniquely identifies the schema
     * @param data transformation data to be added
     */
    public void put(String schemaURI, SchemaServerMethodTransformationData data)
    {
        //add to the map
        _schemaTransformationDataMap.put(schemaURI, data);
    }
    
    /**
     * Saves the schemaTransformationDataMap to file
     */
    public void save()
    {
        try
        {
            FileHandler.writeSchemaTransformationDataMap(
                _schemaTransformationDataMap);
        }
        catch(Exception e)
        {
            //nothing we can do
            //just print the exception
            e.printStackTrace();
        }
    }
    
}
