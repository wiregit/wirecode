package com.limegroup.gnutella.xml;

import java.io.*;
import java.util.*;

/** 
 * This class provides static functions to load/store the files.
 * It handles filename details and
 * abstracts out the interaction with the file system.
 * @author Anurag Singla
 */

public class FileHandler
{

/**
 * Writes the SchemaTransformationDataMap instance to corresponding file
 * @param schemaTransformationDataMap The map to be stored
 */
public static void writeSchemaTransformationDataMap(
    Map schemaTransformationDataMap)
    throws IOException, ClassNotFoundException
{
    ObjectOutputStream out = null;
    try
    {
        //open the file
        out = new ObjectOutputStream(
            new FileOutputStream(LimeXMLProperties.instance()
            .getSchemaTransformationDataMapFile()));
        //write to the file
        out.writeObject(schemaTransformationDataMap);	
    }
    finally
    {
        //close the stream
        if(out != null)
            out.close();
    }
}

/**
* Reads  the SchemaTransformationDataMap from the file where it is stored
* @return The SchemaTransformationDataMap that was read
*/
public static Map readSchemaTransformationDataMap()
    throws IOException, ClassNotFoundException
{
    ObjectInputStream in = null;
    try
    {
        //open the file
        in = new ObjectInputStream(
                                new FileInputStream(LimeXMLProperties.instance()
                                    .getSchemaTransformationDataMapFile()));
        //read and return from it
        return (Map)in.readObject();	
    }
    finally
    {
        //close the file
        if(in != null)
            in.close();
    }    
}

}
