package com.limegroup.gnutella.library;

import java.util.List;

/** A {@link SmartFileCollection} that is shared with one of more people. */
public interface SharedFileCollection extends SmartFileCollection {
    
    /** Gets the current name of this collection. */
    String getName();
    
    /** Returns the unique id of this collection. */
    int getId();
    
    /** Returns the current list of people this collection is shared with. */
    List<String> getSharedIdList();
    
    /** Adds a new person to the list of people the collection is shared with. */
    void addPersonToShareWith(String id);
    
    /**
     * Removes a person from the list of people this collection is shared with.
     * Returns true if the person was removed.
     */
    boolean removePersonToShareWith(String id);
    
    /** Sets the new list of people this collection should be shared with. */
    void setShareIdList(List<String> ids);

}
