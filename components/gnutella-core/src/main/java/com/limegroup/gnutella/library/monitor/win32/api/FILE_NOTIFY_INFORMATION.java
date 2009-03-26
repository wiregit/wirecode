package com.limegroup.gnutella.library.monitor.win32.api;

import com.sun.jna.Structure;

/** This structure is non-trivial since it is a pattern stamped
 * into a large block of result memory rather than something that stands
 * alone or is used for input.
 */
public class FILE_NOTIFY_INFORMATION extends Structure {
    public int NextEntryOffset;
    public int Action;
    public int FileNameLength;
    // filename is not nul-terminated, so we can't use a String/WString
    public char[] FileName = new char[1];
    
    private FILE_NOTIFY_INFORMATION() { } 
    public FILE_NOTIFY_INFORMATION(int size) {
        if (size < size())
            throw new IllegalArgumentException("Size must greater than "
                                               + size() + ", requested " 
                                               + size);
        allocateMemory(size);
    }
    /** WARNING: this filename may be either the short or long form
     * of the filename.
     */
    public String getFilename() {
        return new String(FileName, 0, FileNameLength/2);
    }
    public void read() {
        // avoid reading filename until we know how long it is
        FileName = new char[0];
        super.read();
        FileName = getPointer().getCharArray(12, FileNameLength/2);
    }
    public FILE_NOTIFY_INFORMATION next() {
        if (NextEntryOffset == 0)
            return null;
        FILE_NOTIFY_INFORMATION next = new FILE_NOTIFY_INFORMATION();
        next.useMemory(getPointer(), NextEntryOffset);
        next.read();
        return next;
    }
}