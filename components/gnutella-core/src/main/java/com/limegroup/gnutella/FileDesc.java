/**
 * auth: rsoule
 * file: FileDesc.java
 * desc: This class is a wrapper for file information.
 *
 */

package com.limegroup.gnutella;

public class FileDesc {

    public int _index;
    public String _path;
    public String _name;
    public int _size;
    public ID3Tag _id3Tag;

    /**
     * @param i index of the file
     * @param n the name of the file (e.g., "funny.txt")
     * @param p the fully-qualified path of the file
     *  (e.g., "/home/local/funny.txt")
     * @param s the size of the file, in bytes
     */
    public FileDesc(int i, String n, String p, int s, ID3Tag id3Tag) {
        _index = i;
        _name = n;
        _path = p;
        _size = s;
        _id3Tag = id3Tag;
    }
}
