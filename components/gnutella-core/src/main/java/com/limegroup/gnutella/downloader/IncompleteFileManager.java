package com.limegroup.gnutella.downloader;

import java.io.*;
import com.limegroup.gnutella.*;

/** 
 * A repository of temporary filenames.  Gives out file names for temporary
 * files, ensuring that two duplicate files always get the same name.  This
 * enables smart resumes across hosts.<p>
 *
 * The original version of this class ensured that two IncompleteFileManager
 * never gave out the same temporary files.  That restriction has now been
 * relaxed, so this class is really somewhat overkill.  However, in the future
 * it may become smarter by looking at hashes, etc. 
 */
public class IncompleteFileManager implements Serializable {
    /** The delimiter to use between the size and a real name of a temporary
     * file.  To make it easier to break the temporary name into its constituent
     * parts, this should not contain a number. */
    static final String SEPARATOR="-";

    /** Creates a new TemporaryFile object to for a normal of the given
     *  file/location pair.  The location of the file is determined by the
     *  INCOMPLETE_DIRECTORY property.  The disk is not modified.
     *
     *  This method gives duplicate files the same temporary file.  That is, for
     *  all rfd_i and rfd_j
     *
     *       rfd_i~=rfd_j <==> getFile(rfd_i).equals(getFile(rfd_j))  
     * 
     *  Currently rfd_i~=rfd_j if rfd_i.getName().equals(rfd_j) &&
     *  rfd_i.getSize()==rfd_j.getSize().  In the future, this definition may be
     *  strengthened to depend on hash values.
     */
    public File getFile(RemoteFileDesc rfd) {
        return getFile(rfd.getFileName(), rfd.getSize());
    } 

    /** Same thing as getFile(rfd), where rfd.getFile().equals(name) 
     *  and rfd.getSize()==size. */ 
    public File getFile(String name, int size) {
        return new File(SettingsManager.instance().getIncompleteDirectory(),
                        "T"+SEPARATOR+size+SEPARATOR+name);
    }
}
