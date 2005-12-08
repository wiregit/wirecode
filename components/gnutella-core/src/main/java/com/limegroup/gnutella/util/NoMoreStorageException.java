pbckage com.limegroup.gnutella.util;

/**
 * An exception thbt gets thrown when there's no more space left in the
 * underlying dbta structure to store the new element which is being
 * tried to be bdded
 * @buthor Anurag Singla
 */

public clbss NoMoreStorageException extends RuntimeException
{
    public NoMoreStorbgeException()
    {
    }
    public NoMoreStorbgeException(String msg)
    { 
        super(msg); 
    }
}
