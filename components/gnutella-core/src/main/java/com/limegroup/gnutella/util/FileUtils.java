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

    /**
     * Same as f.listFiles(FileNameFilter) in JDK1.2
     */
    public static File[] listFiles(File f, FilenameFilter filter) {
        String[] children=f.list(filter);
        if(children == null)
            return null;
        File[] ret = new File[children.length];
        for (int i=0; i<children.length; i++) {
            ret[i] = new File(f, children[i]);
        }
    
        return ret;
    }

    /** 
     * Same as f.getParentFile() in JDK1.3. 
     * @requires the File parameter must be a File object constructed
     *  with the canonical path.
     */
    public static File getParentFile(File f) {
    	// slight changes to get around getParent bug on Mac
    	String name=f.getParent();
    	if(name == null) return null;
    	try {
    		return FileUtils.getCanonicalFile(new File(name));
    	} catch(IOException ioe) {
    		//if the exception occurs, simply return null
    		return null;
    	}
    }

    /** Same as f.getCanonicalFile() in JDK1.3. */
    public static File getCanonicalFile(File f) throws IOException {
        return new File(f.getCanonicalPath());
    }

    /**
     * Utility method that returns the file extension of the given file.
     * 
     * @param f the <tt>File</tt> instance that the extension should
     *   be extracted from
     * @return the file extension string, or <tt>null</tt> if the extension
     *   could not be extracted
     */
    public static String getFileExtension(File f) {
        String name = f.getName();
        int index = name.lastIndexOf(".");
        if(index == -1) return null;
        
        // the file must have a name other than the extension
        if(index == 0) return null;
        
        // if the last character of the string is the ".", then there's
        // no extension
        if(index == (name.length()-1)) return null;
        
        return name.substring(index+1);
    }
    
    /**
     * Set of all files that we couldn't set writeable
     * so we don't check again.
     */
    private static final Set UNWRITEABLE =
        Collections.synchronizedSet(new HashSet());
    
    /**
     * Utility method to set a file as non read only.
     * If the file is already writable, does nothing.
     *
     * @param f the <tt>File</tt> instance whose read only flag should
     *  be unset.
     * 
     * @return whether or not <tt>f</tt> is writable after trying to make it
     *  writeable -- note that if the file doesn't exist, then this returns
     *  <tt>true</tt> 
     */
    public static boolean setWriteable(File f) {
        if(!f.exists())
            return true;

        // non Windows-based systems return the wrong value
        // for canWrite when the argument is a directory --
        // writing is based on the 'x' attribute, not the 'w'
        // attribute for directories.
        if(f.canWrite()) {
            if(CommonUtils.isWindows())
                return true;
            else if(!f.isDirectory())
                return true;
        }
            
        String fName;
        try {
            fName = f.getCanonicalPath();
        } catch(IOException ioe) {
            fName = f.getPath();
        }
        
        if( UNWRITEABLE.contains(fName) )
            return false;
            
        String cmds[] = null;
        if( CommonUtils.isWindows() )
            SystemUtils.setWriteable(fName);
        else if ( CommonUtils.isMacClassic() )
            cmds = null; // TODO: fill in.
        else {
            if(f.isDirectory())
                cmds = new String[] { "chmod", "u+w+x", fName };
            else
                cmds = new String[] { "chmod", "u+w", fName};
        }
        
        if( cmds != null ) {
            try { 
                Process p = Runtime.getRuntime().exec(cmds);
                p.waitFor();
            }
            catch(SecurityException ignored) { }
            catch(IOException ignored) { }
            catch(InterruptedException ignored) { }
        }
        
        if( !f.canWrite() ) {
            UNWRITEABLE.add(fName);
            return false;
        } else
            return true;
    }
    
    /**
     * Touches a file, to ensure it exists.
     */
    public static void touch(File f) throws IOException {
        OutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
        } finally {
            if(fos != null) {
                try {
                    fos.close();
                } catch(IOException ignored) {}
            }
        }
    }
}
