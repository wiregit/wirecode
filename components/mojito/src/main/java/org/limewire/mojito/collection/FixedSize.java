package org.limewire.mojito.collection;

public interface FixedSize {

    public int getMaxSize();
    
    public boolean isFull();
}
