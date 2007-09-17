package org.limewire.nio;

/** Marks some object as requiring the SelectionKey attachment. */
public interface RequiresSelectionKeyAttachment {

    /** Sets the attachment that the SelectionKey has. */
    void setAttachment(Object o);
    
}
