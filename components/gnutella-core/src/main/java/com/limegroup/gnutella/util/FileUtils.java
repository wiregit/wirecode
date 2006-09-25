package com.limegroup.gnutella.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UploadManager;


/**
 * This class provides static functions to load/store the files.
 * @author Anurag Singla
 */
public class FileUtils {
    
    private static final Log LOG = LogFactory.getLog(FileUtils.class);

    /**
     * Gets the canonical path, catching buggy Windows errors
     */
    public static String getCanonicalPath(File f) throws IOException {
        try {
            return f.getCanonicalPath();
        } catch(IOException ioe) {
            String msg = ioe.getMessage();
            // windows bugs out :(
            if(CommonUtils.isWindows() && msg != null && msg.indexOf("There are no more files") != -1)
                return f.getAbsolutePath();
            else
                throw ioe;
        }
    }
    
    /** Same as f.getCanonicalFile() in JDK1.3. */
    public static File getCanonicalFile(File f) throws IOException {
        try {
            return f.getCanonicalFile();
        } catch(IOException ioe) {
            String msg = ioe.getMessage();
            // windows bugs out :(
            if(CommonUtils.isWindows() && msg != null && msg.indexOf("There are no more files") != -1)
                return f.getAbsoluteFile();
            else
                throw ioe;
        }
    }

    /** 
     * Detects attempts at directory traversal by testing if testDirectory 
     * really is the parent of testPath.  This method should be used to make
     * sure directory traversal tricks aren't being used to trick
     * LimeWire into reading or writing to unexpected places.
     * 
     * Directory traversal security problems occur when software doesn't 
     * check if input paths contain characters (such as "../") that cause the
     * OS to go up a directory.  This function will ignore benign cases where
     * the path goes up one directory and then back down into the original directory.
     * 
     * @return false if testParent is not the parent of testChild.
     * @throws IOException if getCanonicalPath throws IOException for either input file
     */
    public static final boolean isReallyParent(File testParent, File testChild) throws IOException {
        // Don't check testDirectory.isDirectory... 
        // If it's not a directory, it won't be the parent anyway.
        // This makes the tests more simple.
        
        String testParentName = getCanonicalPath(testParent);
        String testChildParentName = getCanonicalPath(testChild.getAbsoluteFile().getParentFile());
        if (! testParentName.equals(testChildParentName))
            return false;
        
        return true;
    }
    
    
    /**
     * Utility method that returns the file extension of the given file.
     * 
     * @param f the <tt>File</tt> instance from which the extension 
     *   should be extracted
     * @return the file extension string, or <tt>null</tt> if the extension
     *   could not be extracted
     */
    public static String getFileExtension(File f) {
        String name = f.getName();
        return getFileExtension(name);
    }
     
    /**
     * Utility method that returns the file extension of the given file.
     * 
     * @param name the file name <tt>String</tt> from which the extension
     *  should be extracted
     * @return the file extension string, or <tt>null</tt> if the extension
     *   could not be extracted
     */
    public static String getFileExtension(String name) {
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
            
        String cmds[] = null;
        if( CommonUtils.isWindows() || CommonUtils.isMacOSX() )
            SystemUtils.setWriteable(fName);
        else if ( CommonUtils.isOS2() )
            cmds = null; // Find the right command for OS/2 and fill in
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
        
		return f.canWrite();
    }
    
    /**
     * Touches a file, to ensure it exists.
     */
    public static void touch(File f) throws IOException {
        if(f.exists())
            return;
        
        File parent = f.getParentFile();
        if(parent != null)
            parent.mkdirs();

        try {
            f.createNewFile();
        } catch(IOException failed) {
            // Okay, createNewFile failed.  Let's try the old way.
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(f);
            } catch(IOException ioe) {
                ioe.initCause(failed);
                throw ioe;
            } finally {
                IOUtils.close(fos);
            }
        }
    }
    
    public static boolean forceRename(File a, File b) {
    	 // First attempt to rename it.
        boolean success = a.renameTo(b);
        
        // If that fails, try killing any partial uploads we may have
        // to unlock the file, and then rename it.
        if (!success) {
            FileDesc fd = RouterService.getFileManager().getFileDescForFile(
                a);
            if( fd != null ) {
                UploadManager upMan = RouterService.getUploadManager();
                // This must all be synchronized so that a new upload
                // doesn't lock the file before we rename it.
                synchronized(upMan) {
                    if( upMan.killUploadsForFileDesc(fd) )
                        success = a.renameTo(b);
                }
            }
        }
        
        // If that didn't work, try copying the file.
        if (!success) {
            success = CommonUtils.copy(a, b);
            //if copying succeeded, get rid of the original
            //at this point any active uploads will have been killed
            if (success)
            	a.delete();
        }
        return success;
    }
    
    /**
     * Saves the data iff it was written exactly as we wanted.
     */
    public static boolean verySafeSave(File dir, String name, byte[] data) {
        File tmp;
        try {
            tmp = File.createTempFile(name, "tmp", dir);
        } catch(IOException hrorible) {
            return false;
        }
        
        File out = new File(dir, name);
        
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(tmp));
            os.write(data);
            os.flush();
        } catch(IOException bad) {
            return false;
        } finally {
            IOUtils.close(os);
        }
        
        //verify that we wrote everything correctly
        byte[] read = readFileFully(tmp);
        if(read == null || !Arrays.equals(read, data))
            return false;
        
        return forceRename(tmp, out);
    }
    
    /**
     * Reads a file, filling a byte array.
     */
    public static byte[] readFileFully(File source) {
        DataInputStream raf = null;
        int length = (int)source.length();
        if(length <= 0)
            return null;

        byte[] data = new byte[length];
        try {
            raf = new DataInputStream(new BufferedInputStream(new FileInputStream(source)));
            raf.readFully(data);
        } catch(IOException ioe) {
            return null;
        } finally {
            IOUtils.close(raf);
        }
        
        return data;
    }

    /**
     * @param directory Gets all files under this directory RECURSIVELY.
     * @param filter If null, then returns all files.  Else, only returns files
     * extensions in the filter array.
     * @return An array of Files recursively obtained from the directory,
     * according to the filter.
     * 
     */
    public static File[] getFilesRecursive(File directory,
                                           String[] filter) {
        List<File> dirs = new ArrayList<File>();
        // the return array of files...
        List<File> retFileArray = new ArrayList<File>();
        File[] retArray = new File[0];

        // bootstrap the process
        if (directory.exists() && directory.isDirectory())
            dirs.add(directory);

        // while i have dirs to process
        while (dirs.size() > 0) {
            File currDir = dirs.remove(0);
            String[] listedFiles = currDir.list();
            for (int i = 0; (listedFiles != null) && (i < listedFiles.length); i++) {
                File currFile = new File(currDir,listedFiles[i]);
                if (currFile.isDirectory()) // to be dealt with later
                    dirs.add(currFile);
                else if (currFile.isFile()) { // we have a 'file'....
                    boolean shouldAdd = false;
                    if (filter == null)
                        shouldAdd = true;
                    else {
                        String ext = FileUtils.getFileExtension(currFile);
                        for (int j = 0; (j < filter.length) && (ext != null); j++) {
                            if (ext.equalsIgnoreCase(filter[j]))  {
                                shouldAdd = true;
                                
                                // don't keep looping through all filters --
                                // one match is good enough
                                break;
                            }
                        }
                    }
                    if (shouldAdd)
                        retFileArray.add(currFile);
                }
            }
        }        

        if (!retFileArray.isEmpty()) {
            retArray = new File[retFileArray.size()];
            for (int i = 0; i < retArray.length; i++)
                retArray[i] = retFileArray.get(i);
        }

        return retArray;
    }

    /**
     * Parses the text of a special path into a complete platform-specific path in a File.
     * Special paths can be complete or relative, step upwards, and start with platform-specific special folders.
     * 
     * <pre>
     * Special Path               Return File
     * -------------------------  --------------------------
     * C:\Folder\Subfolder        C:\Folder\Subfolder
     * Folder Here                C:\Program Files\LimeWire\Folder Here
     * ..\One Up                  C:\Program Files\One Up
     * Desktop>                   C:\Documents and Settings\User Name\Desktop
     * Documents>In My Documents  C:\Documents and Settings\User Name\My Documents\In My Documents
     * </pre>
     * 
     * The first example is just the entire path to the directory "C:\Folder\Subfolder".
     * These examples assume LimeWire is running from C:\Program Files\LimeWire\LimeWire.jar.
     * Given just "Folder Here", this method will return the path as though Folder Here is sitting next to LimeWire.jar.
     * Use "..\" at the start to move up from where LimeWire.jar is running.
     * Use the tags SystemUtils.getSpecialPath() understands, putting ">" after them as a separator.
     * 
     * @param path The text of a special path.
     * @return     A File with that path made absolute and specific to this platform and running instance.
     *             null given an unknown tag or a relative path this method can't resolve.
     */
    public static File parseSpecialPath(String path) {
    	if (path == null) return null;

    	// If the given path contains a ">", parse for the special folder tag before it
    	int i = path.indexOf(">");
    	if (i != -1) {
    		String tag = path.substring(0, i);
    		String special = SystemUtils.getSpecialPath(tag);
    		if (special == null)
    			return null; // Unknown tag
    		path = path.substring(i + 1);
    		return new File(special, path).getAbsoluteFile();
    	}

    	try {
    		return new File(path).getCanonicalFile();
    	} catch (IOException e) { return null; } // Unable to resolve the relative path
    }

    /**
     * Copies all the files and folders in a directory to a new location.
     * 
     * @param sourceDirectory      The directory to copy, must exist on disk
     * @param destinationDirectory The destination path where the copy will go, must be free on disk
     */
    public static void copyDirectory(File sourceDirectory, File destinationDirectory) {

    	// Make sure the given source directory exists, and make the new empty destination directory
    	if (!sourceDirectory.isDirectory() || destinationDirectory.exists()) throw new IllegalArgumentException();
    	destinationDirectory.mkdirs();

    	// Loop for each name in the source directory, like "file.ext" and "subfolder name"
    	String[] contents = sourceDirectory.list();
    	File source, destination;
    	for (String name : contents) {

    		// Make File objects with complete paths for this file or subfolder
    		source = new File(sourceDirectory, name);
    		destination = new File(destinationDirectory, name);

    		// Copy it across
    		if (source.isDirectory())
    			copyDirectory(source, destination); // Call this same method to copy the subfolder and its contents
    		else
    			CommonUtils.copy(source, destination);
    	}
    }

    /**
     * Deletes the given file or directory, moving it to the trash can or recycle bin if the platform has one.
     * 
     * @param file The file or directory to trash or delete
     * @return     true on success
     */
    public static boolean delete(File file) {
    	if (!file.exists()) {
    		return false;
    	}
    	if (CommonUtils.isMacOSX()) {
    		return moveToTrashOSX(file);
    	} else if (CommonUtils.isWindows()) {
    		return SystemUtils.recycle(file);
    	} else {
    		file.delete();
    		return !file.exists();
    	}
    }

    /**
     * Moves the given file or directory to Trash.
     * 
     * @param file The file or directory to move to Trash
     * @throws IOException if the canonical path cannot be resolved
     *          or if the move process is interrupted
     * @return true on success
     */
    private static boolean moveToTrashOSX(File file) {
    	try {
    		String[] command = moveToTrashCommand(file);
    		ProcessBuilder builder = new ProcessBuilder(command);
    		builder.redirectErrorStream();
    		Process process = builder.start();
    		ProcessUtils.consumeAllInput(process);
    		process.waitFor();
    	} catch (InterruptedException err) {
    		LOG.error("InterruptedException", err);
    	} catch (IOException err) {
    		LOG.error("IOException", err);
    	}
    	return !file.exists();
    }

    /**
     * Creates and returns the the osascript command to move
     * a file or directory to the Trash
     * 
     * @param file The file or directory to move to Trash
     * @throws IOException if the canonical path cannot be resolved
     * @return OSAScript command
     */
    private static String[] moveToTrashCommand(File file) {
    	String path = null;
    	try {
    		path = file.getCanonicalPath();
    	} catch (IOException err) {
    		LOG.error("IOException", err);
    		path = file.getAbsolutePath();
    	}
    	
    	String fileOrFolder = (file.isFile() ? "file" : "folder");
    	
    	String[] command = new String[] { 
    			"osascript", 
    			"-e", "set unixPath to \"" + path + "\"",
    			"-e", "set hfsPath to POSIX file unixPath",
    			"-e", "tell application \"Finder\"", 
    			"-e",    "if " + fileOrFolder + " hfsPath exists then", 
    			"-e",        "move " + fileOrFolder + " hfsPath to trash",
    			"-e",    "end if",
    			"-e", "end tell" 
    	};
    	
    	return command;
    }
}
