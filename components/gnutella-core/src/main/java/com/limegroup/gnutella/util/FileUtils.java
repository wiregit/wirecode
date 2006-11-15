package com.limegroup.gnutella.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.util.HashSet;
import java.util.Set;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UploadManager;


/**
 * This class provides static functions to load/store the files.
 * @author Anurag Singla
 */
public class FileUtils {

    /**
     * Writes the passed map to corresponding file
     * @param filename The name of the file to which to write the passed map
     * @param map The map to be stored
     */
    public static void writeObject(String filename, Object obj)
        throws IOException {
        ObjectOutputStream out = null;
        try {
        	File f = new File(filename);
        	if (f.exists())
        		f.createNewFile();
            //open the file
            out = new ObjectOutputStream(
            		new BufferedOutputStream(
            				new FileOutputStream(f)));
            //write to the file
            out.writeObject(obj);	
            out.flush();
        } finally {
            //close the stream
            IOUtils.close(out);
        }
    }
    
    /**
     * Reads the map stored, in serialized object form, 
     * in the passed file and returns it. from the file where it is stored
     * @param filename The file from where to read the Map
     * @return The map that was read
     */
    public static Object readObject(String filename)
        throws IOException, ClassNotFoundException {
        ObjectInputStream in = null;
        try {
            //open the file
            in = new ObjectInputStream(
            		new BufferedInputStream(
            				new FileInputStream(filename)));
            //read and return the object
            return in.readObject();	
        } finally {
            //close the file
            IOUtils.close(in);
        }    
    }
    
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
     * Determines if file 'a' is an ancestor of file 'b'.
     */
    public static final boolean isAncestor(File a, File b) {
        while(b != null) {
            if(b.equals(a))
                return true;
            b = b.getParentFile();
        }
        return false;
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
     * Detects attempts at directory traversal by testing if testDirectory 
     * really is a parent of testPath.
     * @see isReallyParent
     */
    public static final boolean isReallyInParentPath(File testParent, File testChild) throws IOException {

    	String testParentName = getCanonicalPath(testParent);
    	String testChildParentName = getCanonicalPath(testChild.getAbsoluteFile().getParentFile());
    	return testChildParentName.startsWith(testParentName);
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
        int index = indexOfExtension(name);        
        return index != -1 ? name.substring(index) : null;
    }
    
    /**
     * Returns the starting index of the filename's extension. 
     * @param name
     * @return -1 if first or last character is dot or <code>name</code> does
     * not contain any dot
     */
    public static int indexOfExtension(String name) {
    	int index = name.lastIndexOf(".");
    	if (index <= 0 || index == name.length() - 1) {
    		return -1;
    	}
    	return index + 1;
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
                    if( upMan.killUploadsForFileDesc(fd))
                        success = a.renameTo(b);
                }
            }
        }
        
        if (!success && RouterService.getTorrentManager().killTorrentForFile(a)) 
        	success = a.renameTo(b);
        
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
    		return deleteRecursive(file);
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
    
    public static boolean deleteRecursive(File file) {
		// make sure we only delete canonical children of the parent file we
		// wish to delete. I have a hunch this might be an issue on OSX and
		// Linux under certain circumstances.
		// If anyone can test whether this really happens (possibly related to
		// symlinks), I would much appreciate it.
		String canonicalParent;
		try {
			canonicalParent = file.getCanonicalPath();
		} catch (IOException ioe) {
			return false;
		}

		if (!file.isDirectory())
			return file.delete();

		File[] files = file.listFiles();
		for (int i = 0; i < files.length; i++) {
			try {
				if (!files[i].getCanonicalPath().startsWith(canonicalParent))
					continue;
			} catch (IOException ioe) {
				return false;
			}
			if (!deleteRecursive(files[i]))
				return false;
		}

		return file.delete();
	}
    
    /**
     * @return true if the two files are the same.  If they are both
     * directories returns true if there is at least one file that 
     * conflicts.
     */
    public static boolean conflictsAny(File a, File b) {
    	if (a.equals(b))
    		return true;
    	Set<File> unique = new HashSet<File>();
    	unique.add(a);
    	for (File recursive: getFilesRecursive(a,null))
    		unique.add(recursive);
    	
    	if (unique.contains(b))
    		return true;
    	for (File recursive: getFilesRecursive(b,null)) {
    		if (unique.contains(recursive))
    			return true;
    	}
    	
    	return false;
    	
    }
    
    public static long getLengthRecursive(File f) {
    	if (!f.isDirectory())
    		return f.length();
    	long ret = 0;
    	for (File file : getFilesRecursive(f,null))
    		ret += file.length();
    	return ret;
    }
}
