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
    Map _schemaTransformationDataMap = new HashMap();
    
    /**
     * Creates new SchemaTransformationDataMap
     * @param schemaTransformationDataMapFile The file which stores the 
     * schemaTransformationDataMap in serialized object form
     */
    public SchemaTransformationDataMap(File schemaTransformationDataMapFile)
    {
        this();
        //initialize from the file
        initialize(schemaTransformationDataMapFile);
    }
    
    /**
     * Creates new SchemaTransformationDataMap
     */
    public SchemaTransformationDataMap()
    {
        
    }
    
    /**
     * Initializes the schemaTransformationDataMap from file
     * @param schemaTransformationDataMapFile The file which stores the 
     * schemaTransformationDataMap in serialized object form
     * @modifies _schemaTransformationDataMap
     */
    private void initialize(File schemaTransformationDataMapFile)
    {
        
    }
    
    /**
     * Adds the field mappings for the given schema uri
     * @param schemaURI A string (URI) that uniquely identifies the schema
     * @param fieldMap Map of CanonicalizedFieldName (String) ==>
     * NamesUnderstoodByServer (String)
     */
    public void addFieldMap(String schemaURI, Map fieldMap)
    {
        
    }
    
    /**
     * Saves the schemaTransformationDataMap to given file
     * @param saveFile The file to which the map is to be saved
     */
    public void save(File saveFile)
    {
        
    }
    
}
