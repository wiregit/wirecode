package com.limegroup.gnutella.util;

import java.io.*;

import com.sun.java.util.collections.*;

/**
 *  This class provides static functions to load/store the files.
 * @author Anurag Singla
 */
public class FileUtils
{
    /**
     * Writes the passed map to corresponding file
     * @param filename The name of the file to which to write the passed map
     * @param map The map to be stored
     */
    public static void writeMap(String filename, Map map)
        throws IOException, ClassNotFoundException
    {
        ObjectOutputStream out = null;
        try
        {
            //open the file
            out = new ObjectOutputStream(new FileOutputStream(filename));
            //write to the file
            out.writeObject(map);	
        }
        finally
        {
            //close the stream
            if(out != null)
                out.close();
        }
    }
    
    /**
     * Reads the map stored, in serialized object form, 
     * in the passed file and returns it. from the file where it is stored
     * @param filename The file from where to read the Map
     * @return The map that was read
     */
    public static Map readMap(String filename)
        throws IOException, ClassNotFoundException
    {
        ObjectInputStream in = null;
        try
        {
            //open the file
            in = new ObjectInputStream(new FileInputStream(filename));
            //read and return the object
            return (Map)in.readObject();	
        }
        finally
        {
            //close the file
            if(in != null)
                in.close();
        }    
    }

    /** Same as the f.listFiles() in JDK1.3. */
    public static File[] listFiles(File f) {
        String[] children=f.list();
        if (children==null)
            return null;
    
        File[] ret = new File[children.length];
        for (int i=0; i<children.length; i++) {
            ret[i] = new File(f, children[i]);
        }
    
        return ret;
    }
    
}
