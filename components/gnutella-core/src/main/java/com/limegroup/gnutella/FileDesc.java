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

    public FileDesc(int i, String n, String p, int s) {
        _index = i;
        _name = n;
        _path = p;
        _size = s;
    }

    public void print() {

        System.out.println("Name: " + _name);
        System.out.println("Index: " + _index);
        System.out.println("Size: " + _size);
        System.out.println("Path: " + _path);
        System.out.println(" ");


    }


}
