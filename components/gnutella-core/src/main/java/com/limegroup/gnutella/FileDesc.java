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
	/* i'm storing the data as a String.  I don't 
	   know if this is correct, since md5 and such 
	   might be an int. i know all the others are 
	   public but i like accessor methods */
	private String _meta;  
		
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
    }

	public String getMeta() {return _meta;}
	public void setMeta(String m) {_meta = m;}
	

    public void print() {
        System.out.println("Name: " + _name);
        System.out.println("Index: " + _index);
        System.out.println("Size: " + _size);
        System.out.println("Path: " + _path);
        System.out.println(" ");
    }
}
