package com.limegroup.gnutella.downloader;

import java.io.*;
import com.limegroup.gnutella.*;

/** 
 * A repository of temporary filenames.  Gives out file names for temporary
 * files with the following properties:
 *
 * <ul>
 *   <li>Two IncompleteFileManager's never get out the same filenames.
 *       Since each ManagedDownloader has its own IncompleteFileManager,
 *       downloads can never get interleaved on disk.
 *   <li>Two duplicate files in the same downloader always get the same name.
 *       This enables smart resumes across hosts.
 * </ul> 
 *
 * Note that this class is responsible for determining if two similar files are
 * duplicates!  
 */
public class IncompleteFileManager implements Serializable {
    /** The ID for the next instantiation of IncompleteFileManager.  Used to
     *  guarantee incomplete files of different downloads never conflict.
     *  LOCKING: obtain IncompleteFileManager's lock. */
    private static int nextManagerID=0;
    /** The delimiter to use between the IP address and real name of a temporary
     * file.  To make it easier to break the temporary name into its constituent
     * parts, this should not contain "." or a number. */
    static final String SEPARATOR="-";
    
    /** This' managerID given out during construction. 
     *  @see nextManagerID */
    private int managerID;
    
    public IncompleteFileManager() {
        synchronized (IncompleteFileManager.class) {
            this.managerID=nextManagerID;
            nextManagerID++;
            //TODO: recycle numbers?
        }
    }

    /** Creates a new TemporaryFile object to for a normal of the given
     *  file/location pair.  The location of the file is determined by the
     *  INCOMPLETE_DIRECTORY property.  The disk is not modified.
     *
     *  This method gives duplicate files the same temporary file.  That is, for
     *  all rfd_i and rfd_j
     *
     *       rfd_i~=rfd_j => getFile(rfd_i).equals(tempoaryFile(rfd_j))  
     * 
     *  Currently rfd_i~=rfd_j if rfd_i.getName().equals(rfd_j) &&
     *  rfd_i.getSize()==rfd_j.getSize().  In the future, this definition may be
     *  strengthened to depend on hash values.
     */
    public File getFile(RemoteFileDesc rfd) {
        return new File(SettingsManager.instance().getIncompleteDirectory(),
            "N"+managerID
           +SEPARATOR
           +String.valueOf(rfd.getSize())
           +SEPARATOR
           +rfd.getFileName());
    }

    /**
     * Creates a normal download for a push file.  The location of the file is
     * determined by the INCOMPLETE_DIRECTORY property.  The disk is not
     * modified.  Unlike the other getFile method, this does not give duplicate
     * files the same incomplete name.
     *
     * @param filename the name of the downloaded file, without directory info
     * @param index the index on the server
     * @param serverGUID the 16 byte serverGUID of the uploader.
     */
    public File getFile(String filename, int index, byte[] serverGUID) {
        return new File(SettingsManager.instance().getIncompleteDirectory(),
            "P"+managerID
           +SEPARATOR
           +(new GUID(serverGUID)).toHexString()
           +SEPARATOR
           +index
           +SEPARATOR
           +filename);
    }    
}
