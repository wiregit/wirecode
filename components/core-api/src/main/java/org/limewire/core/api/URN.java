package org.limewire.core.api;

public interface URN extends Comparable<URN> {
    @Override
    public boolean equals(Object obj);
    
    @Override
    public int hashCode();
    
    @Override
    public String toString();
}
