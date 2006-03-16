package com.limegroup.gnutella.io;

import java.util.Set;

/**
 * Interface that allows arbitrary pseudo-selectors to be polled
 * for SelectionKeys in addition to 
 * @author Sam
 *
 */
public interface Pollable {
    
    public Set /* of SelectionKey */ poll();

}
