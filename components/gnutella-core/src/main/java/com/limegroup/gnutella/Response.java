package com.limegroup.gnutella;

/** 
 * A query response record, i.e., a single response from a query search.
 * Create these to respond to a search.   This should have a full suite
 * of observers/mutators, but I'm too lazy. 
 */
public class Response {
    public int index;
    public int size;
    /** The name of the file matching the search.  This does NOT
     *  include the double null terminator.
     */
    public byte[] name;
}
