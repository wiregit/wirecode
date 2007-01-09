package org.limewire.collection;

/**
 * An exception that gets thrown when there's no more space left in the
 * underlying data structure to store the new element which is being
 * tried to be added
 * @author Anurag Singla
 */

public class NoMoreStorageException extends RuntimeException
{
    public NoMoreStorageException()
    {
    }
    public NoMoreStorageException(String msg)
    { 
        super(msg); 
    }
}
