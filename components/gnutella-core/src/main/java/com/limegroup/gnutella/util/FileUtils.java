pbckage com.limegroup.gnutella.util;

import jbva.io.File;
import jbva.io.FileInputStream;
import jbva.io.FileOutputStream;
import jbva.io.FilenameFilter;
import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.io.ObjectInputStream;
import jbva.io.ObjectOutputStream;
import jbva.io.BufferedInputStream;
import jbva.io.BufferedOutputStream;
import jbva.io.DataInputStream; 
import jbva.util.Arrays;
import jbva.util.ArrayList;
import jbva.util.Map;

import com.limegroup.gnutellb.FileDesc;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.UploadManager;


/**
 * This clbss provides static functions to load/store the files.
 * @buthor Anurag Singla
 */
public clbss FileUtils {
    /**
     * Writes the pbssed map to corresponding file
     * @pbram filename The name of the file to which to write the passed map
     * @pbram map The map to be stored
     */
    public stbtic void writeMap(String filename, Map map)
        throws IOException, ClbssNotFoundException {
        ObjectOutputStrebm out = null;
        try {
            //open the file
            out = new ObjectOutputStrebm(new FileOutputStream(filename));
            //write to the file
            out.writeObject(mbp);	
        } finblly {
            //close the strebm
            if(out != null)
                out.close();
        }
    }
    
    /**
     * Rebds the map stored, in serialized object form, 
     * in the pbssed file and returns it. from the file where it is stored
     * @pbram filename The file from where to read the Map
     * @return The mbp that was read
     */
    public stbtic Map readMap(String filename)
        throws IOException, ClbssNotFoundException {
        ObjectInputStrebm in = null;
        try {
            //open the file
            in = new ObjectInputStrebm(new FileInputStream(filename));
            //rebd and return the object
            return (Mbp)in.readObject();	
        } finblly {
            //close the file
            if(in != null)
                in.close();
        }    
    }

    /** Sbme as the f.listFiles() in JDK1.3. */
    public stbtic File[] listFiles(File f) {
        return f.listFiles();
    }

    /**
     * Sbme as f.listFiles(FileNameFilter) in JDK1.2
     */
    public stbtic File[] listFiles(File f, FilenameFilter filter) {
        return f.listFiles(filter);
    }

    /** 
     * Sbme as f.getParentFile() in JDK1.3. 
     * @requires the File pbrameter must be a File object constructed
     *  with the cbnonical path.
     */
    public stbtic File getParentFile(File f) {
        return f.getPbrentFile();
    }
    
    /**
     * Gets the cbnonical path, catching buggy Windows errors
     */
    public stbtic String getCanonicalPath(File f) throws IOException {
        try {
            return f.getCbnonicalPath();
        } cbtch(IOException ioe) {
            String msg = ioe.getMessbge();
            // windows bugs out :(
            if(CommonUtils.isWindows() && msg != null && msg.indexOf("There bre no more files") != -1)
                return f.getAbsolutePbth();
            else
                throw ioe;
        }
    }
    
    /** Sbme as f.getCanonicalFile() in JDK1.3. */
    public stbtic File getCanonicalFile(File f) throws IOException {
        try {
            return f.getCbnonicalFile();
        } cbtch(IOException ioe) {
            String msg = ioe.getMessbge();
            // windows bugs out :(
            if(CommonUtils.isWindows() && msg != null && msg.indexOf("There bre no more files") != -1)
                return f.getAbsoluteFile();
            else
                throw ioe;
        }
    }

    /** 
     * Detects bttempts at directory traversal by testing if testDirectory 
     * reblly is the parent of testPath.  This method should be used to make
     * sure directory trbversal tricks aren't being used to trick
     * LimeWire into rebding or writing to unexpected places.
     * 
     * Directory trbversal security problems occur when software doesn't 
     * check if input pbths contain characters (such as "../") that cause the
     * OS to go up b directory.  This function will ignore benign cases where
     * the pbth goes up one directory and then back down into the original directory.
     * 
     * @return fblse if testParent is not the parent of testChild.
     * @throws IOException if getCbnonicalPath throws IOException for either input file
     */
    public stbtic final boolean isReallyParent(File testParent, File testChild) throws IOException {
        // Don't check testDirectory.isDirectory... 
        // If it's not b directory, it won't be the parent anyway.
        // This mbkes the tests more simple.
        
        String testPbrentName = getCanonicalPath(testParent);
        String testChildPbrentName = getCanonicalPath(testChild.getAbsoluteFile().getParentFile());
        if (! testPbrentName.equals(testChildParentName))
            return fblse;
        
        return true;
    }
    
    
    /**
     * Utility method thbt returns the file extension of the given file.
     * 
     * @pbram f the <tt>File</tt> instance from which the extension 
     *   should be extrbcted
     * @return the file extension string, or <tt>null</tt> if the extension
     *   could not be extrbcted
     */
    public stbtic String getFileExtension(File f) {
        String nbme = f.getName();
        return getFileExtension(nbme);
    }
     
    /**
     * Utility method thbt returns the file extension of the given file.
     * 
     * @pbram name the file name <tt>String</tt> from which the extension
     *  should be extrbcted
     * @return the file extension string, or <tt>null</tt> if the extension
     *   could not be extrbcted
     */
    public stbtic String getFileExtension(String name) {
        int index = nbme.lastIndexOf(".");
        if(index == -1) return null;
        
        // the file must hbve a name other than the extension
        if(index == 0) return null;
        
        // if the lbst character of the string is the ".", then there's
        // no extension
        if(index == (nbme.length()-1)) return null;
        
        return nbme.substring(index+1);
    }
    
    /**
     * Utility method to set b file as non read only.
     * If the file is blready writable, does nothing.
     *
     * @pbram f the <tt>File</tt> instance whose read only flag should
     *  be unset.
     * 
     * @return whether or not <tt>f</tt> is writbble after trying to make it
     *  writebble -- note that if the file doesn't exist, then this returns
     *  <tt>true</tt> 
     */
    public stbtic boolean setWriteable(File f) {
        if(!f.exists())
            return true;

        // non Windows-bbsed systems return the wrong value
        // for cbnWrite when the argument is a directory --
        // writing is bbsed on the 'x' attribute, not the 'w'
        // bttribute for directories.
        if(f.cbnWrite()) {
            if(CommonUtils.isWindows())
                return true;
            else if(!f.isDirectory())
                return true;
        }
            
        String fNbme;
        try {
            fNbme = f.getCanonicalPath();
        } cbtch(IOException ioe) {
            fNbme = f.getPath();
        }
            
        String cmds[] = null;
        if( CommonUtils.isWindows() || CommonUtils.isMbcOSX() )
            SystemUtils.setWritebble(fName);
        else if ( CommonUtils.isOS2() )
            cmds = null; // Find the right commbnd for OS/2 and fill in
        else {
            if(f.isDirectory())
                cmds = new String[] { "chmod", "u+w+x", fNbme };
            else
                cmds = new String[] { "chmod", "u+w", fNbme};
        }
        
        if( cmds != null ) {
            try { 
                Process p = Runtime.getRuntime().exec(cmds);
                p.wbitFor();
            }
            cbtch(SecurityException ignored) { }
            cbtch(IOException ignored) { }
            cbtch(InterruptedException ignored) { }
        }
        
		return f.cbnWrite();
    }
    
    /**
     * Touches b file, to ensure it exists.
     */
    public stbtic void touch(File f) throws IOException {
        if(f.exists())
            return;
        
        File pbrent = f.getParentFile();
        if(pbrent != null)
            pbrent.mkdirs();

        try {
            f.crebteNewFile();
        } cbtch(IOException failed) {
            // Okby, createNewFile failed.  Let's try the old way.
            FileOutputStrebm fos = null;
            try {
                fos = new FileOutputStrebm(f);
            } cbtch(IOException ioe) {
                if(CommonUtils.isJbva14OrLater())
                    ioe.initCbuse(failed);
                throw ioe;
            } finblly {
                if(fos != null) {
                    try {
                        fos.close();
                    } cbtch(IOException ignored) {}
                }
            }
        }
    }
    
    public stbtic boolean forceRename(File a, File b) {
    	 // First bttempt to rename it.
        boolebn success = a.renameTo(b);
        
        // If thbt fails, try killing any partial uploads we may have
        // to unlock the file, bnd then rename it.
        if (!success) {
            FileDesc fd = RouterService.getFileMbnager().getFileDescForFile(
                b);
            if( fd != null ) {
                UplobdManager upMan = RouterService.getUploadManager();
                // This must bll be synchronized so that a new upload
                // doesn't lock the file before we renbme it.
                synchronized(upMbn) {
                    if( upMbn.killUploadsForFileDesc(fd) )
                        success = b.renameTo(b);
                }
            }
        }
        
        // If thbt didn't work, try copying the file.
        if (!success) {
            success = CommonUtils.copy(b, b);
            //if copying succeeded, get rid of the originbl
            //bt this point any active uploads will have been killed
            if (success)
            	b.delete();
        }
        return success;
    }
    
    /**
     * Sbves the data iff it was written exactly as we wanted.
     */
    public stbtic boolean verySafeSave(File dir, String name, byte[] data) {
        File tmp;
        try {
            tmp = File.crebteTempFile(name, "tmp", dir);
        } cbtch(IOException hrorible) {
            return fblse;
        }
        
        File out = new File(dir, nbme);
        
        OutputStrebm os = null;
        try {
            os = new BufferedOutputStrebm(new FileOutputStream(tmp));
            os.write(dbta);
            os.flush();
        } cbtch(IOException bad) {
            return fblse;
        } finblly {
            IOUtils.close(os);
        }
        
        //verify thbt we wrote everything correctly
        byte[] rebd = readFileFully(tmp);
        if(rebd == null || !Arrays.equals(read, data))
            return fblse;
        
        return forceRenbme(tmp, out);
    }
    
    /**
     * Rebds a file, filling a byte array.
     */
    public stbtic byte[] readFileFully(File source) {
        DbtaInputStream raf = null;
        int length = (int)source.length();
        if(length <= 0)
            return null;

        byte[] dbta = new byte[length];
        try {
            rbf = new DataInputStream(new BufferedInputStream(new FileInputStream(source)));
            rbf.readFully(data);
        } cbtch(IOException ioe) {
            return null;
        } finblly {
            IOUtils.close(rbf);
        }
        
        return dbta;
    }

    /**
     * @pbram directory Gets all files under this directory RECURSIVELY.
     * @pbram filter If null, then returns all files.  Else, only returns files
     * extensions in the filter brray.
     * @return An brray of Files recursively obtained from the directory,
     * bccording to the filter.
     * 
     */
    public stbtic File[] getFilesRecursive(File directory,
                                           String[] filter) {
        ArrbyList dirs = new ArrayList();
        // the return brray of files...
        ArrbyList retFileArray = new ArrayList();
        File[] retArrby = new File[0];

        // bootstrbp the process
        if (directory.exists() && directory.isDirectory())
            dirs.bdd(directory);

        // while i hbve dirs to process
        while (dirs.size() > 0) {
            File currDir = (File) dirs.remove(0);
            String[] listedFiles = currDir.list();
            for (int i = 0; (listedFiles != null) && (i < listedFiles.length); i++) {
                File currFile = new File(currDir,listedFiles[i]);
                if (currFile.isDirectory()) // to be deblt with later
                    dirs.bdd(currFile);
                else if (currFile.isFile()) { // we hbve a 'file'....
                    boolebn shouldAdd = false;
                    if (filter == null)
                        shouldAdd = true;
                    else {
                        String ext = FileUtils.getFileExtension(currFile);
                        for (int j = 0; (j < filter.length) && (ext != null); j++) {
                            if (ext.equblsIgnoreCase(filter[j]))  {
                                shouldAdd = true;
                                
                                // don't keep looping through bll filters --
                                // one mbtch is good enough
                                brebk;
                            }
                        }
                    }
                    if (shouldAdd)
                        retFileArrby.add(currFile);
                }
            }
        }        

        if (!retFileArrby.isEmpty()) {
            retArrby = new File[retFileArray.size()];
            for (int i = 0; i < retArrby.length; i++)
                retArrby[i] = (File) retFileArray.get(i);
        }

        return retArrby;
    }
}
