package com.limegroup.gnutella.util;

import java.io.File;
import com.sun.java.util.collections.Comparator;

/**
 * Compares to File's lexically by file name).  Useful for storing Java 1.1.8
 * Files in Java 1.2+ sorted collections classes.  This is needed because Files
 * in 1.1.8 do not implement the Comparable interface, unlike Strings in 1.2+.
 */
public class FileComparator implements Comparator {
    /** Returns (((File)a).getAbsolutePath()).compareTo(
     *              ((File)b).getAbsolutePath()) */
    public int compare(Object a, Object b) {
        String as=((File)a).getAbsolutePath();
        String bs=((File)b).getAbsolutePath();
        return as.compareTo(bs);
    }
}
