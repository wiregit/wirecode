package com.limegroup.gnutella.util;


public interface HashFunction 
{
    /**
     * @return custom hash code for the given object
     */
    public int hash(Object o);
}
