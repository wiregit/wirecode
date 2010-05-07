package com.limegroup.gnutella.library;

import java.util.List;

/** A {@link SmartFileCollection} that is shared with one of more people. */
public interface SharedFileCollection extends FileCollection {
    
    /** Returns the unique id of this collection. */
    int getId();
    
    /** Returns the current list of people this collection is shared with. */
    List<String> getFriendList();
    
    /**
     * Returns true if this is a public collection. 
     */
    boolean isPublic();
}
