package com.limegroup.gnutella.downloader;

import java.io.File;
import com.limegroup.gnutella.*;

/** 
 * A temporary download file.  It is guaranteed that two TemporaryFile objects
 * with the same name but different IP addresses will have distinct names on
 * disk.  For example, the temporary file may be named
 * "C:\LimeWire\Incomplete\FE1273HDSF-a song.mp3".  
 */
public class TemporaryFile extends File {
    /** The delimiter to use between the IP address and real name of a temporary
     * file.  To make it easier to break the temporary name into its constituent
     * parts, this should not contain "." or a number. */
    static final String SEPARATOR="-";

    /** Creates a new TemporaryFile object to represent the given file/location
     *  pair.  The location of the file is determined by the
     *  INCOMPLETE_DIRECTORY property.  The disk is not modified.  
     *
     * @param filename the ultimate name of the file when the temporary
     *  file has been downloaded, e.g., 'foo.mp3'.  This must not contain
     *  any directory components.
     * @param host the serverGUID (aka, clientGUID) of the host we are
     *  downloading from.  REQUIRES: serverGUID.length==16.
     */
    public TemporaryFile(String filename, byte[] serverGUID) {
        super(SettingsManager.instance().getIncompleteDirectory(),
              (new GUID(serverGUID)).toHexString()+SEPARATOR+filename);
    }

    /** Same as TemporaryFile(rfd.getFileName(), rfd.getClientGUID()) */
    public TemporaryFile(RemoteFileDesc rfd) {
        this(rfd.getFileName(), rfd.getClientGUID());
    }
}
