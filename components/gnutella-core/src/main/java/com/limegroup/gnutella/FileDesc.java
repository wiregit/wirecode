/**
 * This class is a wrapper for file information.
 *
 * Modified by Sumeet Thadani (5/21): No need to store meta-data here
 */

package com.limegroup.gnutella;

public class FileDesc {

    public int _index;
    public String _path;
    public String _name;
    public int _size;
    /**
     * byte[] that stores the hash of the file. The format of the hash is SHA1
     */
    byte[] _hash;
		
    /**
     * @param i index of the file
     * @param n the name of the file (e.g., "funny.txt")
     * @param p the fully-qualified path of the file
     *  (e.g., "/home/local/funny.txt")
     * @param s the size of the file, in bytes.  (Note that
     *  files are currently limited to Integer.MAX_VALUE bytes
     *  length, i.e., 2048MB.)
     */
    public FileDesc(int i, String n, String p, int s) {
        _index = i;
        _name = n;
        _path = p;
        _size = s;
        _hash = null;
    }

    public FileDesc(int i, String n, String p, int s, byte[] h) {
        _index = i;
        _name = n;
        _path = p;
        _size = s;
        _hash = h;
    }

    public void print() {
        System.out.println("Name: " + _name);
        System.out.println("Index: " + _index);
        System.out.println("Size: " + _size);
        System.out.println("Path: " + _path);
        System.out.println("Hash  " + _hash);
        System.out.println(" ");
    }
}


